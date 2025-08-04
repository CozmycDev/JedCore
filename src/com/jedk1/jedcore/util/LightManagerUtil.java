package com.jedk1.jedcore.util;

import com.jedk1.jedcore.configuration.JedCoreConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

public class LightManagerUtil {

    private static final boolean PROJECTKORRA_LIGHTMANAGER_AVAILABLE;
    
    static {
        boolean available = false;
        try {
            Class.forName("com.projectkorra.projectkorra.util.LightManager");
            available = true;
        } catch (ClassNotFoundException e) {
            // ProjectKorra LightManager not available, fallback to built-in LightManager
        }
        PROJECTKORRA_LIGHTMANAGER_AVAILABLE = available;
    }

    public static void emitFirebendingLight(final Location location) {
        if (!JedCoreConfig.getConfig((Player)null).getBoolean("Properties.Fire.DynamicLight.Enabled")) {
            return;
        }
        
        int brightness = JedCoreConfig.getConfig((Player)null).getInt("Properties.Fire.DynamicLight.Brightness");
        long keepAlive = JedCoreConfig.getConfig((Player)null).getLong("Properties.Fire.DynamicLight.KeepAlive");

        if (brightness < 1 || brightness > 15) {
            throw new IllegalArgumentException("Properties.Fire.DynamicLight.Brightness must be between 1 and 15.");
        }

        createLight(location).brightness(brightness).timeUntilFadeout(keepAlive).emit();
    }

    public static LightBuilder createLight(Location location) {
        return new LightBuilder(location);
    }

    public static boolean isProjectKorraLightManagerAvailable() {
        return PROJECTKORRA_LIGHTMANAGER_AVAILABLE;
    }

    public static class LightBuilder {
        private final Location location;
        private int brightness = 15; // default brightness
        private long timeUntilFade = 50; // default expiry time in ms
        private Collection<? extends Player> observers = null; // null means all players
        
        public LightBuilder(Location location) {
            this.location = location;
        }

        public LightBuilder brightness(int brightness) {
            this.brightness = Math.max(1, Math.min(15, brightness));
            return this;
        }

        public LightBuilder timeUntilFadeout(long expiry) {
            this.timeUntilFade = expiry;
            return this;
        }

        public LightBuilder observers(Collection<? extends Player> observers) {
            this.observers = observers;
            return this;
        }

        public void emit() {
            if (PROJECTKORRA_LIGHTMANAGER_AVAILABLE) {
                emitWithProjectKorraLightManager();
            } else {
                emitWithJedCoreLightManager();
            }
        }
        
        private void emitWithProjectKorraLightManager() {
            try {
                com.projectkorra.projectkorra.util.LightManager.LightBuilder lightBuilder = 
                    com.projectkorra.projectkorra.util.LightManager.createLight(location);
                
                lightBuilder = lightBuilder.brightness(brightness);
                lightBuilder = lightBuilder.timeUntilFadeout(timeUntilFade);
                
                if (observers != null) {
                    lightBuilder = lightBuilder.observers(observers);
                }
                
                lightBuilder.emit();
                
            } catch (Exception e) {
                // Fallback to JedCore's LightManager if ProjectKorra's fails
                emitWithJedCoreLightManager();
            }
        }
        
        private void emitWithJedCoreLightManager() {
            if (observers != null) {
                LightManager.get().createLight(location).brightness(brightness).timeUntilFadeout(timeUntilFade).observers(observers).emit();
            } else {
                LightManager.get().createLight(location).brightness(brightness).timeUntilFadeout(timeUntilFade).emit();
            }
        }
    }
}
