package com.jedk1.jedcore.ability.waterbending;

import java.util.*;

import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.Vector;

import com.jedk1.jedcore.JedCore;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.DamageHandler;

public class BloodPuppet extends BloodAbility implements AddonAbility {

	private boolean nightOnly;
	private boolean fullMoonOnly;
	private boolean undeadMobs;
	private boolean bloodPuppetThroughBlocks;
	private boolean requireBound;
	private int distance;
	@Attribute(Attribute.DURATION)
	private long holdTime;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;

	private long endTime;

	public LivingEntity puppet;
	private long lastDamageTime = 0;

	Random rand = new Random();

	public BloodPuppet(Player player) {
		super(player);
		if (!isEligible(player, true)) {
			return;
		}

		setFields();
		endTime = System.currentTimeMillis() + holdTime;

		if (grab()) {
			start();
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		nightOnly = config.getBoolean("Abilities.Water.BloodPuppet.NightOnly");
		fullMoonOnly = config.getBoolean("Abilities.Water.BloodPuppet.FullMoonOnly");
		undeadMobs = config.getBoolean("Abilities.Water.BloodPuppet.UndeadMobs");
		bloodPuppetThroughBlocks = config.getBoolean("Abilities.Water.BloodPuppet.IgnoreWalls");
		requireBound = config.getBoolean("Abilities.Water.BloodPuppet.RequireBound");
		distance = config.getInt("Abilities.Water.BloodPuppet.Distance");
		holdTime = config.getLong("Abilities.Water.BloodPuppet.HoldTime");
		cooldown = config.getLong("Abilities.Water.BloodPuppet.Cooldown");
	}

	public boolean isEligible(Player player, boolean hasAbility) {
		if (!bPlayer.canBend(this) || !bPlayer.canBloodbend() || (hasAbility && hasAbility(player, BloodPuppet.class))) {
			return false;
		}
		if (nightOnly && !isNight(player.getWorld()) && !bPlayer.canBloodbendAtAnytime()) {
			return false;
		}
		return !fullMoonOnly || isFullMoon(player.getWorld()) || bPlayer.canBloodbendAtAnytime();
	}

	private boolean canAttack() {
		switch (puppet.getType()) {
		case SKELETON:
		case SPIDER:
		case GIANT:
		case ZOMBIE:
		case SLIME:
		case GHAST:
		case PIGLIN:
		case ZOMBIFIED_PIGLIN:
		case ENDERMAN:
		case CAVE_SPIDER:
		case SILVERFISH:
		case BLAZE:
		case MAGMA_CUBE:
		case WITCH:
		case ENDERMITE:
		case DROWNED:
		case PLAYER:
			return true;
		default:
			return false;
		}
	}

	private boolean grab() {
		List<Entity> entities = new ArrayList<>();
		for (int i = 1; i < distance; i++) {
			Location location;
			if (bloodPuppetThroughBlocks) {
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

		if (e == null)
			return false;

		if (!(e instanceof LivingEntity))
			return false;

		if (!undeadMobs && GeneralMethods.isUndead(e))
			return false;

		if ((e instanceof Player) && !canBeBloodbent((Player) e)) {
			return false;
		}
		if (RegionProtection.isRegionProtected(player, e.getLocation(), this)) {
			return false;
		}

		for (BloodPuppet bb : getAbilities(BloodPuppet.class)) {
			if (bb.puppet.getEntityId() == e.getEntityId()) {
				return false;
			}
		}

		puppet = (LivingEntity) e;
		DamageHandler.damageEntity(puppet, 0, this);
		if (puppet instanceof Creature)
			((Creature) puppet).setTarget(null);

		if (e instanceof Player && BendingPlayer.getBendingPlayer((Player) e) != null) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer((Player) e);
			bPlayer.blockChi();
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

	public static void attack(Player player) {
		if (hasAbility(player, BloodPuppet.class)) {
			getAbility(player, BloodPuppet.class).attack();
		}
	}

	private void attack() {
		if (!canAttack())
			return;

		long damageCd = 0;
		if (System.currentTimeMillis() > lastDamageTime + damageCd) {
			lastDamageTime = System.currentTimeMillis();

			if (puppet instanceof Skeleton) {
				Skeleton skelly = (Skeleton) puppet;
				List<Entity> nearby = GeneralMethods.getEntitiesAroundPoint(skelly.getLocation(), 5);
				nearby.remove(puppet);
				if (nearby.size() < 1)
					return;
				int randy = rand.nextInt(nearby.size());
				Entity target = nearby.get(randy);
				if (target instanceof LivingEntity) {
					LivingEntity e = (LivingEntity) target;
					Location loc = puppet.getLocation().getBlock().getRelative(GeneralMethods.getCardinalDirection(GeneralMethods.getDirection(puppet.getEyeLocation(), e.getEyeLocation()))).getLocation();
					Arrow a = puppet.getWorld().spawnArrow(loc, GeneralMethods.getDirection(puppet.getEyeLocation(), e.getEyeLocation()), 0.6f, 12);
					a.setShooter(puppet);
					if (e instanceof Creature)
						((Creature) e).setTarget(puppet);
				}
			}

			else if (puppet instanceof Creeper) {
				Creeper creep = (Creeper) puppet;
				creep.setPowered(true);
			}

			else if (puppet instanceof Ghast) {
				Ghast gaga = (Ghast) puppet;
				List<Entity> nearby = GeneralMethods.getEntitiesAroundPoint(gaga.getLocation(), 5);
				nearby.remove(puppet);
				if (nearby.size() < 1)
					return;
				int randy = rand.nextInt(nearby.size());
				Entity target = nearby.get(randy);
				if (target instanceof LivingEntity) {
					LivingEntity e = (LivingEntity) target;
					Location loc = puppet.getLocation().getBlock().getRelative(GeneralMethods.getCardinalDirection(GeneralMethods.getDirection(puppet.getEyeLocation(), e.getEyeLocation()))).getLocation();
					Fireball fb = puppet.getWorld().spawn(loc, Fireball.class);
					fb.setVelocity(GeneralMethods.getDirection(puppet.getEyeLocation(), e.getEyeLocation()).multiply(0.25));
					fb.setIsIncendiary(true);
					fb.setShooter(puppet);
					if (e instanceof Creature)
						((Creature) e).setTarget(puppet);
				}
			}

			else if (puppet instanceof Blaze) {
				Blaze balawalaze = (Blaze) puppet;
				List<Entity> nearby = GeneralMethods.getEntitiesAroundPoint(balawalaze.getLocation(), 5);
				nearby.remove(puppet);
				if (nearby.size() < 1)
					return;
				int randy = rand.nextInt(nearby.size());
				Entity target = nearby.get(randy);
				if (target instanceof LivingEntity) {
					LivingEntity e = (LivingEntity) target;
					Location loc = puppet.getLocation().getBlock().getRelative(GeneralMethods.getCardinalDirection(GeneralMethods.getDirection(puppet.getEyeLocation(), e.getEyeLocation()))).getLocation();
					Fireball fb = puppet.getWorld().spawn(loc, Fireball.class);
					fb.setVelocity(GeneralMethods.getDirection(puppet.getEyeLocation(), e.getEyeLocation()).multiply(0.5));
					fb.setShooter(puppet);
					if (e instanceof Creature)
						((Creature) e).setTarget(puppet);
				}
			}

			else if (puppet instanceof Witch) {
				Witch missmagus = (Witch) puppet;
				List<Entity> nearby = GeneralMethods.getEntitiesAroundPoint(missmagus.getLocation(), 5);
				nearby.remove(puppet);
				if (nearby.size() < 1)
					return;
				int randy = rand.nextInt(nearby.size());
				Entity target = nearby.get(randy);
				if (target instanceof LivingEntity) {
					LivingEntity e = (LivingEntity) target;
					ThrownPotion tp = missmagus.launchProjectile(ThrownPotion.class, GeneralMethods.getDirection(puppet.getEyeLocation(), e.getEyeLocation()));
					ItemStack potionItem = new ItemStack(Material.SPLASH_POTION, 1);
					PotionMeta potion = (PotionMeta) potionItem.getItemMeta();
					potion.setBasePotionType(JedCore.plugin.getPotionEffectAdapter().getHarmingPotionType());
					potionItem.setItemMeta(potion);
					tp.setItem(potionItem);
					tp.setVelocity(GeneralMethods.getDirection(puppet.getEyeLocation(), e.getEyeLocation()).multiply(0.125));
					tp.setShooter(puppet);
					if (e instanceof Creature)
						((Creature) e).setTarget(puppet);
				}
			}

			else {
				for (Entity e : GeneralMethods.getEntitiesAroundPoint(puppet.getLocation(), 2)) {
					if (e.getEntityId() == puppet.getEntityId())
						continue;

					if (e instanceof LivingEntity) {
						int damage = 2;
						if (puppet instanceof Player) {
							Player p = (Player) puppet;

							switch (p.getInventory().getItemInMainHand().getType()) {
								case WOODEN_SWORD:
								case GOLDEN_SWORD:
									damage = 5;
									break;
								case STONE_SWORD:
									damage = 6;
									break;
								case IRON_SWORD:
									damage = 7;
									break;
								case DIAMOND_SWORD:
									damage = 8;
									break;
								default:
									break;
							}
						}
						((LivingEntity) e).damage(damage, puppet);
						if (e instanceof Creature)
							((Creature) e).setTarget(puppet);
					}
				}
			}
		}
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline() || player.isDead()) {
			remove();
			return;
		}
		if (!isEligible(player, false)) {
			remove();
			return;
		}

		if (!player.isSneaking()) {
			remove();
			return;
		}

		if (System.currentTimeMillis() > endTime) {
			remove();
			return;
		}

		if ((puppet instanceof Player && !((Player) puppet).isOnline()) || puppet.isDead()) {
			remove();
			return;
		}

		Location newLocation = puppet.getLocation();

		Location location = GeneralMethods.getTargetedLocation(player, distance + 1);
		double distance = location.distance(newLocation);
		double dx, dy, dz;
		dx = location.getX() - newLocation.getX();
		dy = location.getY() - newLocation.getY();
		dz = location.getZ() - newLocation.getZ();
		Vector vector = new Vector(dx, dy, dz);
		if (distance > .5) {
			puppet.setVelocity(vector.normalize().multiply(.5));
		} else {
			puppet.setVelocity(new Vector(0, 0, 0));
		}
		puppet.setFallDistance(0);
		if (puppet instanceof Creature) {
			((Creature) puppet).setTarget(null);
		}
		AirAbility.breakBreathbendingHold(puppet);
	}

	@Override
	public void remove() {
		if (player.isOnline()) {
			bPlayer.addCooldown(this);
		}
		if (puppet instanceof Player && ((Player) puppet).isOnline()) {
			BendingPlayer.getBendingPlayer((Player) puppet).unblockChi();
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
		return "BloodPuppet";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Water.BloodPuppet.Description");
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

	public boolean canBloodPuppetThroughBlocks() {
		return bloodPuppetThroughBlocks;
	}

	public void setCanBloodPuppetThroughBlocks(boolean bloodPuppetThroughBlocks) {
		this.bloodPuppetThroughBlocks = bloodPuppetThroughBlocks;
	}

	public boolean requiresBound() {
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

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public LivingEntity getPuppet() {
		return puppet;
	}

	public void setPuppet(LivingEntity puppet) {
		this.puppet = puppet;
	}

	public long getLastDamageTime() {
		return lastDamageTime;
	}

	public void setLastDamageTime(long lastDamageTime) {
		this.lastDamageTime = lastDamageTime;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.BloodPuppet.Enabled");
	}
}
