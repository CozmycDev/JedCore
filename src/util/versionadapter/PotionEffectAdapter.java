package com.jedk1.jedcore.util.versionadapter;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

public interface PotionEffectAdapter {
    PotionType getHarmingPotionType();
    PotionEffect getSlownessEffect(int duration, int strength);
    PotionEffect getResistanceEffect(int duration, int strength);
    PotionEffect getNauseaEffect(int duration);
    void applyJumpBoost(Player player, int duration, int strength);
    boolean hasWaterPotion(Inventory inventory);
}
