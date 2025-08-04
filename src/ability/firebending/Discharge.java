package com.jedk1.jedcore.ability.firebending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.collision.CollisionDetector;
import com.jedk1.jedcore.collision.Sphere;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Discharge extends LightningAbility implements AddonAbility {
	private final HashMap<Integer, Location> branches = new HashMap<>();

	private Location location;
	private Vector direction;
	private boolean hit;
	private int spaces;
	private double branchSpace;
	private final Random rand = new Random();

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown, avatarCooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	private boolean slotSwapping;
	@Attribute("CollisionRadius")
	private double entityCollisionRadius;

	private float soundVolume;
	private int soundInterval;

	public Discharge(Player player) {
		super(player);

		if (!bPlayer.canBend(this) || hasAbility(player, Discharge.class) || !bPlayer.canLightningbend()) {
			return;
		}

		setFields();
		
		direction = player.getEyeLocation().getDirection().normalize();
		
		if (bPlayer.isAvatarState() || JCMethods.isSozinsComet(player.getWorld())) {
			this.cooldown = avatarCooldown;
		}

		start();
		if (!isRemoved()) {
			bPlayer.addCooldown(this);
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		damage = config.getDouble("Abilities.Fire.Discharge.Damage");
		cooldown = config.getLong("Abilities.Fire.Discharge.Cooldown");
		avatarCooldown = config.getLong("Abilities.Fire.Discharge.AvatarCooldown");
		duration = config.getLong("Abilities.Fire.Discharge.Duration");
		slotSwapping = config.getBoolean("Abilities.Fire.Discharge.SlotSwapping");
		entityCollisionRadius = config.getDouble("Abilities.Fire.Discharge.EntityCollisionRadius");

		soundVolume = (float) config.getDouble("Abilities.Fire.Discharge.Sound.Volume");
		soundInterval = config.getInt("Abilities.Fire.Discharge.Sound.Interval");
		
		branchSpace = 0.2;
	}
	
	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (!canBend()) {
			remove();
			return;
		}

		if (System.currentTimeMillis() < (getStartTime() + duration) && !hit) {
			advanceLocation();
		} else {
			remove();
		}
	}

	private boolean canBend() {
		if (!slotSwapping) {
			return bPlayer.canBendIgnoreCooldowns(this);
		} else {
			return bPlayer.canBendIgnoreBindsCooldowns(this);
		}
	}

	private void advanceLocation() {
		if (location == null) {
			Location origin = player.getEyeLocation().clone();
			location = origin.clone();
			branches.put(branches.size() + 1, location);
		}

		spaces++;
		if (spaces % 3 == 0) {
			Location prevBranch = branches.get(1);
			branches.put(branches.size() + 1, prevBranch);
		}
		
		List<Integer> cleanup = new ArrayList<>();
		
		for (int i : branches.keySet()) {
			Location origin = branches.get(i);

			if (origin != null) {
				Location l = origin.clone();

				if (!isTransparent(l.getBlock())) {
					cleanup.add(i);
					continue;
				}

				l.add(createBranch(), createBranch(), createBranch());
				branchSpace += 0.001;

				for (int j = 0; j < 5; j++) {
					playLightningbendingParticle(l.clone(), 0f, 0f, 0f);
					JCMethods.emitLight(l.clone());

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

						for (int k = 0; k < 5; k++) {
							playLightningbendingParticle(entity.getLocation(), (float) Math.random(), (float) Math.random(), (float) Math.random());
							JCMethods.emitLight(entity.getLocation());
						}

						entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BEE_HURT, soundVolume, 0.2f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_HURT, soundVolume, 0.2f);

						return true;
					});

					l = l.add(direction.clone().multiply(0.2));
				}

				branches.put(i, l);
			}
		}
		
		for (int i : cleanup) {
			branches.remove(i);
		}

		cleanup.clear();
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
