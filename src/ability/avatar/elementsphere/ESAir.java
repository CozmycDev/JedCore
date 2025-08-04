package com.jedk1.jedcore.ability.avatar.elementsphere;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ESAir extends AvatarAbility implements AddonAbility {

	private Location location;
	private double travelled;

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.KNOCKBACK)
	private double knockback;
	@Attribute(Attribute.SPEED)
	private int speed;

	public ESAir(Player player) {
		super(player);

		if (!hasAbility(player, ElementSphere.class)) {
			return;
		}

		ElementSphere currES = getAbility(player, ElementSphere.class);
		if (currES.getAirUses() == 0) {
			return;
		}

		if (bPlayer.isOnCooldown("ESAir")) {
			return;
		}

		setFields();

		if (RegionProtection.isRegionProtected(this, player.getTargetBlock(getTransparentMaterialSet(), (int) range).getLocation())) {
			return;
		}

		location = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().multiply(1));

		start();

		if (!isRemoved()) {
			bPlayer.addCooldown("ESAir", getCooldown());
			currES.setAirUses(currES.getAirUses() - 1);
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		cooldown = config.getLong("Abilities.Avatar.ElementSphere.Air.Cooldown");
		range = config.getDouble("Abilities.Avatar.ElementSphere.Air.Range");
		damage = config.getDouble("Abilities.Avatar.ElementSphere.Air.Damage");
		knockback = config.getDouble("Abilities.Avatar.ElementSphere.Air.Knockback");
		speed = config.getInt("Abilities.Avatar.ElementSphere.Air.Speed");
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (travelled >= range) {
			remove();
			return;
		}

		advanceAttack();
	}

	private void advanceAttack() {
		for (int i = 0; i < speed; i++) {
			travelled++;
			if (travelled >= range) return;

			location = location.add(location.getDirection().clone().multiply(1));

			if (RegionProtection.isRegionProtected(this, location)) {
				travelled = range;
				return;
			}

			if (GeneralMethods.isSolid(location.getBlock()) || isWater(location.getBlock())) {
				travelled = range;
				return;
			}

			AirAbility.playAirbendingParticles(location, 5);
			AirAbility.playAirbendingSound(location);

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 2.5)) {
				if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId()
						&& !(entity instanceof ArmorStand)
						&& !RegionProtection.isRegionProtected(this, entity.getLocation())
						&& !((entity instanceof Player targetPlayer)
						&& Commands.invincible.contains((targetPlayer).getName()))) {
					DamageHandler.damageEntity(entity, damage, this);
					GeneralMethods.setVelocity(this, entity, location.getDirection().multiply(knockback));
					travelled = range;
				}
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
		return "ElementSphereAir";
	}
	
	@Override
	public boolean isHiddenAbility() {
		return true;
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
		return null;
	}

	public double getDistanceTravelled() {
		return travelled;
	}

	public void setDistanceTravelled(double travelled) {
		this.travelled = travelled;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public double getKnockback() {
		return knockback;
	}

	public void setKnockback(double knockback) {
		this.knockback = knockback;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Avatar.ElementSphere.Enabled");
	}
}
