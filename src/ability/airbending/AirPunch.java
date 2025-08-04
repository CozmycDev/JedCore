package com.jedk1.jedcore.ability.airbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.collision.CollisionDetector;
import com.jedk1.jedcore.collision.Sphere;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AirPunch extends AirAbility implements AddonAbility {

	private final Map<Location, Double> locations = new ConcurrentHashMap<>();

	private int shots;
	private long lastShotTime;

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private long threshold;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.COOLDOWN)
	private double damage;
	@Attribute("CollisionRadius")
	private double entityCollisionRadius;

	public AirPunch(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		if (hasAbility(player, AirPunch.class)) {
			AirPunch ap = getAbility(player, AirPunch.class);
			ap.createShot();
			return;
		}
		
		setFields();

		start();

		if (!isRemoved()) createShot();
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Air.AirPunch.Cooldown");
		threshold = config.getLong("Abilities.Air.AirPunch.Threshold");
		shots = config.getInt("Abilities.Air.AirPunch.Shots");
		range = config.getDouble("Abilities.Air.AirPunch.Range");
		damage = config.getDouble("Abilities.Air.AirPunch.Damage");
		entityCollisionRadius = config.getDouble("Abilities.Air.AirPunch.EntityCollisionRadius");
	}

	@Override
	public void progress() {
		progressShots();

		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			prepareRemove();
			return;
		}

		if (shots == 0 || System.currentTimeMillis() > lastShotTime + threshold) {
			prepareRemove();
		}
	}

	private void prepareRemove() {
		if (player.isOnline() && !bPlayer.isOnCooldown(this)) {
			bPlayer.addCooldown(this);
		}

		if (locations.isEmpty()) {
			remove();
		}
	}

	private void createShot() {
		if (shots >= 1) {
			lastShotTime = System.currentTimeMillis();
			shots--;
			locations.put(player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5).normalize()), 0D);
		}
	}

	private void progressShots() {
		Iterator<Map.Entry<Location, Double>> iterator = locations.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Location, Double> entry = iterator.next();
			Location originalLoc = entry.getKey();
			double dist = entry.getValue();
			ShotResult result = simulateShotProgression(originalLoc, dist);

			iterator.remove();

			if (result.moved) {
				locations.put(result.newLoc, result.newDist);
			}
		}
	}

	private record ShotResult(Location newLoc, double newDist, boolean moved) {}

	private ShotResult simulateShotProgression(Location startLoc, double startDist) {
		Location loc = startLoc.clone();
		double dist = startDist;
		boolean shouldRemove = false;
		boolean moved = false;

		for (int i = 0; i < 3 && !shouldRemove; i++) {
			dist++;
			if (dist >= range) {
				shouldRemove = true;
			} else {
				Location nextLoc = calculateNextLocation(loc);
				if (isPathBlocked(nextLoc)) {
					shouldRemove = true;
				} else {
					applyShotEffects(nextLoc);
					if (checkAndHandleCollision(nextLoc)) {
						shouldRemove = true;
					} else {
						loc = nextLoc;
						moved = true;
					}
				}
			}
		}

		return new ShotResult(loc, dist, moved);
	}

	private Location calculateNextLocation(Location currentLocation) {
		return currentLocation.add(currentLocation.getDirection().clone().multiply(1));
	}

	private boolean isPathBlocked(Location location) {
		return GeneralMethods.isSolid(location.getBlock()) || isWater(location.getBlock()) || RegionProtection.isRegionProtected(player, location, this);
	}

	private void applyShotEffects(Location location) {
		getAirbendingParticles().display(location, 2, Math.random() / 5, Math.random() / 5, Math.random() / 5, 0.0);
		playAirbendingSound(location);
	}

	private boolean checkAndHandleCollision(Location location) {
		return CollisionDetector.checkEntityCollisions(player, new Sphere(location.toVector(), entityCollisionRadius), entity -> {
			DamageHandler.damageEntity(entity, damage, this);
			return true;
		});
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public double getCollisionRadius() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getDouble("Abilities.Air.AirPunch.AbilityCollisionRadius");
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.isRemovingFirst()) {
			Location location = collision.getLocationFirst();

			locations.remove(location);
		}
	}

	@Override
	public List<Location> getLocations() {
		return new ArrayList<>(locations.keySet());
	}

	@Override
	public String getName() {
		return "AirPunch";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Air.AirPunch.Description");
	}

	public long getThreshold() {
		return threshold;
	}

	public void setThreshold(long threshold) {
		this.threshold = threshold;
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

	public double getEntityCollisionRadius() {
		return entityCollisionRadius;
	}

	public void setEntityCollisionRadius(double entityCollisionRadius) {
		this.entityCollisionRadius = entityCollisionRadius;
	}

	public int getShots() {
		return shots;
	}

	public void setShots(int shots) {
		this.shots = shots;
	}

	public long getLastShotTime() {
		return lastShotTime;
	}

	public void setLastShotTime(long lastShotTime) {
		this.lastShotTime = lastShotTime;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Air.AirPunch.Enabled");
	}
}
