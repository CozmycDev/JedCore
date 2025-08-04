package com.jedk1.jedcore.util.versionadapter;

import com.projectkorra.projectkorra.GeneralMethods;
import org.bukkit.Bukkit;

public class PotionEffectAdapterFactory {

    private PotionEffectAdapter adapter;

    public PotionEffectAdapterFactory() {
        int serverVersion = GeneralMethods.getMCVersion();

        if (serverVersion >= 1205) {
            Bukkit.getLogger().info("[JedCore] Using 1.20.5+ PotionEffectAdapter");
            adapter = new PotionEffectAdapter_1_20_5();
        } else {
            Bukkit.getLogger().info("[JedCore] Using 1.20.4- PotionEffectAdapter");
            adapter = new PotionEffectAdapter_1_20_4();
        }
    }

    public PotionEffectAdapter getAdapter() {
        return adapter;
    }
}
