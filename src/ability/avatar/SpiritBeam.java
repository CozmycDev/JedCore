package com.jedk1.jedcore.ability.avatar;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpiritBeam extends AvatarAbility implements AddonAbility {

    private Location location;
	private Vector direction;
	private boolean damagesBlocks;
	private long regen;
	private boolean avatarOnly;

	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.RADIUS)
	private double radius;

	public SpiritBeam(Player player) {
		super(player);

		if (bPlayer.isOnCooldown(this)) return;

		setFields();

		if (avatarOnly && !bPlayer.isAvatarState()) return;

		start();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		duration = config.getInt("Abilities.Avatar.SpiritBeam.Duration");
		cooldown = config.getInt("Abilities.Avatar.SpiritBeam.Cooldown");
		damage = config.getDouble("Abilities.Avatar.SpiritBeam.Damage");
		range = config.getInt("Abilities.Avatar.SpiritBeam.Range");
		avatarOnly = config.getBoolean("Abilities.Avatar.SpiritBeam.AvatarStateOnly");
		damagesBlocks = config.getBoolean("Abilities.Avatar.SpiritBeam.BlockDamage.Enabled");
		regen = config.getLong("Abilities.Avatar.SpiritBeam.BlockDamage.Regen");
		radius = config.getDouble("Abilities.Avatar.SpiritBeam.BlockDamage.Radius");
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (System.currentTimeMillis() > getStartTime() + duration) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (!player.isSneaking()) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (avatarOnly && !bPlayer.isAvatarState()) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		createBeam();
	}

	private void createBeam() {
		location = player.getLocation().add(0, 1.2, 0);
		Vector beamDirection = location.getDirection().normalize().multiply(0.5);

		for (double i = 0; i < range; i += 0.5) {
			location = location.add(beamDirection);

			if (isBeamObstructed(location)) {
				return;
			}

			displayBeamParticles(location, beamDirection);
			JCMethods.emitLight(location);
			damageNearbyEntities(location);

			if (handleBlockCollision(location)) {
				return;
			}
		}
	}

	private boolean isBeamObstructed(Location location) {
		return RegionProtection.isRegionProtected(player, location, this);
	}

	private void displayBeamParticles(Location location, Vector direction) {
		String purple = "#A020F0";
		JCMethods.displayColoredParticles(purple, location, 1, 0f, 0f, 0f, 0f);
		JCMethods.displayColoredParticles(purple, location, 1, (float) Math.random() / 3, (float) Math.random() / 3, (float) Math.random() / 3, 0f);

		float randomOffset = (float) Math.random() / 3;
		ParticleEffect.BLOCK_CRACK.display(location, 1, randomOffset, randomOffset, randomOffset, 0.1F, Material.NETHER_PORTAL.createBlockData());
		ParticleEffect.BLOCK_CRACK.display(location, 1, (float) direction.getX(), (float) direction.getY(), (float) direction.getZ(), 0.1F, Material.NETHER_PORTAL.createBlockData());
	}

	private void damageNearbyEntities(Location location) {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 2)) {
			if (entity instanceof LivingEntity livingEntity && livingEntity.getEntityId() != player.getEntityId()) {
				livingEntity.setFireTicks(100);
				DamageHandler.damageEntity(livingEntity, damage, this);
			}
		}
	}

	private boolean handleBlockCollision(Location location) {
		if (location.getBlock().getType().isSolid()) {
			location.getWorld().createExplosion(location, 0F);
			if (damagesBlocks) {
				damageBlocksInRadius(location);
			}
			return true;
		}
		return false;
	}

	private void damageBlocksInRadius(Location center) {
		for (Location loc : GeneralMethods.getCircle(center, (int) radius, 0, false, true, 0)) {
			if (!JCMethods.isUnbreakable(loc.getBlock())) {
				new RegenTempBlock(loc.getBlock(), Material.AIR, Material.AIR.createBlockData(), regen, false);
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
		return "SpiritBeam";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Avatar.SpiritBeam.Description");
	}

	public Vector getDirection() {
		return direction;
	}

	public void setDirection(Vector direction) {
		this.direction = direction;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public boolean isAvatarOnly() {
		return avatarOnly;
	}

	public void setAvatarOnly(boolean avatarOnly) {
		this.avatarOnly = avatarOnly;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public boolean damagesBlocks() {
		return damagesBlocks;
	}

	public void setDamagesBlocks(boolean blockdamage) {
		this.damagesBlocks = blockdamage;
	}

	public long getRegen() {
		return regen;
	}

	public void setRegen(long regen) {
		this.regen = regen;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Avatar.SpiritBeam.Enabled");
	}
}
