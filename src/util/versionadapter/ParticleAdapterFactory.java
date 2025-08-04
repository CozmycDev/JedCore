package com.jedk1.jedcore.util.versionadapter;

import com.projectkorra.projectkorra.GeneralMethods;
import org.bukkit.Bukkit;

public class ParticleAdapterFactory {

    private ParticleAdapter adapter;

    public ParticleAdapterFactory() {
        int serverVersion = GeneralMethods.getMCVersion();

        if (serverVersion >= 1205) {
            Bukkit.getLogger().info("[JedCore] Using 1.20.5+ ParticleAdapter");
            adapter = new ParticleAdapter_1_20_5();
        } else {
            Bukkit.getLogger().info("[JedCore] Using 1.20.4- ParticleAdapter");
            adapter = new ParticleAdapter_1_20_4();
        }
    }

    public ParticleAdapter getAdapter() {
        return adapter;
    }
}