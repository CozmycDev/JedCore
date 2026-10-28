package com.jedk1.jedcore.ability.waterbending.passive;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.IceAbility;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class IcePassive {

	public static void handleSkating() {
		Map<World, SkateConfig> resultCache = new HashMap<>();

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			SkateConfig config = resultCache.get(player.getWorld());
			if (config == null) {
				ConfigurationSection section = JedCoreConfig.getConfig(player);

				boolean enabled = section.getBoolean("Abilities.Water.Ice.Passive.Skate.Enabled");
				int speedFactor = section.getInt("Abilities.Water.Ice.Passive.Skate.SpeedFactor");
				int duration = section.getInt("Abilities.Water.Ice.Passive.Skate.LeaveIceDuration", 60);

				config = new SkateConfig(enabled, speedFactor, duration);
				resultCache.put(player.getWorld(), config);
			}

			if (!config.enabled) continue;
			if (JCMethods.isDisabledWorld(player.getWorld())) continue;
			if (!player.isOnGround()) continue;
			if (!player.isSprinting()) continue;
			if (!IceAbility.isIce(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) continue;

			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

			if (bPlayer == null) continue;
			if (!bPlayer.canIcebend()) continue;
			if (!bPlayer.isPassiveToggled(Element.WATER)) continue;

			if (!player.hasPermission("bending.ability.IceSkate")) continue;

			player.getWorld().spawnParticle(Particle.POOF, player.getLocation().clone().add(0, 0.2, 0), 15, Math.random() / 2, Math.random() / 2, Math.random() / 2, 0);
			player.removePotionEffect(PotionEffectType.SPEED);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, config.duration, config.speedFactor));
		}
	}

	private static class SkateConfig {
		final boolean enabled;
		final int speedFactor;
		final int duration;

		SkateConfig(boolean enabled, int speedFactor, int duration) {
			this.enabled = enabled;
			this.speedFactor = speedFactor;
			this.duration = duration;
		}
	}
}
