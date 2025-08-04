package com.jedk1.jedcore.ability.waterbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.ThrownEntityTracker;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.object.HorizontalVelocityTracker;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Bloodbending extends BloodAbility implements AddonAbility {

	private boolean nightOnly;
	private boolean fullMoonOnly;
	private boolean undeadMobs;
	private boolean bloodbendingThroughBlocks;
	private boolean requireBound;
	private int distance;
	@Attribute(Attribute.DURATION)
	private long holdTime;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	
	private long time;
	public LivingEntity victim;
	private BendingPlayer victimBPlayer;
	private boolean grabbed;
	
	public Bloodbending(Player player) {
		super(player);
		if (this.player == null || !isEligible(player, true)) {
			return;
		}
		setFields();
		time = System.currentTimeMillis() + holdTime;
		if (grab()) {
			start();
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		nightOnly = config.getBoolean("Abilities.Water.Bloodbending.NightOnly");
		fullMoonOnly = config.getBoolean("Abilities.Water.Bloodbending.FullMoonOnly");
		undeadMobs = config.getBoolean("Abilities.Water.Bloodbending.UndeadMobs");
		bloodbendingThroughBlocks = config.getBoolean("Abilities.Water.Bloodbending.IgnoreWalls");
		requireBound = config.getBoolean("Abilities.Water.Bloodbending.RequireBound");
		distance = config.getInt("Abilities.Water.Bloodbending.Distance");
		holdTime = config.getLong("Abilities.Water.Bloodbending.HoldTime");
		cooldown = config.getLong("Abilities.Water.Bloodbending.Cooldown");
	}

	public boolean isEligible(Player player, boolean hasAbility) {
		if (!bPlayer.canBend(this) || !bPlayer.canBloodbend() || (hasAbility && hasAbility(player, Bloodbending.class))) {
			return false;
		}
		if (nightOnly && !isNight(player.getWorld()) && !bPlayer.canBloodbendAtAnytime()) {
			return false;
		}
		return !fullMoonOnly || isFullMoon(player.getWorld()) || bPlayer.canBloodbendAtAnytime();
	}

	public static void launch(Player player) {
		if (hasAbility(player, Bloodbending.class)) {
			getAbility(player, Bloodbending.class).launch();
		}
	}

	private void launch() {
		if (Arrays.asList(ElementalAbility.getTransparentMaterials()).contains(player.getEyeLocation().getBlock().getType())) {
			Vector direction = GeneralMethods.getDirection(player.getEyeLocation(), GeneralMethods.getTargetedLocation(player, 20, ElementalAbility.getTransparentMaterials())).normalize().multiply(3);
			if (!victim.isDead()) {
				victim.setVelocity(direction);

				new HorizontalVelocityTracker(victim, player, 200L, this);
				new ThrownEntityTracker(this, victim, player, 200L);
			}
			remove();
		}
	}

	private boolean grab() {
		List<Entity> entities = new ArrayList<>();
		for (int i = 1; i < distance; i++) {
			Location location;
			if (bloodbendingThroughBlocks) {
				location = player.getTargetBlock(null, i).getLocation();
			} else {
				location = GeneralMethods.getTargetedLocation(player, i, ElementalAbility.getTransparentMaterials());
			}
			entities = GeneralMethods.getEntitiesAroundPoint(location, 1.7);
			entities.remove(player);

			if (!entities.isEmpty() && !entities.contains(player)) {
				break;
			}
		}
		if (entities.isEmpty()) {
			return false;
		}
		Entity e = entities.get(0);
		if (e == null) {
			return false;
		}
		if (!(e instanceof LivingEntity)) {
			return false;
		}
		if (e instanceof ArmorStand) {
			return false;
		}
		if (!undeadMobs	&& GeneralMethods.isUndead(e)) {
			return false;
		}
		if ((e instanceof Player) && !canBeBloodbent((Player) e)) {
			return false;
		}
		if (RegionProtection.isRegionProtected(player, e.getLocation(), this)) {
			return false;
		}
		for (Bloodbending bb : getAbilities(Bloodbending.class)) {
			if (bb.victim.getEntityId() == e.getEntityId()) {
				return false;
			}
		}

		victim = (LivingEntity) e;
		DamageHandler.damageEntity(victim, 0, this);
		HorizontalVelocityTracker.remove(victim);
		if (victim instanceof Creature) {
			((Creature) victim).setTarget(null);
		}
		if ((e instanceof Player) && BendingPlayer.getBendingPlayer((Player) e) != null) {
			victimBPlayer = BendingPlayer.getBendingPlayer((Player) e);
		}
		return true;
	}
	
	private boolean canBeBloodbent(Player player) {
		if (Commands.invincible.contains(player.getName())) {
			return false;
		}
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (requireBound) {
			if (bPlayer.getAbilities().containsValue("Bloodbending")) {
				return false;
			}
			return !bPlayer.getAbilities().containsValue("BloodPuppet");
		} else {
			if (bPlayer.canBind(getAbility("Bloodbending")) && bPlayer.canBloodbend()) {
				return isDay(player.getWorld()) && !bPlayer.canBloodbendAtAnytime();
			}
		}
		return true;
	}

	@Override
	public void progress() {
		if (!isEligible(player, false)) {
			remove();
			return;
		}
		if (!grabbed) {
			if (victim instanceof Player && victimBPlayer != null) {
				victimBPlayer.blockChi();
				grabbed = true;
			}
		}

		if (!player.isSneaking()) {
			remove();
			return;
		}
		if (!player.isOnline() || player.isDead()) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > time) {
			remove();
			return;
		}
		if (victim.isDead()) {
			remove();
			return;
		}
		if ((victim instanceof Player) && !((Player) victim).isOnline()) {
			remove();
			return;
		}
		Location oldLocation = victim.getLocation();
		Location loc = GeneralMethods.getTargetedLocation(player, (int) player.getLocation().distance(oldLocation));
		double distance = loc.distance(oldLocation);
		Vector v = GeneralMethods.getDirection(oldLocation, GeneralMethods.getTargetedLocation(player, 10));
		if (distance > 1.2D) {
			victim.setVelocity(v.normalize().multiply(0.8D));
		} else {
			victim.setVelocity(new Vector(0, 0, 0));
		}
		victim.setFallDistance(0.0F);
		if (victim instanceof Creature) {
			((Creature) victim).setTarget(null);
		}
		AirAbility.breakBreathbendingHold(victim);
	}

	@Override
	public void remove() {
		if (player.isOnline()) {
			bPlayer.addCooldown(this);
		}
		if (victim instanceof Player && victimBPlayer != null) {
			victimBPlayer.unblockChi();
		}
		super.remove();
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "Bloodbending";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Water.Bloodbending.Description");
	}

	public boolean isNightOnly() {
		return nightOnly;
	}

	public void setNightOnly(boolean nightOnly) {
		this.nightOnly = nightOnly;
	}

	public boolean isFullMoonOnly() {
		return fullMoonOnly;
	}

	public void setFullMoonOnly(boolean fullMoonOnly) {
		this.fullMoonOnly = fullMoonOnly;
	}

	public boolean isUndeadMobs() {
		return undeadMobs;
	}

	public void setUndeadMobs(boolean undeadMobs) {
		this.undeadMobs = undeadMobs;
	}

	public boolean isBloodbendingThroughBlocks() {
		return bloodbendingThroughBlocks;
	}

	public void setBloodbendingThroughBlocks(boolean bloodbendingThroughBlocks) {
		this.bloodbendingThroughBlocks = bloodbendingThroughBlocks;
	}

	public boolean isRequireBound() {
		return requireBound;
	}

	public void setRequireBound(boolean requireBound) {
		this.requireBound = requireBound;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public long getHoldTime() {
		return holdTime;
	}

	public void setHoldTime(long holdTime) {
		this.holdTime = holdTime;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public LivingEntity getVictim() {
		return victim;
	}

	public void setVictim(LivingEntity victim) {
		this.victim = victim;
	}

	public BendingPlayer getVictimBPlayer() {
		return victimBPlayer;
	}

	public void setVictimBPlayer(BendingPlayer victimBPlayer) {
		this.victimBPlayer = victimBPlayer;
	}

	public boolean isGrabbed() {
		return grabbed;
	}

	public void setGrabbed(boolean grabbed) {
		this.grabbed = grabbed;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.Bloodbending.Enabled");
	}
}
