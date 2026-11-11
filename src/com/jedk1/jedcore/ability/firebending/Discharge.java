package com.jedk1.jedcore.ability.firebending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.collision.CollisionDetector;
import com.jedk1.jedcore.collision.Sphere;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.lightning.LightningArc;
import com.jedk1.jedcore.util.lightning.LightningDisplay;
import com.jedk1.jedcore.util.lightning.LightningEffects;
import com.jedk1.jedcore.util.lightning.LightningElectrocution;
import com.jedk1.jedcore.util.lightning.LightningStyle;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.LightManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Discharge extends LightningAbility implements AddonAbility {
	private static final String DISPLAY_TAG = "jedcore_discharge";
	private static final int MAX_BRANCHES = 5;
	private static final Map<UUID, Long> LAST_LIGHTNING_THREAT = new ConcurrentHashMap<>();
	private final HashMap<Integer, Location> branches = new HashMap<>();

	private LightningDisplay display;
	private LightningStyle lightningStyle;
	private final boolean defensiveMode;
	private boolean threatActive;
	private long lastThreatTime;
	private Location location;
	private Vector direction;
	private boolean hit;
	private int spaces;
	private int nextBranchId = 2;
	private double branchSpace;
	private final Random rand = new Random();

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.SPEED)
	private double speed;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown, avatarCooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	private boolean slotSwapping;
	@Attribute("CollisionRadius")
	private double entityCollisionRadius;
	@Attribute("StunChance")
	private double stunChance;
	@Attribute("Stun" + Attribute.DURATION)
	private double stunDuration;

	private float soundVolume;
	private int soundInterval;

	public Discharge(Player player) {
		this(player, false);
	}

	public Discharge(Player player, boolean defensiveMode) {
		super(player);

		this.defensiveMode = defensiveMode;

		if (defensiveMode) {
			if (!bPlayer.canBendIgnoreCooldowns(this) || hasAbility(player, Discharge.class) || !bPlayer.canLightningbend()) {
				return;
			}
		} else if (!bPlayer.canBend(this) || hasAbility(player, Discharge.class) || !bPlayer.canLightningbend()) {
			return;
		}

		setFields();
		display = new LightningDisplay(lightningStyle);

		direction = GeneralMethods.getMainHandLocation(player).getDirection().normalize();

		if (bPlayer.isAvatarState() || JCMethods.isSozinsComet(player.getWorld())) {
			this.cooldown = avatarCooldown;
		}

		start();
		if (!isRemoved() && !defensiveMode) {
			bPlayer.addCooldown(this);
		}

		if (!isRemoved() && defensiveMode) {
			threatActive = true;
			lastThreatTime = System.currentTimeMillis();
			markLightningThreat(player);
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		damage = config.getDouble("Abilities.Fire.Discharge.Damage");
		range = Math.max(0.5, config.getDouble("Abilities.Fire.Discharge.Range", 10.0));
		speed = Math.max(0.05, config.getDouble("Abilities.Fire.Discharge.Speed", 1.0));
		cooldown = config.getLong("Abilities.Fire.Discharge.Cooldown");
		avatarCooldown = config.getLong("Abilities.Fire.Discharge.AvatarCooldown");
		long travelDuration = (long) Math.ceil(range / speed * 50.0) + 250L;
		duration = Math.max(50L, Math.max(config.getLong("Abilities.Fire.Discharge.Duration", 1000L) / 2L, travelDuration));
		slotSwapping = config.getBoolean("Abilities.Fire.Discharge.SlotSwapping");
		entityCollisionRadius = config.getDouble("Abilities.Fire.Discharge.EntityCollisionRadius");
		stunChance = getConfig().getDouble("Abilities.Fire.Discharge.Stun.Chance");
		stunDuration = getConfig().getDouble("Abilities.Fire.Discharge.Stun.Duration");

		soundVolume = (float) config.getDouble("Abilities.Fire.Discharge.Sound.Volume");
		soundInterval = config.getInt("Abilities.Fire.Discharge.Sound.Interval");

		branchSpace = 0.2;

		lightningStyle = LightningStyle.builder()
				.material(Material.LIGHT_BLUE_CONCRETE_POWDER)
				.thickness(0.05)
				.lengthPadding(0.02)
				.brightness(15)
				.viewRange(1.0f)
				.emitLight(true)
				.maxSegments(32)
				.segmentLength(0.8)
				.jitter(0.18)
				.mainArcs(1)
				.branchChance(0.45)
				.branchInterval(3, 6)
				.branchLength(0.25)
				.subBranchChance(0.35)
				.subBranchLength(0.15)
				.branchJitter(0.7)
				.clipToBlocks(true)
				.displayTag(DISPLAY_TAG)
				.build();
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (defensiveMode) {
			if (!player.isSneaking()) {
				remove();
				return;
			}

			updateThreatStatus();
			if (!threatActive && System.currentTimeMillis() - lastThreatTime > 1000L) {
				remove();
				return;
			}

			if (!canBend()) {
				remove();
				return;
			}

			hit = false;
			advanceLocation();
			return;
		}

		if (!canBend()) {
			remove();
			return;
		}

		if (System.currentTimeMillis() < (getStartTime() + duration) && !hit) {
			advanceLocation();
			if (branches.isEmpty()) {
				remove();
			}
		} else {
			remove();
		}
	}

	private void updateThreatStatus() {
		if (isLightningThreat(player)) {
			threatActive = true;
			lastThreatTime = System.currentTimeMillis();
			markLightningThreat(player);
		} else if (hasRecentLightningThreat(player, 1000L)) {
			threatActive = true;
		} else {
			threatActive = false;
		}
	}

	private boolean canBend() {
		if (defensiveMode) {
			if (slotSwapping) {
				return bPlayer.canBendIgnoreBindsCooldowns(this) && bPlayer.canLightningbend();
			}
			return bPlayer.canBendIgnoreCooldowns(this) && bPlayer.canLightningbend();
		}

		if (!slotSwapping) {
			return bPlayer.canBendIgnoreCooldowns(this);
		} else {
			return bPlayer.canBendIgnoreBindsCooldowns(this);
		}
	}

	private void advanceLocation() {
		location = GeneralMethods.getMainHandLocation(player);
		direction = location.getDirection().normalize();

		if (branches.isEmpty() && (defensiveMode || spaces == 0)) {
			branches.put(1, location.clone());
		}

		spaces++;
		if (spaces % 3 == 0 && branches.size() < MAX_BRANCHES) {
			Location prevBranch = branches.get(1);
			if (prevBranch != null) {
				branches.put(nextBranchId++, prevBranch.clone());
			}
		}

		List<LightningArc> arcs = new ArrayList<>();
		List<Integer> cleanup = new ArrayList<>();

		int steps = Math.max(5, (int) Math.ceil(speed / 0.2));
		double stepDistance = speed / steps;
		double rangeSquared = range * range;

		for (int i : branches.keySet()) {
			Location origin = branches.get(i);

			if (origin != null) {
				Location l = origin.clone();

				if (!isTransparent(l.getBlock())) {
					cleanup.add(i);
					continue;
				}

				l.add(createBranch(), createBranch(), createBranch());
				l = clampToCollisionRadius(l);
				branchSpace += 0.001;

				boolean outOfRange = false;

				for (int j = 0; j < steps; j++) {
					if (rand.nextInt(soundInterval) == 0) {
						player.getWorld().playSound(l, Sound.ENTITY_BEE_HURT, soundVolume, 0.2f);
					}

					Vector vec = l.toVector();

					hit = CollisionDetector.checkEntityCollisions(player, new Sphere(l.toVector(), entityCollisionRadius), (entity) -> {
						if (RegionProtection.isRegionProtected(this, entity.getLocation()) || ((entity instanceof Player) && Commands.invincible.contains(entity.getName()))) {
							return true;
						}
						Vector knockbackVector = entity.getLocation().toVector().subtract(vec).normalize().multiply(0.8);
						GeneralMethods.setVelocity(this, entity, knockbackVector);

						DamageHandler.damageEntity(entity, damage, this);

						LightManager.createLight(entity.getLocation()).emit();

						entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BEE_HURT, soundVolume, 0.2f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_HURT, soundVolume, 0.2f);

						if (ThreadLocalRandom.current().nextDouble() <= this.stunChance) {
							LightningElectrocution.apply((LivingEntity) entity, this, this.stunDuration);
						}
						return true;
					});

					Location next = clampToCollisionRadius(l.clone().add(direction.clone().multiply(stepDistance)));
					if (next.distanceSquared(location) > rangeSquared) {
						outOfRange = true;
						break;
					}

					l = next;
				}

				if (outOfRange) {
					arcs.add(LightningArc.create(location, l, lightningStyle));
					cleanup.add(i);
					continue;
				}

				branches.put(i, l);
				arcs.add(LightningArc.create(location, l, lightningStyle));
			}
		}

		for (int i : cleanup) {
			branches.remove(i);
		}

		cleanup.clear();

		if (display != null) {
			display.render(arcs);
		}

		LightningEffects.spawnBlockImpactParticles(arcs);
	}

	private Location clampToCollisionRadius(Location point) {
		double radius = Math.max(0.05, entityCollisionRadius);
		Vector axis = direction.clone().normalize();
		Vector relative = point.toVector().subtract(location.toVector());
		double along = relative.dot(axis);
		Vector lateral = relative.clone().subtract(axis.clone().multiply(along));
		double lateralSquared = lateral.lengthSquared();

		if (lateralSquared <= radius * radius) {
			return point;
		}

		lateral.multiply(radius / Math.sqrt(lateralSquared));
		Vector clamped = location.toVector().add(axis.clone().multiply(along)).add(lateral);
		return clamped.toLocation(point.getWorld(), point.getYaw(), point.getPitch());
	}

	private double createBranch() {
		int i = rand.nextInt(3);

		switch (i) {
			case 0:
				return branchSpace;
			case 2:
				return -branchSpace;
			default:
				return 0.0;
		}
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public List<Location> getLocations() {
		return new ArrayList<>(branches.values());
	}

	@Override
	public double getCollisionRadius() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getDouble("Abilities.Fire.Discharge.AbilityCollisionRadius");
	}

	@Override
	public String getName() {
		return "Discharge";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	@Override
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return "* JedCore Addon *\n" + config.getString("Abilities.Fire.Discharge.Description");
	}

	public HashMap<Integer, Location> getBranches() {
		return branches;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Vector getDirection() {
		return direction;
	}

	public void setDirection(Vector direction) {
		this.direction = direction;
	}

	public boolean isHit() {
		return hit;
	}

	public void setHit(boolean hit) {
		this.hit = hit;
	}

	public int getSpaces() {
		return spaces;
	}

	public void setSpaces(int spaces) {
		this.spaces = spaces;
	}

	public double getBranchSpace() {
		return branchSpace;
	}

	public void setBranchSpace(double branchSpace) {
		this.branchSpace = branchSpace;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getAvatarCooldown() {
		return avatarCooldown;
	}

	public void setAvatarCooldown(long avatarCooldown) {
		this.avatarCooldown = avatarCooldown;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public double getStunChance() {
		return stunChance;
	}

	public void setStunChance(double stunChance) {
		this.stunChance = stunChance;
	}

	public double getStunDuration() {
		return stunDuration;
	}

	public void setStunDuration(double stunDuration) {
		this.stunDuration = stunDuration;
	}

	public boolean isSlotSwapping() {
		return slotSwapping;
	}

	public void setSlotSwapping(boolean slotSwapping) {
		this.slotSwapping = slotSwapping;
	}

	public double getEntityCollisionRadius() {
		return entityCollisionRadius;
	}

	public void setEntityCollisionRadius(double entityCollisionRadius) {
		this.entityCollisionRadius = entityCollisionRadius;
	}

	public static void markLightningThreat(Player player) {
		LAST_LIGHTNING_THREAT.put(player.getUniqueId(), System.currentTimeMillis());
	}

	public static boolean hasRecentLightningThreat(Player player, long windowMs) {
		Long last = LAST_LIGHTNING_THREAT.get(player.getUniqueId());
		return last != null && System.currentTimeMillis() - last <= windowMs;
	}

	public static boolean isLightningDamage(EntityDamageEvent event) {
		if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
			return true;
		}

		if (event instanceof EntityDamageByEntityEvent) {
			Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
			if (damager instanceof Player) {
				BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer((Player) damager);
				if (bendingPlayer != null) {
					CoreAbility bound = bendingPlayer.getBoundAbility();
					if (bound instanceof LightningAbility) {
						return true;
					}

					String name = bendingPlayer.getBoundAbilityName();
					if (name != null && name.toLowerCase().contains("lightning")) {
						return true;
					}
				}

				if (CoreAbility.hasAbility((Player) damager, LightningStream.class)
						|| CoreAbility.hasAbility((Player) damager, LightningBurst.class)
						|| CoreAbility.hasAbility((Player) damager, Discharge.class)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isLightningThreat(Player player) {
		double threatRange = 4.5;
		Location loc = player.getLocation();
		if (loc.getWorld() == null) {
			return false;
		}

		for (LightningStream stream : CoreAbility.getAbilities(LightningStream.class)) {
			if (stream.getPlayer().equals(player)) {
				continue;
			}

			Location origin = stream.getLocation();
			if (origin != null && origin.getWorld() != null && origin.getWorld().equals(loc.getWorld())
					&& origin.distanceSquared(loc) <= threatRange * threatRange) {
				return true;
			}
		}

		if (com.jedk1.jedcore.util.LightningCollisionSystem.isNearLightning(player, threatRange)) {
			return true;
		}

		if (LightningBurst.isNearBolt(player, threatRange)) {
			return true;
		}

		for (Discharge discharge : CoreAbility.getAbilities(Discharge.class)) {
			if (discharge.getPlayer().equals(player)) {
				continue;
			}

			Location dischargeLocation = discharge.getLocation();
			if (dischargeLocation != null && dischargeLocation.getWorld() != null
					&& dischargeLocation.getWorld().equals(loc.getWorld())
					&& dischargeLocation.distanceSquared(loc) <= threatRange * threatRange) {
				return true;
			}

			for (Location branch : discharge.getLocations()) {
				if (branch.getWorld() != null && branch.getWorld().equals(loc.getWorld())
						&& branch.distanceSquared(loc) <= threatRange * threatRange) {
					return true;
				}
			}
		}

		for (Entity entity : player.getNearbyEntities(threatRange, threatRange, threatRange)) {
			if (entity instanceof org.bukkit.entity.LightningStrike) {
				return true;
			}
		}

		return false;
	}

	public static void updateDefensiveTriggers() {
		for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
			BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
			if (bendingPlayer == null) {
				continue;
			}

			if (!"Discharge".equalsIgnoreCase(bendingPlayer.getBoundAbilityName())) {
				continue;
			}

			if (!player.isSneaking()) {
				continue;
			}

			if (CoreAbility.hasAbility(player, Discharge.class)) {
				continue;
			}

			if (!bendingPlayer.canLightningbend()) {
				continue;
			}

			if (!isLightningThreat(player) && !hasRecentLightningThreat(player, 1000L)) {
				continue;
			}

			new Discharge(player, true);
		}
	}

	@Override
	public void remove() {
		if (display != null) {
			display.remove();
			display = null;
		}
		super.remove();
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Fire.Discharge.Enabled");
	}
}
