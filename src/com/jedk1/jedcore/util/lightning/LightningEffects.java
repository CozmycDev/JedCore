package com.jedk1.jedcore.util.lightning;

import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class LightningEffects {

	private static final double IMPACT_KEY_SCALE = 10.0;

	private LightningEffects() {}

	public static void spawnBlockImpactParticles(LightningArc arc) {
		spawnBlockImpactParticles(Collections.singletonList(arc));
	}

	public static void spawnBlockImpactParticles(Iterable<LightningArc> arcs) {
		Set<String> spawnedBlockImpacts = new HashSet<>();

		for (LightningArc arc : arcs) {
			for (Location impact : arc.getBlockHits()) {
				if (impact.getWorld() == null) {
					continue;
				}

				String key = impact.getWorld().getName()
						+ ":" + Math.round(impact.getX() * IMPACT_KEY_SCALE)
						+ ":" + Math.round(impact.getY() * IMPACT_KEY_SCALE)
						+ ":" + Math.round(impact.getZ() * IMPACT_KEY_SCALE);

				if (!spawnedBlockImpacts.add(key)) {
					continue;
				}

				impact.getWorld().spawnParticle(Particle.POOF, impact, 1, 0.0, 0.0, 0.0, 0.01);
				impact.getWorld().spawnParticle(Particle.SMOKE, impact, 3, 0.0, 0.0, 0.0, 0.01);
			}
		}
	}
}
