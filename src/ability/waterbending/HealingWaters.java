package com.jedk1.jedcore.ability.waterbending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.LightManager;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.HealingAbility;
import com.projectkorra.projectkorra.chiblocking.Smokescreen;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import com.projectkorra.projectkorra.waterbending.util.WaterReturn;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class HealingWaters extends HealingAbility implements AddonAbility {

	private static long time = 0;
	private static boolean enabled = true;

	public HealingWaters(Player player) {
		super(player);
	}

	public static void heal(Server server) {
		if (enabled) {
			if (System.currentTimeMillis() - time >= 1000) {
				time = System.currentTimeMillis();
				for (Player player : server.getOnlinePlayers()) {
					BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
					if (bPlayer != null && bPlayer.canBend(getAbility("HealingWaters"))) {
						heal(player);
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static void heal(Player player) {
		if (inWater(player)) {
			if (player.isSneaking()) {
				Entity entity = GeneralMethods.getTargetedEntity(player, getRange(player), new ArrayList<>());
				if (entity instanceof LivingEntity && inWater(entity)) {
					Location playerLoc = entity.getLocation();
					playerLoc.add(0, 1, 0);
					JCMethods.displayColoredParticles("#9696E1", playerLoc, 3, Math.random(), Math.random(), Math.random(), 0f, 50);
					ParticleEffect.WATER_WAKE.display(playerLoc, 25, 0, 0, 0, 0.05F);
					giveHPToEntity((LivingEntity) entity);
					emitLight(playerLoc);
					emitLight(entity.getLocation());
				}
			} else {
				Location playerLoc = player.getLocation();
				playerLoc.add(0, 1, 0);
				JCMethods.displayColoredParticles("#9696E1", playerLoc, 3, Math.random(), Math.random(), Math.random(), 0f, 50);
				ParticleEffect.WATER_WAKE.display(playerLoc, 25, 0, 0, 0, 0.05F);
				giveHP(player);
				emitLight(playerLoc);
			}

		} else if(hasWaterSupply(player) && player.isSneaking()) {
			Entity entity = GeneralMethods.getTargetedEntity(player, getRange(player), new ArrayList<>());
			if (entity != null) {
				if (entity instanceof LivingEntity) {
					Damageable dLe = (Damageable) entity;
					if (dLe.getHealth() < dLe.getMaxHealth()) {
						Location playerLoc = entity.getLocation();
						playerLoc.add(0, 1, 0);
						JCMethods.displayColoredParticles("#9696E1", playerLoc, 3, Math.random(), Math.random(), Math.random(), 0f, 50);
						ParticleEffect.WATER_WAKE.display(playerLoc, 25, 0, 0, 0, 0.05F);
						giveHPToEntity((LivingEntity) entity);
						entity.setFireTicks(0);
						Random rand = new Random();
						if (rand.nextInt(getDrainChance(player)) == 0) drainWaterSupply(player);
						emitLight(playerLoc);
						emitLight(entity.getLocation());
					}
				}
			} else {
				Location playerLoc = player.getLocation();
				playerLoc.add(0, 1, 0);

				JCMethods.displayColoredParticles("#FFFFFF", playerLoc, 3, Math.random(), Math.random(), Math.random(), 0f, 50);
				JCMethods.displayColoredParticles("#FFFFFF", playerLoc, 3, Math.random(), Math.random(), Math.random(), 0f);

				ParticleEffect.WATER_WAKE.display(playerLoc, 25, 0, 0, 0, 0.05F);
				giveHP(player);
				player.setFireTicks(0);
				Random rand = new Random();
				if (rand.nextInt(getDrainChance(player)) == 0) drainWaterSupply(player);
				emitLight(playerLoc);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static void giveHPToEntity(LivingEntity le) {
		if (!le.isDead() && le.getHealth() < le.getMaxHealth()) {
			applyHealingToEntity(le);
		}
		for (PotionEffect effect : le.getActivePotionEffects()) {
			if (isNegativeEffect(effect.getType())) {
				le.removePotionEffect(effect.getType());
			}
		}
	}

	private static void giveHP(Player player){
		if (!player.isDead() && player.getHealth() < 20) {
			applyHealing(player);
		}
		for(PotionEffect effect : player.getActivePotionEffects()) {
			if(isNegativeEffect(effect.getType())) {
				if((effect.getType() == PotionEffectType.BLINDNESS) && Smokescreen.getBlindedTimes().containsKey(player.getName())) {
					return;
				}
				player.removePotionEffect(effect.getType());
			}
		}
	}



	private static boolean inWater(Entity entity) {
		Block block = entity.getLocation().getBlock();
		return isWater(block) && !TempBlock.isTempBlock(block);
	}

	private static boolean hasWaterSupply(Player player){
		ItemStack heldItem = player.getInventory().getItemInMainHand();
		return (heldItem.isSimilar(WaterReturn.waterBottleItem()) || heldItem.getType() == Material.WATER_BUCKET);

	}

	private static void drainWaterSupply(Player player){
		ItemStack heldItem = player.getInventory().getItemInMainHand();
		ItemStack emptyBottle = new ItemStack(Material.GLASS_BOTTLE, 1);
		if (heldItem.isSimilar(WaterReturn.waterBottleItem())) {
			if (heldItem.getAmount() > 1) {
				heldItem.setAmount(heldItem.getAmount() - 1);
				HashMap<Integer, ItemStack> cantFit = player.getInventory().addItem(emptyBottle);
				for (int id : cantFit.keySet()) {
					player.getWorld().dropItem(player.getEyeLocation(), cantFit.get(id));
				}
			} else {
				player.getInventory().setItemInMainHand(emptyBottle);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static void applyHealing(Player player) {
		if (!RegionProtection.isRegionProtected(player, player.getLocation(), "HealingWaters"))
			if(player.getHealth() < player.getMaxHealth()) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 70, getPower(player)));
				AirAbility.breakBreathbendingHold(player);
			}
	}

	@SuppressWarnings("deprecation")
	private static void applyHealingToEntity(LivingEntity le) {
		if (le.getHealth() < le.getMaxHealth()) {
			le.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 70, 1));
			AirAbility.breakBreathbendingHold(le);
		}
	}

	public static int getPower(Player player) {
		ConfigurationSection config = JedCoreConfig.getConfig(player);
		return config.getInt("Abilities.Water.HealingWaters.Power");
	}

	public static double getRange(Player player) {
		ConfigurationSection config = JedCoreConfig.getConfig(player);
		return config.getDouble("Abilities.Water.HealingWaters.Range");
	}

	public static int getDrainChance(Player player) {
		ConfigurationSection config = JedCoreConfig.getConfig(player);
		return config.getInt("Abilities.Water.HealingWaters.DrainChance");
	}

	public static void emitLight(Location loc) {
		ConfigurationSection config = JedCoreConfig.getConfig((Player)null);
		if (config.getBoolean("Abilities.Water.HealingWaters.DynamicLight.Enabled")) {
			int brightness = config.getInt("Abilities.Water.HealingWaters.DynamicLight.Brightness");
			long keepAlive = config.getLong("Abilities.Water.HealingWaters.DynamicLight.KeepAlive");

			LightManager.createLight(loc).brightness(brightness).timeUntilFadeout(keepAlive).emit();
		}
	}

	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "HealingWaters";
	}

	@Override
	public boolean isHarmlessAbility() {
		return true;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	@Override
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return "* JedCore Addon *\n" + config.getString("Abilities.Water.HealingWaters.Description");
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		enabled = config.getBoolean("Abilities.Water.HealingWaters.Enabled");
		return enabled;
	}

	@Override
	public void progress() {}
}