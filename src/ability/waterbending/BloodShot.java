package com.jedk1.jedcore.ability.waterbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Particle.DustTransition;

import java.util.List;

import static java.lang.Thread.sleep;

public class BloodShot extends BloodAbility implements AddonAbility {

    private static final String path = "ExtraAbilities.Hihelloy.BloodShot.";

    private long cooldown;
    private double damage;
    private int range;
    private double hitRadius;
    private double selfDamage;
    private double speed;

    private Location origin;
    private Vector direction;

    public BloodShot(Player player) {
        super(player);

        if (!bPlayer.canBend(this) || bPlayer.isOnCooldown(this)) return;

        setFieldsFromConfig();
        DamageHandler.damageEntity(player, selfDamage, this);
        this.origin = player.getEyeLocation().clone();
        this.direction = origin.getDirection().normalize();

        bPlayer.addCooldown(this);
        start();

        launchWave();
    }

    private void setFieldsFromConfig() {
        ConfigurationSection config = JedCoreConfig.getConfig(this.player);
        cooldown = config.getLong(path + "Cooldown");
        damage = config.getDouble(path + "Damage");
        range = config.getInt(path + "Range");
        hitRadius = config.getDouble(path + "Hitradius");
        selfDamage = config.getDouble(path + "SelfDamage");
        speed = config.getDouble(path + "Speed");
    }

    public void launchWave() {
        // Schedule task using Folia-safe method
        Bukkit.getGlobalRegionScheduler().run(JedCore.plugin, scheduledTask -> {
            Location current = origin.clone();
            DustTransition dust = new DustTransition(Color.RED, Color.RED, 1.2F);

            for (int i = 0; i < range; i++) {
                current.add(direction.clone().multiply(speed));

                // Check if ability is still active
                if (!this.bPlayer.canBend(this) || !player.isOnline() || player.isDead()) {
                    remove();
                    return;
                }

                Location particleLoc = current.clone();

                // Execute particle and hit detection on main thread
                Bukkit.getScheduler().runTask(JedCore.plugin, () -> {
                    World world = particleLoc.getWorld();
                    if (world == null) return;

                    world.spawnParticle(Particle.DUST_COLOR_TRANSITION, particleLoc, 10, 0.2, 0.2, 0.2, 0, dust);
                    world.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 2f);

                    List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(particleLoc, hitRadius);
                    for (Entity entity : entities) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            DamageHandler.damageEntity(entity, damage, this);
                            entity.setFireTicks(0);
                            remove();
                        }
                    }
                });

                // Delay between iterations (speed can be adjusted here)
                try {
                    sleep(10); // ~1 tick
                } catch (InterruptedException ignored) {}
            }

            // Clean up after loop ends
            Bukkit.getScheduler().runTask(JedCore.plugin, this::remove);
        });
    }

    // Required AddonAbility metadata
    @Override public String getName() { return "BloodShot"; }
    @Override public String getInstructions() { return "Left-click to fire a blood energy wave."; }
    @Override public String getDescription() {
        return "An ability that fires a wave of blood energy to damage opponents, but hurts you as well.";
    }
    @Override public boolean isEnabled() {
        ConfigurationSection config = JedCoreConfig.getConfig(this.player);
        return config.getBoolean(path + "Enabled");
    }
    @Override public long getCooldown() { return cooldown; }
    @Override public boolean isSneakAbility() { return false; }
    @Override public boolean isHarmlessAbility() { return false; }
    @Override public void progress() {} // Not used in instant abilities
    @Override public void load() {}
    @Override public void stop() {}
    @Override public String getAuthor() { return "Hihelloy"; }
    @Override public String getVersion() { return "1.0"; }
    @Override public Location getLocation() { return origin; }

    @Override
    public void remove() {
        super.remove();
    }
}
