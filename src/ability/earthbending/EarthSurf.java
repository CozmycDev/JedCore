package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.MaterialUtil;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.TempBlock;

import com.projectkorra.projectkorra.util.TempFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class EarthSurf extends EarthAbility implements AddonAbility {
	private static final double TARGET_HEIGHT = 1.5;

	private Location location;
	private double prevHealth;

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private long minimumCooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	private boolean cooldownEnabled;
	private boolean durationEnabled;
	private boolean removeOnAnyDamage;
	@Attribute(Attribute.SPEED)
	private double speed;
	private double springStiffness;
	private final Set<Block> ridingBlocks = new HashSet<>();
	private CollisionDetector collisionDetector = new DefaultCollisionDetector();
	private DoubleSmoother heightSmoother;

	public EarthSurf(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		if (hasAbility(player, EarthSurf.class)) {
			getAbility(player, EarthSurf.class).remove();
			return;
		}

		setFields();

		location = player.getLocation();

		if (canStart()) {
			prevHealth = player.getHealth();

			this.flightHandler.createInstance(player, this.getName());
			player.setAllowFlight(true);
			player.setFlying(false);
			start();
		}
	}

	private boolean canStart() {
		Block beneath = getBlockBeneath(player.getLocation().clone());
		double maxHeight = getMaxHeight();

		return isEarthbendable(player, beneath) && !isMetal(beneath) && beneath.getLocation().distanceSquared(player.getLocation()) <= maxHeight * maxHeight;
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Earth.EarthSurf.Cooldown.Cooldown");
		minimumCooldown = config.getLong("Abilities.Earth.EarthSurf.Cooldown.MinimumCooldown");
		duration = config.getLong("Abilities.Earth.EarthSurf.Duration.Duration");
		cooldownEnabled = config.getBoolean("Abilities.Earth.EarthSurf.Cooldown.Enabled");
		durationEnabled = config.getBoolean("Abilities.Earth.EarthSurf.Duration.Enabled");
		removeOnAnyDamage = config.getBoolean("Abilities.Earth.EarthSurf.RemoveOnAnyDamage");
		speed = config.getDouble("Abilities.Earth.EarthSurf.Speed");
		springStiffness = config.getDouble("Abilities.Earth.EarthSurf.SpringStiffness");

		int smootherSize = config.getInt("Abilities.Earth.EarthSurf.HeightTolerance");
		this.heightSmoother = new DoubleSmoother(Math.max(smootherSize, 1));

		if (config.getBoolean("Abilities.Earth.EarthSurf.RelaxedCollisions")) {
			this.collisionDetector = new RelaxedCollisionDetector();
		}

		if (!config.getBoolean("Abilities.Earth.EarthSurf.Cooldown.Scaled")) {
			minimumCooldown = cooldown;
		}
	}

	@Override
	public void progress() {
		if (shouldRemove()) {
			remove();
			return;
		}

		this.player.setFlying(false);

		if (!collisionDetector.isColliding(player) && player.getHealth() >= prevHealth) {
			movePlayer();

			if (removeOnAnyDamage) {
				prevHealth = player.getHealth();
			}
		} else {
			remove();
		}
	}

	private boolean shouldRemove() {
		if (player == null || player.isDead() || !player.isOnline()) return true;
		if (!bPlayer.canBendIgnoreCooldowns(this)) return true;
		if (!isEarthbendable(player, getBlockBeneath(player.getLocation().clone()))) return true;
		if (durationEnabled && System.currentTimeMillis() > getStartTime() + duration) return true;

		return player.isSneaking();
	}

	private void movePlayer() {
		location = player.getEyeLocation().clone();
		location.setPitch(0);
		Vector direction = location.getDirection().normalize();

		// How far the player is above the ground.
		double height = getPlayerDistance();
		double maxHeight = getMaxHeight();
		double smoothedHeight = heightSmoother.add(height);

		// Destroy ability if player gets too far from ground.
		if (smoothedHeight > maxHeight) {
			remove();
			return;
		}

		// Calculate the spring force to push the player back to the target height.
		double displacement = height - TARGET_HEIGHT;
		double force = -springStiffness * displacement;

		double maxForce = 0.5;
		if (Math.abs(force) > maxForce) {
			// Cap the force to maxForce so the player isn't instantly pulled to the ground.
			force = force / Math.abs(force) * maxForce;
		}

		Vector velocity = direction.clone().multiply(speed).setY(force);

		rideWave();

		player.setVelocity(velocity);
		player.setFallDistance(0);
	}

	private double getMaxHeight() {
		return TARGET_HEIGHT + 2.0;
	}

	private double getPlayerDistance() {
		Location l = player.getLocation().clone();
		while (true) {
			if (l.getBlockY() <= l.getWorld().getMinHeight()) break;
			if (ElementalAbility.isAir(l.getBlock().getType()) && ridingBlocks.contains(l.getBlock())) break;
			if (GeneralMethods.isSolid(l.getBlock())) break;

			l.add(0, -0.1, 0);
		}
		return player.getLocation().getY() - l.getY();
	}

	private Block getBlockBeneath(Location l) {
		while (l.getBlockY() > l.getWorld().getMinHeight() && MaterialUtil.isTransparent(l.getBlock())) {
			l.add(0, -0.5, 0);
		}
		return l.getBlock();
	}

	private void rideWave() {
		for (int i = 0; i < 3; i++) {
			Location loc = location.clone();
			if (i < 2)
				loc.add(getSideDirection(i));

			//Player Positioning
			double distOffset = 2.5;
			Location bL = loc.clone().add(0, -2.9, 0).toVector().add(location.clone().getDirection().multiply(distOffset)).toLocation(player.getWorld());
			while (!ElementalAbility.isAir(loc.clone().add(0, -2.9, 0).toVector().add(location.clone().getDirection().multiply(distOffset)).toLocation(player.getWorld()).getBlock().getType())) {
				loc.add(0, 0.1, 0);
			}

			if (isEarthbendable(player, getBlockBeneath(loc.clone().add(0, -2.9, 0).toVector().add(location.clone().getDirection().multiply(distOffset)).toLocation(player.getWorld()))) && getBlockBeneath(bL) != null) {
				Block block = loc.clone().add(0, -3.9, 0).toVector().add(location.clone().getDirection().multiply(distOffset - 0.5)).toLocation(player.getWorld()).getBlock();
				Location temp = loc.clone().add(0, -2.9, 0).toVector().add(location.clone().getDirection().multiply(distOffset)).toLocation(player.getWorld());

				if (RegionProtection.isRegionProtected(this, block.getLocation())) {
					continue;
				}
				// Don't render blocks above the player because it looks bad.
				// TODO: Change this check to see if it's a reachable position instead.
				if (block.getLocation().getY() > this.player.getLocation().getY()) {
					continue;
				}

				if (DensityShift.isPassiveSand(block)) {
					DensityShift.revertSand(block);
				}

				if (!GeneralMethods.isSolid(block.getLocation().add(0, 1, 0).getBlock()) && !ElementalAbility.isAir(block.getLocation().add(0, 1, 0).getBlock().getType())) {
					if (DensityShift.isPassiveSand(block.getRelative(BlockFace.UP))) {
						DensityShift.revertSand(block.getRelative(BlockFace.UP));
					}

					new TempBlock(block.getRelative(BlockFace.UP), Material.AIR.createBlockData());
				}

				if (GeneralMethods.isSolid(block)) {
					ridingBlocks.add(block);
					new RegenTempBlock(block, Material.AIR, Material.AIR.createBlockData(), 1000L, true, ridingBlocks::remove);
				} else {
					new RegenTempBlock(block, Material.AIR, Material.AIR.createBlockData(), 1000L);
				}

				new TempFallingBlock(temp, getBlockBeneath(bL).getBlockData(), new Vector(0, 0.25, 0), this, true);
				
				for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc.clone().add(0, -2.9, 0).toVector().add(location.clone().getDirection().multiply(distOffset)).toLocation(player.getWorld()), 1.5D)) {
					if (e instanceof LivingEntity && e.getEntityId() != player.getEntityId()) {
						e.setVelocity(new Vector(0, 0.3, 0));
					}
				}
			}
		}
	}

	private Vector getSideDirection(int side) {
		Vector direction = location.clone().getDirection().normalize();
		switch (side) {
			case 0: // RIGHT
				return new Vector(-direction.getZ(), 0.0, direction.getX()).normalize();
			case 1: // LEFT
				return new Vector(direction.getZ(), 0.0, -direction.getX()).normalize();
			default:
				break;
		}

		return null;
	}

	@Override
	public void remove() {
		this.flightHandler.removeInstance(player, this.getName());

		if (cooldownEnabled && player.isOnline()) {
			long scaledCooldown = cooldown;

			if (durationEnabled && duration > 0) {
				double t = Math.min((System.currentTimeMillis() - this.getStartTime()) / (double) duration, 1.0);
				scaledCooldown = Math.max((long) (cooldown * t), minimumCooldown);
			}

			bPlayer.addCooldown(this, scaledCooldown);
		}

		super.remove();
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
		return "EarthSurf";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.EarthSurf.Description");
	}

	public static double getTargetHeight() {
		return TARGET_HEIGHT;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public double getPrevHealth() {
		return prevHealth;
	}

	public void setPrevHealth(double prevHealth) {
		this.prevHealth = prevHealth;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getMinimumCooldown() {
		return minimumCooldown;
	}

	public void setMinimumCooldown(long minimumCooldown) {
		this.minimumCooldown = minimumCooldown;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public boolean isCooldownEnabled() {
		return cooldownEnabled;
	}

	public void setCooldownEnabled(boolean cooldownEnabled) {
		this.cooldownEnabled = cooldownEnabled;
	}

	public boolean isDurationEnabled() {
		return durationEnabled;
	}

	public void setDurationEnabled(boolean durationEnabled) {
		this.durationEnabled = durationEnabled;
	}

	public boolean isRemoveOnAnyDamage() {
		return removeOnAnyDamage;
	}

	public void setRemoveOnAnyDamage(boolean removeOnAnyDamage) {
		this.removeOnAnyDamage = removeOnAnyDamage;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public double getSpringStiffness() {
		return springStiffness;
	}

	public void setSpringStiffness(double springStiffness) {
		this.springStiffness = springStiffness;
	}

	public Set<Block> getRidingBlocks() {
		return ridingBlocks;
	}

	public CollisionDetector getCollisionDetector() {
		return collisionDetector;
	}

	public void setCollisionDetector(CollisionDetector collisionDetector) {
		this.collisionDetector = collisionDetector;
	}

	public DoubleSmoother getHeightSmoother() {
		return heightSmoother;
	}

	public void setHeightSmoother(DoubleSmoother heightSmoother) {
		this.heightSmoother = heightSmoother;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		return config.getBoolean("Abilities.Earth.EarthSurf.Enabled");
	}

	private interface CollisionDetector {
		boolean isColliding(Player player);
	}

	private abstract static class AbstractCollisionDetector implements CollisionDetector {
		protected boolean isCollision(Location location) {
			Block block = location.getBlock();
			return !MaterialUtil.isTransparent(block) || block.isLiquid() || block.getType().isSolid();
		}
	}

	private class DefaultCollisionDetector extends AbstractCollisionDetector {
		@Override
		public boolean isColliding(Player player) {
			// The location in front of the player, where the player will be in one second.
			Location front = player.getEyeLocation().clone();
			front.setPitch(0);

			Vector direction = front.getDirection().clone().setY(0).normalize();
			double playerSpeed = player.getVelocity().clone().setY(0).length();

			front.add(direction.clone().multiply(Math.max(speed, playerSpeed)));

			for (int i = 0; i < 3; ++i) {
				Location location = front.clone().add(0, -i, 0);
				if (isCollision(location)) {
					return true;
				}
			}

			return false;
		}
	}

	private class RelaxedCollisionDetector extends AbstractCollisionDetector {
		@Override
		public boolean isColliding(Player player) {
			// The location in front of the player, where the player will be in one second.
			Location front = player.getEyeLocation().clone().subtract(0.0, 0.5, 0.0);
			front.setPitch(0);

			Vector direction = front.getDirection().clone().setY(0).normalize();
			double playerSpeed = player.getVelocity().clone().setY(0).length();

			front.add(direction.clone().multiply(Math.max(speed, playerSpeed)));

			return isCollision(front);
		}
	}

	private static class DoubleSmoother {
		private final double[] values;
		private final int size;
		private int index;

		public DoubleSmoother(int size) {
			this.size = size;
			this.index = 0;

			values = new double[size];
		}

		public double add(double value) {
			values[index] = value;
			index = (index + 1) % size;
			return get();
		}

		public double get() {
			return Arrays.stream(this.values).sum() / this.size;
		}
	}
}
