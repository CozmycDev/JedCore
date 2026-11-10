package com.jedk1.jedcore.util;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.ability.firebending.LightningStream;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LightningCollisionSystem {

	// ported from particles only, still needs work.
	// lets be real. lightning doesnt clip lightning.

	private static final List<ActiveLightning> activeLightningList = new ArrayList<>();
	private static final double COLLISION_DETECTION_RANGE = 3.0;
	private static final double COLLISION_DAMAGE_RADIUS = 2.5;
	private static final int COLLISION_DURATION_TICKS = 40;
	private static final int DAMAGE_COOLDOWN_TICKS = 10;

	public static class ActiveLightning {
		private final Player caster;
		private final LightningStream sourceAbility;
		private final List<Location> branchEndpoints;
		private boolean isInCollision;
		private ActiveLightning collidingWith;
		private Location collisionPoint;
		private boolean isRemoved;

		public ActiveLightning(Player caster, LightningStream sourceAbility) {
			this.caster = caster;
			this.sourceAbility = sourceAbility;
			this.branchEndpoints = new ArrayList<>();
			this.isInCollision = false;
			this.isRemoved = false;
		}

		public void updateBranchEndpoints(List<Location> endpoints) {
			this.branchEndpoints.clear();
			this.branchEndpoints.addAll(endpoints);
		}

		public Player getCaster() {
			return caster;
		}

		public List<Location> getBranchEndpoints() {
			return branchEndpoints;
		}

		public boolean isInCollision() {
			return isInCollision;
		}

		public Location getCollisionPoint() {
			return collisionPoint;
		}

		public ActiveLightning getCollidingWith() {
			return collidingWith;
		}

		public void markRemoved() {
			this.isRemoved = true;
		}

		public boolean isRemoved() {
			return isRemoved;
		}
	}

	public static ActiveLightning registerLightning(Player caster, LightningStream sourceAbility) {
		ActiveLightning lightning = new ActiveLightning(caster, sourceAbility);
		activeLightningList.add(lightning);
		return lightning;
	}

	public static void unregisterLightning(ActiveLightning lightning) {
		if (lightning == null) {
			return;
		}

		lightning.markRemoved();

		if (lightning.isInCollision && lightning.collidingWith != null) {
			lightning.collidingWith.isInCollision = false;
			lightning.collidingWith.collidingWith = null;
			lightning.collidingWith.collisionPoint = null;
		}
		activeLightningList.remove(lightning);
	}

	public static void checkCollisions(ActiveLightning lightning) {
		if (lightning == null || lightning.isRemoved()) {
			return;
		}

		if (lightning.isInCollision) {
			maintainCollision(lightning);
			return;
		}

		for (ActiveLightning other : activeLightningList) {
			if (other == lightning || other.caster.equals(lightning.caster)) {
				continue;
			}

			if (other.isRemoved() || other.isInCollision) {
				continue;
			}

			CollisionResult result = detectCollision(lightning, other);
			if (result != null) {
				initiateCollision(lightning, other, result);
				return;
			}
		}
	}

	private static CollisionResult detectCollision(ActiveLightning lightning1, ActiveLightning lightning2) {
		for (Location endpoint1 : lightning1.branchEndpoints) {
			for (Location endpoint2 : lightning2.branchEndpoints) {
				if (endpoint1.getWorld().equals(endpoint2.getWorld())) {
					double distance = endpoint1.distance(endpoint2);
					if (distance <= COLLISION_DETECTION_RANGE) {
						Location midpoint = endpoint1.clone().add(endpoint2.toVector()).multiply(0.5);
						return new CollisionResult(endpoint1, endpoint2, midpoint, distance);
					}
				}
			}
		}
		return null;
	}

	private static void initiateCollision(ActiveLightning lightning1, ActiveLightning lightning2, CollisionResult result) {
		lightning1.isInCollision = true;
		lightning1.collidingWith = lightning2;
		lightning1.collisionPoint = result.midpoint;

		lightning2.isInCollision = true;
		lightning2.collidingWith = lightning1;
		lightning2.collisionPoint = result.midpoint;

		spawnCollisionEffects(result.midpoint);
		startCollisionDamageZone(lightning1, lightning2, result.midpoint);
	}

	private static void maintainCollision(ActiveLightning lightning) {
		if (lightning.collidingWith == null || lightning.collisionPoint == null) {
			lightning.isInCollision = false;
			return;
		}

		if (lightning.collidingWith.isRemoved()) {
			lightning.isInCollision = false;
			lightning.collidingWith = null;
			lightning.collisionPoint = null;
			return;
		}

		boolean stillInRange = false;
		for (Location endpoint1 : lightning.branchEndpoints) {
			for (Location endpoint2 : lightning.collidingWith.branchEndpoints) {
				if (endpoint1.distance(endpoint2) <= COLLISION_DETECTION_RANGE * 1.5) {
					stillInRange = true;
					lightning.collisionPoint = endpoint1.clone().add(endpoint2.toVector()).multiply(0.5);
					lightning.collidingWith.collisionPoint = lightning.collisionPoint;
					break;
				}
			}
			if (stillInRange) {
				break;
			}
		}

		if (!stillInRange) {
			lightning.isInCollision = false;
			lightning.collidingWith.isInCollision = false;
			lightning.collidingWith.collidingWith = null;
			lightning.collidingWith.collisionPoint = null;
			lightning.collidingWith = null;
			lightning.collisionPoint = null;
		} else {
			spawnCollisionParticles(lightning.collisionPoint);
		}
	}

	private static void spawnCollisionEffects(Location location) {
		location.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.8f);
		location.getWorld().playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 2.0f);

		location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 30, 0.5, 0.5, 0.5, 0.1);
		location.getWorld().spawnParticle(Particle.END_ROD, location, 20, 0.3, 0.3, 0.3, 0.05);
	}

	private static void spawnCollisionParticles(Location location) {
		location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 3, 0.3, 0.3, 0.3, 0.02);
		location.getWorld().spawnParticle(Particle.END_ROD, location, 2, 0.2, 0.2, 0.2, 0.01);
	}

	private static void startCollisionDamageZone(ActiveLightning lightning1, ActiveLightning lightning2, Location center) {
		new BukkitRunnable() {
			private int ticks = 0;
			private final Set<Player> immunePlayers = new HashSet<>();
			private final Map<UUID, Integer> lastDamageTick = new HashMap<>();

			{
				immunePlayers.add(lightning1.caster);
				immunePlayers.add(lightning2.caster);
			}

			@Override
			public void run() {
				ticks++;

				if (lightning1.isRemoved() || lightning2.isRemoved()) {
					cancel();
					return;
				}

				if (!lightning1.isInCollision || !lightning2.isInCollision) {
					cancel();
					return;
				}

				if (ticks > COLLISION_DURATION_TICKS) {
					cancel();
					return;
				}

				Location currentCenter = lightning1.collisionPoint;
				if (currentCenter == null) {
					cancel();
					return;
				}

				for (Entity entity : currentCenter.getWorld().getNearbyEntities(currentCenter, COLLISION_DAMAGE_RADIUS, COLLISION_DAMAGE_RADIUS, COLLISION_DAMAGE_RADIUS, e -> e instanceof LivingEntity)) {
					LivingEntity living = (LivingEntity) entity;

					if (living instanceof Player && immunePlayers.contains(living)) {
						continue;
					}

					if (!living.isValid()) {
						continue;
					}

					UUID entityId = living.getUniqueId();
					if (lastDamageTick.containsKey(entityId)) {
						if (ticks - lastDamageTick.get(entityId) < DAMAGE_COOLDOWN_TICKS) {
							continue;
						}
					}

					double doubleDamage = lightning1.sourceAbility.getDamage() * 2.0;
					DamageHandler.damageEntity(living, doubleDamage, lightning1.sourceAbility);

					lastDamageTick.put(entityId, ticks);

					living.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
							living.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
					living.getWorld().playSound(living.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.5f, 1.5f);
				}

				if (ticks % 5 == 0) {
					spawnDamageZoneParticles(currentCenter);
				}
			}
		}.runTaskTimer(JedCore.plugin, 0L, 1L);
	}

	private static void spawnDamageZoneParticles(Location center) {
		int particleCount = 20;
		for (int i = 0; i < particleCount; i++) {
			double angle = (2 * Math.PI * i) / particleCount;
			double x = Math.cos(angle) * COLLISION_DAMAGE_RADIUS;
			double z = Math.sin(angle) * COLLISION_DAMAGE_RADIUS;
			Location particleLoc = center.clone().add(x, 0, z);
			center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0.5, 0, 0.01);
		}
	}

	public static void drawCollisionConnection(Location point1, Location point2) {
		Vector direction = point2.toVector().subtract(point1.toVector());
		double distance = direction.length();
		direction.normalize();

		for (double d = 0; d < distance; d += 0.3) {
			Location particleLoc = point1.clone().add(direction.clone().multiply(d));
			particleLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.05, 0.05, 0.05, 0);
		}
	}

	public static boolean isBlocked(ActiveLightning lightning, Player target) {
		if (lightning == null || !lightning.isInCollision) {
			return false;
		}

		if (lightning.collidingWith != null) {
			if (target.equals(lightning.collidingWith.caster)) {
				return true;
			}
			if (target.equals(lightning.caster)) {
				return true;
			}
		}

		return false;
	}

	private static class CollisionResult {
		final Location endpoint1;
		final Location endpoint2;
		final Location midpoint;
		final double distance;

		CollisionResult(Location endpoint1, Location endpoint2, Location midpoint, double distance) {
			this.endpoint1 = endpoint1;
			this.endpoint2 = endpoint2;
			this.midpoint = midpoint;
			this.distance = distance;
		}
	}

	public static boolean isNearLightning(Player player, double range) {
		double rangeSquared = range * range;
		for (ActiveLightning active : activeLightningList) {
			if (active.isRemoved() || active.getCaster().equals(player)) {
				continue;
			}

			for (Location endpoint : active.getBranchEndpoints()) {
				if (endpoint.getWorld() != null && endpoint.getWorld().equals(player.getWorld())
						&& endpoint.distanceSquared(player.getLocation()) <= rangeSquared) {
					return true;
				}
			}

			Location collisionPoint = active.getCollisionPoint();
			if (collisionPoint != null && collisionPoint.getWorld() != null
					&& collisionPoint.getWorld().equals(player.getWorld())
					&& collisionPoint.distanceSquared(player.getLocation()) <= rangeSquared) {
				return true;
			}
		}

		return false;
	}

	public static void clearAll() {
		activeLightningList.clear();
	}
}
