package com.jedk1.jedcore.util.versionadapter;

import org.bukkit.*;

public class ParticleAdapter_1_20_5 implements ParticleAdapter {

    @Override
    public void displayColoredParticles(String hex, Location location, int amount, double offsetX, double offsetY, double offsetZ, double extra, int alpha) {
        if (location.getWorld() == null) return;
        int[] color = hexToRgb(hex);
        location.getWorld().spawnParticle(Particle.valueOf("ENTITY_EFFECT"), location, amount, extra, offsetX, offsetY, offsetZ, Color.fromARGB(alpha, color[0], color[1], color[2]));
    }

    @Override
    public void displayMagneticParticles(Location location) {
        location.getWorld().spawnParticle(Particle.valueOf("MYCELIUM"), location, 1, 0, 0, 0, 0.01);
    }

    private int[] hexToRgb(String hex) {
        return new int[] {
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)
        };
    }
}
