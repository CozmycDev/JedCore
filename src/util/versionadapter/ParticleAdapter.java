package com.jedk1.jedcore.util.versionadapter;

import org.bukkit.Location;

public interface ParticleAdapter {
    void displayColoredParticles(String hex, Location location, int amount, double offsetX, double offsetY, double offsetZ, double extra, int alpha);
    void displayMagneticParticles(Location location);
}
