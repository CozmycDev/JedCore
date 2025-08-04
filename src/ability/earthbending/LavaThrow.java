package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentHashMap;

public class LavaThrow extends LavaAbility implements AddonAbility {
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.SELECT_RANGE)
	private int sourceRange;
	private long sourceRegen;
	@Attribute("MaxShots")
	private int shotMax;
	@Attribute(Attribute.FIRE_TICK)
	private int fireTicks;
	@Attribute("CurveFactor")
	private double curveFactor;

	private Location location;
	private int shots;
	private Block selectedSource;
	private boolean isInitialState = true;

	private final ConcurrentHashMap<Location, Location> blasts = new ConcurrentHashMap<>();

	public LavaThrow(Player player) {
		super(player);

		if (!bPlayer.canBend(this) || !bPlayer.canLavabend()) {
			return;
		}

		setFields();

		location = player.getLocation();
		location.setPitch(0);

		if (prepare()) {
			player.getWorld().playSound(selectedSource.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1.0f, 1.0f);
			start();
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Earth.LavaThrow.Cooldown");
		range = config.getInt("Abilities.Earth.LavaThrow.Range");
		damage = config.getDouble("Abilities.Earth.LavaThrow.Damage");
		sourceRange = config.getInt("Abilities.Earth.LavaThrow.SourceGrabRange");
		sourceRegen = config.getLong("Abilities.Earth.LavaThrow.SourceRegenDelay");
		shotMax = config.getInt("Abilities.Earth.LavaThrow.MaxShots");
		fireTicks = config.getInt("Abilities.Earth.LavaThrow.FireTicks");
		curveFactor = config.getDouble("Abilities.Earth.LavaThrow.CurveFactor");
	}

	@Override
	public void progress() {
		if (player == null || player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (!bPlayer.getBoundAbilityName().equalsIgnoreCase("LAVATHROW")) {
			remove();
			if (shots > 0) bPlayer.addCooldown(this);
			return;
		}

		if (player.getLocation().distance(selectedSource.getLocation()) >= sourceRange) {
			remove();
			if (shots > 0) bPlayer.addCooldown(this);
			return;
		}

		if (blasts.isEmpty() && shots >= shotMax && !isInitialState) {
			remove();
			bPlayer.addCooldown(this);
			return;
		}

		selectedSource.getWorld().spawnParticle(Particle.FLAME, selectedSource.getLocation(), 2, 0.3, 1.0, 0.3, 0.05);
		selectedSource.getWorld().spawnParticle(Particle.LAVA, selectedSource.getLocation(), 2, 0.2, 0.2, 0.2, 0);

		handleBlasts();
	}

	private boolean prepare() {
		Block targetBlock = getTargetLavaBlock(sourceRange);

		if (targetBlock != null) {
			selectedSource = targetBlock;
		}

		return selectedSource != null;
	}

	public Block getTargetLavaBlock(int maxDistance) {
		Location eyeLocation = player.getEyeLocation();
		Vector direction = eyeLocation.getDirection();
		World world = player.getWorld();

		RayTraceResult result = world.rayTraceBlocks(
				eyeLocation, direction, maxDistance,
				FluidCollisionMode.ALWAYS, true
		);

		if (result != null) {
			Block hitBlock = result.getHitBlock();
			if (LavaAbility.isLava(hitBlock)) {
				return hitBlock;
			}
		}
		return null;
	}

	public void createBlast() {
		if (selectedSource != null && shots < shotMax) {
			isInitialState = false;
			shots++;

			if (shots >= shotMax) {
				bPlayer.addCooldown(this);
			}

			Location origin = selectedSource.getLocation().clone().add(0, 2, 0);
			player.getWorld().playSound(origin, Sound.ITEM_BUCKET_EMPTY_LAVA, 1.0f, 1.0f);
			double viewRange = range + origin.distance(player.getEyeLocation());
			Location viewTarget = GeneralMethods.getTargetedLocation(player, viewRange, Material.WATER, Material.LAVA);
			Vector direction = viewTarget.clone().subtract(origin).toVector().normalize();
			Location head = origin.clone();

			head.setDirection(direction);
			blasts.put(head, origin);

			new RegenTempBlock(selectedSource.getRelative(BlockFace.UP), Material.LAVA,
					Material.LAVA.createBlockData(bd -> ((Levelled)bd).setLevel(0)), 200);
			new RegenTempBlock(selectedSource, Material.AIR, Material.AIR.createBlockData(), sourceRegen, false);
		}
	}

	public void handleBlasts() {
		for (Location l : blasts.keySet()) {
			Location head = l.clone();
			Location origin = blasts.get(l);

			if (l.distance(origin) > range) {
				blasts.remove(l);
				continue;
			}

			if (GeneralMethods.isSolid(l.getBlock())) {
				blasts.remove(l);
				continue;
			}

			Vector currentDirection = head.getDirection();
			Vector playerLookDirection = player.getEyeLocation().getDirection();

			Vector curveVector = playerLookDirection.clone()
					.subtract(currentDirection)
					.multiply(curveFactor);

			Vector newDirection = currentDirection.clone()
					.add(curveVector)
					.normalize();

			head.setDirection(newDirection);
			head = head.add(newDirection.multiply(1));

			new RegenTempBlock(l.getBlock(), Material.LAVA, Material.LAVA.createBlockData(bd -> ((Levelled)bd).setLevel(0)), 200);
			ParticleEffect.LAVA.display(head, 1, Math.random(), Math.random(), Math.random(), 0);

			boolean hit = false;

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(l, 2.0D)) {
				if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !RegionProtection.isRegionProtected(this, entity.getLocation()) && !((entity instanceof Player) && Commands.invincible.contains(((Player) entity).getName()))) {
					DamageHandler.damageEntity(entity, damage, this);
					blasts.remove(l);

					hit = true;
					entity.setFireTicks(this.fireTicks);
				}
			}

			if (!hit) {
				blasts.remove(l);
				blasts.put(head, origin);
			}
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
	public String getName() {
		return "LavaThrow";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.LavaThrow.Description");
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public int getSourceRange() {
		return sourceRange;
	}

	public void setSourceRange(int sourceRange) {
		this.sourceRange = sourceRange;
	}

	public long getSourceRegen() {
		return sourceRegen;
	}

	public void setSourceRegen(long sourceRegen) {
		this.sourceRegen = sourceRegen;
	}

	public int getShotMax() {
		return shotMax;
	}

	public void setShotMax(int shotMax) {
		this.shotMax = shotMax;
	}

	public int getFireTicks() {
		return fireTicks;
	}

	public void setFireTicks(int fireTicks) {
		this.fireTicks = fireTicks;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public int getShots() {
		return shots;
	}

	public void setShots(int shots) {
		this.shots = shots;
	}

	public ConcurrentHashMap<Location, Location> getBlasts() {
		return blasts;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.LavaThrow.Enabled");
	}
}
