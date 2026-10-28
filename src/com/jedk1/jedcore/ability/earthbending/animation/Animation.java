package com.jedk1.jedcore.ability.earthbending.animation;

import org.bukkit.Location;

public interface Animation {

    void animate();

    void updateLocation(Location newLocation);

    default void remove() {}
}
