package com.jedk1.jedcore.util;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;

public class MaterialUtil {

    public static boolean isSign(Material material) {
        return Tag.SIGNS.isTagged(material);
    }

    public static boolean isSign(Block block) {
        return isSign(block.getType());
    }

    public static boolean isTransparent(Block block) {
        if (block == null) return true;
        return block.isPassable();
    }
}
