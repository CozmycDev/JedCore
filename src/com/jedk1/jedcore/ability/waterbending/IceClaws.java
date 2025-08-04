package com.jedk1.jedcore.ability.waterbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.potion.PotionEffectType;

public class IceClaws extends IceAbility implements AddonAbility {

	@Attribute(Attribute.COOLDOWN)
	private long punchCooldown;
	@Attribute(Attribute.COOLDOWN)
	private long throwCooldown;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeUp;
	private int punchSlowDur;
	private int throwSlowDur;
	private int punchSlownessLevel;
	private int throwSlownessLevel;
	@Attribute(Attribute.DAMAGE)
	private double punchDamage;
	@Attribute(Attribute.DAMAGE)
	private double throwDamage;
	@Attribute(Attribute.RANGE)
	private double range;
	private boolean throwable;
	private double throwSpeed;

	private Location head;
	private Location origin;
	private boolean launched;

	private Boolean iceInMainHand = null;

	public IceClaws(Player player) {
		super(player);
		if (!bPlayer.canBend(this) || !bPlayer.canIcebend()) {
			return;
		}

		if (hasAbility(player, IceClaws.class)) {
			IceClaws ic = getAbility(player, IceClaws.class);
			if (!ic.throwable) {
				ic.remove();
			}
			return;
		}

		setFields();
		start();
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		punchCooldown = config.getLong("Abilities.Water.IceClaws.Punch.Cooldown", 4000);
		punchDamage = config.getDouble("Abilities.Water.IceClaws.Punch.Damage", 2.0);
		punchSlownessLevel = config.getInt("Abilities.Water.IceClaws.Punch.Slowness", 3);
		punchSlowDur = config.getInt("Abilities.Water.IceClaws.Punch.SlowDuration", 5000);

		throwCooldown = config.getLong("Abilities.Water.IceClaws.Throw.Cooldown", 4000);
		throwDamage = config.getDouble("Abilities.Water.IceClaws.Throw.Damage", 2.0);
		throwSlownessLevel = config.getInt("Abilities.Water.IceClaws.Throw.Slowness", 3);
		throwSlowDur = config.getInt("Abilities.Water.IceClaws.Throw.SlowDuration", 5000);
		throwSpeed = config.getDouble("Abilities.Water.IceClaws.Throw.Speed", 1.0);

		chargeUp = config.getLong("Abilities.Water.IceClaws.ChargeTime", 1000);
		range = config.getDouble("Abilities.Water.IceClaws.Range", 10);
		throwable = config.getBoolean("Abilities.Water.IceClaws.Throwable", true);

		applyModifiers();
	}
	
	private void applyModifiers() {
		punchCooldown -= ((long) getNightFactor(punchCooldown) - punchCooldown);
		throwCooldown -= ((long) getNightFactor(throwCooldown) - throwCooldown);
		punchDamage = getNightFactor(punchDamage);
		throwDamage = getNightFactor(throwDamage);
		range = getNightFactor(range);
	}

	@Override
	public void progress() {
		if (player == null || player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + chargeUp) {
			if (!launched && throwable) {
				displayClaws();
			} else {
				if (!shoot()) {
					remove();
				}
			}
		} else if (player.isSneaking()) {
			displayChargeUp();
		} else {
			remove();
		}
	}


	public static void swapHands(Player player) {
		ConfigurationSection config = JedCoreConfig.getConfig(player);
		if (!config.getBoolean("Abilities.Water.IceClaws.AllowHandSwap", true)) return;
		IceClaws ic = getAbility(player, IceClaws.class);
		if (ic == null)
			return;
		if (ic.iceInMainHand == null)
			ic.iceInMainHand = true;
		else ic.iceInMainHand = !ic.iceInMainHand;
	}

	public Location getRightHandPos() {
		return (player.getMainHand() == MainHand.RIGHT == ((iceInMainHand == null) || iceInMainHand) ?
				GeneralMethods.getRightSide(player.getLocation(), .55) :
				GeneralMethods.getLeftSide(player.getLocation(), .55)).add(0, 1.2, 0);
	}

	public boolean shoot() {
		for (double i = 0; i < 1; i += throwSpeed) {
			head.add(origin.clone().getDirection().multiply(throwSpeed));
			if (origin.distance(head) >= range) return false;
			if (!isTransparent(head.getBlock())) return false;
			GeneralMethods.displayColoredParticle("66FFFF", head);
			GeneralMethods.displayColoredParticle("CCFFFF", head);
			ParticleEffect.SNOW_SHOVEL.display(head, 1, 0, 0, 0, 0);
			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(head, 1.5)) {
				if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId() && !(entity instanceof ArmorStand)) {
					freezeEntity((LivingEntity) entity, false);
					return false;
				}
			}
		}
		return true;
	}

	public static void throwClaws(Player player) {
		if (hasAbility(player, IceClaws.class)) {
			IceClaws ic = getAbility(player, IceClaws.class);
			if (!ic.launched && player.isSneaking()) {
				ic.launched = true;
				ic.origin = ic.player.getEyeLocation();
				ic.head = ic.origin.clone();
			}
		}
	}

	private void displayClaws() {
		Location location = getRightHandPos().toVector().add(player.getEyeLocation().getDirection().clone().multiply(.75D)).toLocation(player.getWorld());
		GeneralMethods.displayColoredParticle("66FFFF", location);
		GeneralMethods.displayColoredParticle("CCFFFF", location);
	}

	private void displayChargeUp() {
		Location location = getRightHandPos().toVector().add(player.getEyeLocation().getDirection().clone().multiply(.75D)).toLocation(player.getWorld());
		ParticleEffect.WATER_SPLASH.display(location, 1, Math.random()/3, Math.random()/3, Math.random()/3, 0.0);
	}

	public static boolean freezeEntity(Player player, LivingEntity entity) {
		if (hasAbility(player, IceClaws.class)) {
			getAbility(player, IceClaws.class).freezeEntity(entity);
			return true;
		}
		return false;
	}

	private void freezeEntity(LivingEntity entity) {
		freezeEntity(entity, true);
	}

	private void freezeEntity(LivingEntity entity, boolean isPunch) {
		if (entity.hasPotionEffect(PotionEffectType.SPEED)) {
			entity.removePotionEffect(PotionEffectType.SPEED);
		}
		int duration = isPunch ? punchSlowDur : throwSlowDur;
		int level = isPunch ? punchSlownessLevel : throwSlownessLevel;
		entity.addPotionEffect(JedCore.plugin.getPotionEffectAdapter().getSlownessEffect(duration, level));
		bPlayer.addCooldown(this);
		remove();
		DamageHandler.damageEntity(entity, isPunch ? punchDamage : throwDamage, this);
	}
	
	@Override
	public long getCooldown() {
		return launched ? throwCooldown : punchCooldown;
	}

	@Override
	public Location getLocation() {
		return head;
	}

	@Override
	public String getName() {
		return "IceClaws";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Water.IceClaws.Description");
	}

	public void setPunchCooldown(long punchCooldown) { this.punchCooldown = punchCooldown; }
	public long getPunchCooldown() { return punchCooldown; }
	public void setThrowCooldown(long throwCooldown) { this.throwCooldown = throwCooldown; }
	public long getThrowCooldown() { return throwCooldown; }
	public void setPunchDamage(double punchDamage) { this.punchDamage = punchDamage; }
	public double getPunchDamage() { return punchDamage; }
	public void setThrowDamage(double throwDamage) { this.throwDamage = throwDamage; }
	public double getThrowDamage() { return throwDamage; }
	public void setPunchSlowDur(int punchSlowDur) { this.punchSlowDur = punchSlowDur; }
	public int getPunchSlowDur() { return punchSlowDur; }
	public void setThrowSlowDur(int throwSlowDur) { this.throwSlowDur = throwSlowDur; }
	public int getThrowSlowDur() { return throwSlowDur; }

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public boolean isThrowable() {
		return throwable;
	}

	public void setThrowable(boolean throwable) {
		this.throwable = throwable;
	}

	public Location getHead() {
		return head;
	}

	public void setHead(Location head) {
		this.head = head;
	}

	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(Location origin) {
		this.origin = origin;
	}

	public boolean isLaunched() {
		return launched;
	}

	public void setLaunched(boolean launched) {
		this.launched = launched;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.IceClaws.Enabled");
	}

	public int getPunchSlownessLevel() {
		return punchSlownessLevel;
	}

	public void setPunchSlownessLevel(int punchSlownessLevel) {
		this.punchSlownessLevel = punchSlownessLevel;
	}

	public int getThrowSlownessLevel() {
		return throwSlownessLevel;
	}

	public void setThrowSlownessLevel(int throwSlownessLevel) {
		this.throwSlownessLevel = throwSlownessLevel;
	}

	public double getThrowSpeed() {
		return throwSpeed;
	}

	public void setThrowSpeed(double throwSpeed) {
		this.throwSpeed = throwSpeed;
	}
}