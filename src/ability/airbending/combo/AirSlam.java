package com.jedk1.jedcore.ability.airbending.combo;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.ThrownEntityTracker;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.object.HorizontalVelocityTracker;
import com.projectkorra.projectkorra.region.RegionProtection;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class AirSlam extends AirAbility implements AddonAbility, ComboAbility {

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.KNOCKBACK)
	private double power;
	@Attribute(Attribute.RANGE)
	private int range;

	private LivingEntity target;

	public AirSlam(Player player) {
		super(player);
		
		if (!bPlayer.canBendIgnoreBinds(this)) {
			return;
		}
		
		setFields();

		Entity targetEntity = GeneralMethods.getTargetedEntity(player, range, new ArrayList<>());
		if (!(targetEntity instanceof LivingEntity)
				|| RegionProtection.isRegionProtected(this, targetEntity.getLocation())
				|| ((targetEntity instanceof Player) && Commands.invincible.contains(targetEntity.getName())))
			return;

		this.target = (LivingEntity) targetEntity;

		start();

		if (!isRemoved()) {
			bPlayer.addCooldown(this);
			GeneralMethods.setVelocity(this, target, new Vector(0, 2, 0));
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Air.AirCombo.AirSlam.Cooldown");
		power = config.getDouble("Abilities.Air.AirCombo.AirSlam.Power");
		range = config.getInt("Abilities.Air.AirCombo.AirSlam.Range");
	}

	@Override
	public void progress() {
		if (player == null || player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (System.currentTimeMillis() > getStartTime() + 50) {
			Vector dir = player.getLocation().getDirection();
			GeneralMethods.setVelocity(this, target, new Vector(dir.getX(), 0.05, dir.getZ()).multiply(power));
			new HorizontalVelocityTracker(target, player, 0L, this);
			new ThrownEntityTracker(this, target, player, 0L);
			target.setFallDistance(0);
		}

		if (System.currentTimeMillis() > getStartTime() + 400) {
			remove();
			return;
		}

		playAirbendingParticles(target.getLocation(), 10);
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return target != null ? target.getLocation() : null;
	}

	@Override
	public String getName() {
		return "AirSlam";
	}
	
	@Override
	public boolean isHiddenAbility() {
		return false;
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
	public Object createNewComboInstance(Player player) {
		return new AirSlam(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		return ComboUtil.generateCombinationFromList(this, JedCoreConfig.getConfig(player).getStringList("Abilities.Air.AirCombo.AirSlam.Combination"));
	}

	@Override
	public String getInstructions() {
		return JedCoreConfig.getConfig(player).getString("Abilities.Air.AirCombo.AirSlam.Instructions");
	}

	@Override
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return "* JedCore Addon *\n" + config.getString("Abilities.Air.AirCombo.AirSlam.Description");
	}
	
	@Override
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public LivingEntity getTarget() {
		return target;
	}

	public void setTarget(LivingEntity target) {
		this.target = target;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Air.AirCombo.AirSlam.Enabled");
	}
}
