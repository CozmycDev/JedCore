package com.jedk1.jedcore.util.lightning;

import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class LightningWaterElectrify {

	private static final double MIN_RANGE = 0.5;

	private LightningWaterElectrify() {}

	public static List<Hit> collectHits(Player player, Iterable<LightningArc> arcs, LightningStyle style, double range) {
		double effectiveRange = Math.max(MIN_RANGE, range);
		Location playerLocation = player.getLocation();
		List<LightningArc.Segment> segments = new ArrayList<>();
		double maxDistanceSquared = 0.0;

		for (LightningArc arc : arcs) {
			for (LightningArc.Segment segment : arc.getSegments()) {
				segments.add(segment);
				maxDistanceSquared = Math.max(maxDistanceSquared, segment.getFrom().distanceSquared(playerLocation));
				maxDistanceSquared = Math.max(maxDistanceSquared, segment.getTo().distanceSquared(playerLocation));
			}
		}

		if (segments.isEmpty()) {
			return Collections.emptyList();
		}

		double searchRadius = Math.sqrt(maxDistanceSquared) + effectiveRange;
		double rangeSquared = effectiveRange * effectiveRange;
		Set<UUID> seen = new HashSet<>();
		List<Hit> hits = new ArrayList<>();

		for (Entity entity : player.getWorld().getNearbyEntities(
				playerLocation,
				searchRadius,
				searchRadius,
				searchRadius,
				e -> e instanceof LivingEntity && !e.equals(player) && e.isInWater()
		)) {
			LivingEntity living = (LivingEntity) entity;

			if (!seen.add(living.getUniqueId())) {
				continue;
			}

			Location target = living.getLocation().add(0, 0.5, 0);
			Location origin = closestPointOnSegments(target, segments, rangeSquared);

			if (origin != null) {
				hits.add(new Hit(living, LightningArc.create(origin, target, style)));
			}
		}

		return hits;
	}

	public static void applyDamage(CoreAbility ability, LivingEntity target, double damage, double stunChance, double stunDuration) {
		DamageHandler.damageEntity(target, damage, ability);

		if (stunChance > 0.0 && stunDuration > 0.0 && ThreadLocalRandom.current().nextDouble() <= stunChance) {
			LightningElectrocution.apply(target, ability, stunDuration);
		}

		Location effectLocation = target.getLocation().add(0, 0.5, 0);
		target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, effectLocation, 12, 0.4, 0.4, 0.4, 0.1);
		target.getWorld().spawnParticle(Particle.BUBBLE_POP, effectLocation, 6, 0.3, 0.3, 0.3, 0.02);
	}

	private static Location closestPointOnSegments(Location point, List<LightningArc.Segment> segments, double rangeSquared) {
		Vector pointVector = point.toVector();
		Vector closest = null;
		double bestDistanceSquared = rangeSquared;

		for (LightningArc.Segment segment : segments) {
			Vector from = segment.getFrom().toVector();
			Vector direction = segment.getTo().toVector().subtract(from);
			double lengthSquared = direction.lengthSquared();
			double t = lengthSquared < 0.0001
					? 0.0
					: Math.max(0.0, Math.min(1.0, pointVector.clone().subtract(from).dot(direction) / lengthSquared));
			Vector candidate = from.clone().add(direction.multiply(t));
			double distanceSquared = pointVector.distanceSquared(candidate);

			if (distanceSquared <= bestDistanceSquared) {
				bestDistanceSquared = distanceSquared;
				closest = candidate;
			}
		}

		return closest == null ? null : closest.toLocation(point.getWorld());
	}

	public static final class Hit {
		private final LivingEntity target;
		private final LightningArc arc;

		private Hit(LivingEntity target, LightningArc arc) {
			this.target = target;
			this.arc = arc;
		}

		public LivingEntity getTarget() {
			return target;
		}

		public LightningArc getArc() {
			return arc;
		}
	}
}
