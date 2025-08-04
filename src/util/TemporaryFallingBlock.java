package com.jedk1.jedcore.util;

import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

public class TemporaryFallingBlock extends TempFallingBlock {

    private final Location originalLocation;

    public TemporaryFallingBlock(Location location, BlockData data, Vector velocity, CoreAbility ability) {
        super(location, data, velocity, ability);
        originalLocation = location.clone();
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public boolean hasMoved() {
        Location currentLocation = getFallingBlock().getLocation();
        return currentLocation.getX() != originalLocation.getX() || currentLocation.getZ() != originalLocation.getZ();
    }

    public void resetToOriginalLocation() {
        getFallingBlock().teleport(originalLocation);
    }
}
