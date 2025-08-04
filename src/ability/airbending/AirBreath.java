package com.jedk1.jedcore.ability.airbending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class AirBreath extends AirAbility implements AddonAbility {

	private int particles;
	private boolean coolLava;
	private boolean extinguishFire;
	private boolean extinguishMobs;
	private boolean damageEnabled;
	private double launch;
	private boolean regenOxygen;
	private boolean avatarAmplify;
	private int avatarRange;
	private double avatarKnockback;

    @Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.DAMAGE)
	private double playerDamage;
	@Attribute(Attribute.DAMAGE)
	private double mobDamage;
	@Attribute(Attribute.KNOCKBACK)
	private double knockback;
	@Attribute(Attribute.RANGE)
	private int range;

	public AirBreath(Player player) {
		super(player);
		if (!bPlayer.canBend(this)) {
			return;
		}

		setFields();

		if (bPlayer.isAvatarState() && avatarAmplify) {
			range = avatarRange;
			knockback = avatarKnockback;
		}
		start();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Air.AirBreath.Cooldown");
		duration = config.getLong("Abilities.Air.AirBreath.Duration");
		particles = config.getInt("Abilities.Air.AirBreath.Particles");
		coolLava = config.getBoolean("Abilities.Air.AirBreath.AffectBlocks.Lava");
		extinguishFire = config.getBoolean("Abilities.Air.AirBreath.AffectBlocks.Fire");
		extinguishMobs = config.getBoolean("Abilities.Air.AirBreath.ExtinguishEntities");
		damageEnabled = config.getBoolean("Abilities.Air.AirBreath.Damage.Enabled");
		playerDamage = config.getDouble("Abilities.Air.AirBreath.Damage.Player");
		mobDamage = config.getDouble("Abilities.Air.AirBreath.Damage.Mob");
		knockback = config.getDouble("Abilities.Air.AirBreath.Knockback");
		range = config.getInt("Abilities.Air.AirBreath.Range");
		launch = config.getDouble("Abilities.Air.AirBreath.LaunchPower");
		regenOxygen = config.getBoolean("Abilities.Air.AirBreath.RegenTargetOxygen");
		avatarAmplify = config.getBoolean("Abilities.Air.AirBreath.Avatar.Enabled");
		avatarRange = config.getInt("Abilities.Air.AirBreath.Avatar.Range");
		avatarKnockback = config.getDouble("Abilities.Air.AirBreath.Avatar.Knockback");
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (!(bPlayer.getBoundAbility() instanceof AirBreath)) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (!player.isSneaking()) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}

		if (System.currentTimeMillis() < getStartTime() + duration) {
			playAirbendingSound(player.getLocation());
			createBeam();
		} else {
			bPlayer.addCooldown(this);
			remove();
		}
	}

	private boolean isLocationSafe(Location loc) {
		Block block = loc.getBlock();

		if (RegionProtection.isRegionProtected(player, loc, this)) {
			return false;
		}

		return isTransparent(block);
	}

	private void createBeam() {
		Location loc = player.getEyeLocation();
		Vector dir = player.getLocation().getDirection();
		double step = 1;
		double size = 0;
		double damageregion = 1.5;

		for (double i = 0; i < range; i += step) {
			loc = loc.add(dir.clone().multiply(step));
			size += 0.005;
			damageregion += 0.01;

			if (!handleLocationSafety(loc)) {
				return;
			}

			handleEntityCollisions(loc, dir, damageregion);
			displayBeamParticles(loc, size);
			handleBlockEffects();
		}
	}

	private boolean handleLocationSafety(Location loc) {
		if (!isLocationSafe(loc)) {
			if (!isTransparent(loc.getBlock()) && player.getLocation().getPitch() > 30) {
				GeneralMethods.setVelocity(this, player, player.getLocation().getDirection().multiply(-launch));
			}
			return false;
		}
		return true;
	}

	private void handleEntityCollisions(Location loc, Vector dir, double damageregion) {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, damageregion)) {
			if (isValidTarget(entity)) {
				if (isEntityProtected(entity)) {
					continue;
				}
				applyEntityEffects(entity, dir);
			}
		}
	}

	private boolean isValidTarget(Entity entity) {
		return entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand);
	}

	private boolean isEntityProtected(Entity entity) {
		return RegionProtection.isRegionProtected(this, entity.getLocation()) || (entity instanceof Player && Commands.invincible.contains(entity.getName()));
	}

	private void applyEntityEffects(Entity entity, Vector dir) {
		if (entity instanceof LivingEntity livingEntity) {
			applyDamage(livingEntity);
			applyOxygenRegen(livingEntity);
			if (extinguishMobs) {
				livingEntity.setFireTicks(0);
			}
		}
		dir.multiply(knockback);
		GeneralMethods.setVelocity(this, entity, dir);
	}

	private void applyDamage(LivingEntity entity) {
		if (damageEnabled) {
			if (entity instanceof Player) {
				DamageHandler.damageEntity(entity, playerDamage, this);
			} else {
				DamageHandler.damageEntity(entity, mobDamage, this);
			}
		}
	}

	private void applyOxygenRegen(LivingEntity entity) {
		if (regenOxygen && isWater(entity.getLocation().getBlock()) && !entity.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
			entity.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100, 2));
		}
	}

	private void displayBeamParticles(Location loc, double size) {
		if (isWater(loc.getBlock())) {
			ParticleEffect.WATER_BUBBLE.display(loc, particles, Math.random(), Math.random(), Math.random(), size);
		}

		ParticleEffect mainParticle = getAirbendingParticles();
		if (mainParticle == ParticleEffect.CLOUD) {
			ParticleEffect.CLOUD.display(loc, particles, Math.random(), Math.random(), Math.random(), size);
			JCMethods.displayColoredParticles("#FFFFFF", loc, particles, Math.random(), Math.random(), Math.random(), 0f);
			JCMethods.displayColoredParticles("#FFFFFF", player.getLocation(), particles, Math.random(), Math.random(), Math.random(), size, 50);
		} else if (mainParticle != null) {
			mainParticle.display(loc, particles, Math.random(), Math.random(), Math.random(), size);
		}
	}

	private void handleBlockEffects() {
		JCMethods.extinguishBlocks(player, "AirBreath", range, 2, extinguishFire, coolLava);
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return player.getEyeLocation();
	}

	@Override
	public String getName() {
		return "AirBreath";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Air.AirBreath.Description");
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public int getParticles() {
		return particles;
	}

	public void setParticles(int particles) {
		this.particles = particles;
	}

	public boolean isCoolLava() {
		return coolLava;
	}

	public void setCoolLava(boolean coolLava) {
		this.coolLava = coolLava;
	}

	public boolean canExtinguishFire() {
		return extinguishFire;
	}

	public void setExtinguishFire(boolean extinguishFire) {
		this.extinguishFire = extinguishFire;
	}

	public boolean canExtinguishMobs() {
		return extinguishMobs;
	}

	public void setExtinguishMobs(boolean extinguishMobs) {
		this.extinguishMobs = extinguishMobs;
	}

	public boolean isDamageEnabled() {
		return damageEnabled;
	}

	public void setDamageEnabled(boolean damageEnabled) {
		this.damageEnabled = damageEnabled;
	}

	public double getPlayerDamage() {
		return playerDamage;
	}

	public void setPlayerDamage(double playerDamage) {
		this.playerDamage = playerDamage;
	}

	public double getMobDamage() {
		return mobDamage;
	}

	public void setMobDamage(double mobDamage) {
		this.mobDamage = mobDamage;
	}

	public double getKnockback() {
		return knockback;
	}

	public void setKnockback(double knockback) {
		this.knockback = knockback;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public double getLaunch() {
		return launch;
	}

	public void setLaunch(double launch) {
		this.launch = launch;
	}

	public boolean canRegenOxygen() {
		return regenOxygen;
	}

	public void setRegenOxygen(boolean regenOxygen) {
		this.regenOxygen = regenOxygen;
	}

	public boolean isAvatarAmplify() {
		return avatarAmplify;
	}

	public void setAvatarAmplify(boolean avatarAmplify) {
		this.avatarAmplify = avatarAmplify;
	}

	public int getAvatarRange() {
		return avatarRange;
	}

	public void setAvatarRange(int avatarRange) {
		this.avatarRange = avatarRange;
	}

	public double getAvatarKnockback() {
		return avatarKnockback;
	}

	public void setAvatarKnockback(double avatarKnockback) {
		this.avatarKnockback = avatarKnockback;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Air.AirBreath.Enabled");
	}
}
