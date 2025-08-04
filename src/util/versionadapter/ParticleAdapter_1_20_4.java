package com.jedk1.jedcore.util.versionadapter;

import org.bukkit.*;

public class ParticleAdapter_1_20_4 implements ParticleAdapter {

    @Override
    public void displayColoredParticles(String hex, Location location, int amount, double offsetX, double offsetY, double offsetZ, double extra, int alpha) {
        if (location.getWorld() == null) return;
        int[] color = hexToRgb(hex);
        if (alpha < 255) {
            location.getWorld().spawnParticle(Particle.valueOf("SPELL_MOB_AMBIENT"), location, 0, color[0] / 255D, color[1] / 255D, color[2] / 255D, 1);
        } else {
            location.getWorld().spawnParticle(Particle.valueOf("SPELL_MOB"), location, 0, color[0] / 255D, color[1] / 255D, color[2] / 255D, 1);
        }
    }

    @Override
    public void displayMagneticParticles(Location location) {
        location.getWorld().spawnParticle(Particle.valueOf("ASH"), location, 1, 0, 0, 0, 0.03);
    }

    private int[] hexToRgb(String hex) {
        return new int[] {
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)
        };
    }
}