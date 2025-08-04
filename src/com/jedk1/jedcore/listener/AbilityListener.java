package com.jedk1.jedcore.listener;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.ability.airbending.*;
import com.jedk1.jedcore.ability.avatar.SpiritBeam;
import com.jedk1.jedcore.ability.avatar.elementsphere.ElementSphere;
import com.jedk1.jedcore.ability.chiblocking.DaggerThrow;
import com.jedk1.jedcore.ability.earthbending.*;
import com.jedk1.jedcore.ability.earthbending.combo.Crevice;
import com.jedk1.jedcore.ability.earthbending.combo.MagmaBlast;
import com.jedk1.jedcore.ability.firebending.*;
import com.jedk1.jedcore.ability.passive.WallRun;
import com.jedk1.jedcore.ability.waterbending.*;
import com.jedk1.jedcore.ability.waterbending.combo.WaterFlow;
import com.jedk1.jedcore.ability.waterbending.combo.WaterGimbal;
import com.jedk1.jedcore.util.SchedulerUtil;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.airbending.Suffocate;
import com.projectkorra.projectkorra.earthbending.EarthArmor;
import com.projectkorra.projectkorra.earthbending.lava.LavaFlow;
import com.projectkorra.projectkorra.firebending.FireJet;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.waterbending.blood.Bloodbending;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityListener implements Listener {

	JedCore plugin;

	public AbilityListener(JedCore plugin) {
		this.plugin = plugin;
	}

	private final List<UUID> recentlyDropped = new ArrayList<>();

	@EventHandler(priority = EventPriority.LOWEST)
	// Abilities that should bypass punch cancels should be handled here.
	public void onPlayerSwingBypassCancel(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_AIR) {
			return;
		}
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) {
			return;
		}

		if (Suffocate.isBreathbent(player) || bPlayer.isChiBlocked()) {
			return;
		}

		if (Bloodbending.isBloodbent(player) || MovementHandler.isStopped(player)) {
			return;
		}

		CoreAbility coreAbil = bPlayer.getBoundAbility();
		if (coreAbil == null) {
			return;
		}

		if (!bPlayer.canBendIgnoreCooldowns(coreAbil)) {
			return;
		}

		if (coreAbil instanceof FireAbility && bPlayer.isElementToggled(Element.FIRE)) {
			if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.Fire.CanBendWithWeapons")) {
				return;
			}

			// FireSki bypasses punch cancel because the normal version is activated from sneaking alone.
			// The punch activation exists just so people don't accidentally activate it, so they should have the same
			// requirements for activation.
			if (coreAbil instanceof FireJet && FireSki.isPunchActivated(player.getWorld())) {
				if (player.isSneaking()) {
					FireSki ski = new FireSki(player);
					if (ski.isStarted() && !bPlayer.isOnCooldown("FireJet")) {
						// The event only needs to be cancelled when FireSki is set to have no cooldown.
						// This is to prevent FireJet from activating from the same swing event.
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
		recentlyDropped.add(event.getPlayer().getUniqueId());

		// FirePunch item burn effect
		Player player = event.getPlayer();
		FirePunch fp = FirePunch.getAbility(player, FirePunch.class);
		boolean burnEnabled = com.jedk1.jedcore.configuration.JedCoreConfig.getConfig((Player)null).getBoolean("Abilities.Fire.FirePunch.BurnsDroppedItems", true);
		if (fp != null && burnEnabled) {
			var item = event.getItemDrop();
			var loc = fp.getRightHandPos();
			ParticleEffect.LAVA.display(loc, 5, 0.1, 0.1, 0.1, 0.05);
			ParticleEffect.FLAME.display(loc, 3, 0.1, 0.1, 0.1, 0.01);
			ParticleEffect.SMOKE_NORMAL.display(loc, 2, 0.05, 0.05, 0.05, 0.01);
			loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 1.4f);
			SchedulerUtil.ensureEntity(item, item::remove);
		}

		SchedulerUtil.runGlobalLater(() -> recentlyDropped.remove(event.getPlayer().getUniqueId()), 2L);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInventoryInteract(InventoryClickEvent event) {
		InventoryAction action = event.getAction();
		if (action == InventoryAction.DROP_ALL_CURSOR ||
				action == InventoryAction.DROP_ALL_SLOT ||
				action == InventoryAction.DROP_ONE_CURSOR ||
				action == InventoryAction.DROP_ONE_SLOT ||
				action == InventoryAction.UNKNOWN) {

			recentlyDropped.add(event.getWhoClicked().getUniqueId());

			SchedulerUtil.runGlobalLater(() -> {
				recentlyDropped.remove(event.getWhoClicked().getUniqueId());
			}, 2L);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerSwing(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (recentlyDropped.contains(player.getUniqueId())) return;

		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_AIR) {
			return;
		}
		if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.isCancelled()){
			return;
		}
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) {
			return;
		}

		if (Suffocate.isBreathbent(player)) {
			event.setCancelled(true);
			return;
		} else if (Bloodbending.isBloodbent(player) || MovementHandler.isStopped(player)) {
			event.setCancelled(true);
			return;
		} else if (bPlayer.isChiBlocked()) {
			event.setCancelled(true);
			return;
		} else if (GeneralMethods.isInteractable(player.getTargetBlock((Set<Material>)null, 5))) {
			return;
		}

		if (bPlayer.isToggled()) {
			new WallRun(player);
		}

		CoreAbility coreAbil = bPlayer.getBoundAbility();
		if (coreAbil == null) {
			if (MultiAbilityManager.hasMultiAbilityBound(player)){
				String abil = MultiAbilityManager.getBoundMultiAbility(player);
				if (abil.equalsIgnoreCase("elementsphere")) {
					new ElementSphere(player);
				}
			}
			return;
		}

		String abilName = bPlayer.getBoundAbilityName();
		Class<? extends CoreAbility> abilClass = coreAbil.getClass();

		if (bPlayer.canBendIgnoreCooldowns(coreAbil)) {

			if (coreAbil instanceof AirAbility && bPlayer.isElementToggled(Element.AIR)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.Air.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(AirBlade.class)) {
					new AirBlade(player);
				}
				if (abilClass.equals(AirPunch.class)) {
					new AirPunch(player);
				}
			}

			if (coreAbil instanceof EarthAbility && bPlayer.isElementToggled(Element.EARTH)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.Earth.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(EarthArmor.class)) {
					new MetalArmor(player);
				}
				if (abilClass.equals(EarthLine.class)) {
					EarthLine.shootLine(player);
				}
				if (abilClass.equals(EarthShard.class)) {
					EarthShard.throwShard(player);
				}
				if (abilClass.equals(EarthSurf.class)) {
					new EarthSurf(player);
				}
				if (abilClass.equals(Fissure.class)) {
					new Fissure(player);
				}
				if (abilClass.equals(LavaFlux.class)) {
					new LavaFlux(player);
				}
				if (abilClass.equals(LavaThrow.class)) {
					LavaThrow lt = CoreAbility.getAbility(player, LavaThrow.class);
					if (lt != null) {
						lt.createBlast();
					}
				}
				if (abilClass.equals(MetalFragments.class)) {
					MetalFragments.shootFragment(player);
				}
				if (abilClass.equals(MetalHook.class)) {
					new MetalHook(player);
				}
				if (abilClass.equals(MetalShred.class)) {
					MetalShred.extend(player);
				}
				if (abilClass.equals(MudSurge.class)) {
					MudSurge.mudSurge(player);
				}
				if (abilClass.equals(SandBlast.class)) {
					SandBlast.blastSand(player);
				}
				if (abilClass.equals(LavaFlow.class)) {
					MagmaBlast.performAction(player);
				}
				if (abilClass.equals(MagnetShield.class)) {
					new MagnetShield(player, true);
				}
			}

			if (coreAbil instanceof FireAbility && bPlayer.isElementToggled(Element.FIRE)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.Fire.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(Combustion.class)) {
					Combustion.combust(player);
				}
				if (abilClass.equals(Discharge.class)) {
					new Discharge(player);
				}
				if (abilClass.equals(FireBall.class)) {
					new FireBall(player);
				}
				if (abilClass.equals(FirePunch.class)) {
					new FirePunch(player);
				}
				if (abilClass.equals(FireShots.class)) {
					FireShots.fireShot(player);
				}
			}

			if (coreAbil instanceof WaterAbility && bPlayer.isElementToggled(Element.WATER)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.WATER.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(com.jedk1.jedcore.ability.waterbending.Bloodbending.class)) {
					com.jedk1.jedcore.ability.waterbending.Bloodbending.launch(player);
				}
				if (abilClass.equals(BloodPuppet.class)) {
					BloodPuppet.attack(player);
				}
				if (abilClass.equals(IceClaws.class)) {
					IceClaws.throwClaws(player);
				}
				if (abilName.equals("Drain")) {
					Drain.fireBlast(player);
				}
				if (coreAbil.getName().equalsIgnoreCase("watermanipulation")) {
					WaterGimbal.prepareBlast(player);
				}
				if (coreAbil.getName().equalsIgnoreCase("watermanipulation")) {
					WaterFlow.freeze(player);
				}
			}
			if (coreAbil instanceof ChiAbility && bPlayer.isElementToggled(Element.CHI)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.Chi.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(DaggerThrow.class)) {
					new DaggerThrow(player);
				}
			}

			if (coreAbil instanceof AvatarAbility) {
				if (abilClass.equals(ElementSphere.class)) {
					new ElementSphere(player);
				}
			}
		}
	}

	public static ConcurrentHashMap<UUID, Long> recent = new ConcurrentHashMap<UUID, Long>();

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		if (event.isCancelled() || bPlayer == null) {
			return;
		}

		String abilName = bPlayer.getBoundAbilityName();
		if (Suffocate.isBreathbent(player)) {
			if (!abilName.equalsIgnoreCase("AirSwipe")
					&& !abilName.equalsIgnoreCase("FireBlast")
					&& !abilName.equalsIgnoreCase("EarthBlast")
					&& !abilName.equalsIgnoreCase("WaterManipulation")) {
				if(!player.isSneaking()) {
					event.setCancelled(true);
				}
			}
		}

		if (MovementHandler.isStopped(player) || Bloodbending.isBloodbent(player)) {
			event.setCancelled(true);
			return;
		}

		CoreAbility coreAbil = bPlayer.getBoundAbility();
		if (coreAbil == null) {
			return;
		}

		Class<? extends CoreAbility> abilClass = coreAbil.getClass();

		if (bPlayer.isChiBlocked()) {
			event.setCancelled(true);
			return;
		}

		if (player.isSneaking() && bPlayer.canBendIgnoreCooldowns(coreAbil)) {
			if (coreAbil instanceof FireAbility && bPlayer.isElementToggled(Element.FIRE)) {
				if (abilClass.equals(FireShots.class)) {
					FireShots.swapHands(player);
				}

				if (abilClass.equals(FirePunch.class)) {
					FirePunch.swapHands(player);
				}
			}
		}

		if (!player.isSneaking() && bPlayer.canBendIgnoreCooldowns(coreAbil)) {
			if (coreAbil instanceof AirAbility && bPlayer.isElementToggled(Element.AIR)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !plugin.getConfig().getBoolean("Properties.Air.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(AirBreath.class)) {
					new AirBreath(player);
				}
				if (abilClass.equals(AirGlide.class)) {
					new AirGlide(player);
				}
				if (abilClass.equals(Meditate.class)) {
					new Meditate(player);
				}
				if (abilClass.equals(SonicBlast.class)) {
					new SonicBlast(player);
				}
			}

			if (coreAbil instanceof EarthAbility && bPlayer.isElementToggled(Element.EARTH)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.Earth.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(EarthKick.class)) {
					new EarthKick(player);
				}
				if (abilClass.equals(EarthLine.class)) {
					new EarthLine(player);
				}
				if (abilClass.equals(EarthPillar.class)) {
					new EarthPillar(player);
				}
				if (abilClass.equals(EarthShard.class)) {
					new EarthShard(player);
				}
				if (abilClass.equals(Fissure.class)) {
					Fissure.performAction(player);
				}
				if (abilClass.equals(LavaDisc.class)) {
					new LavaDisc(player);
				}
				if (abilClass.equals(MagnetShield.class)) {
					new MagnetShield(player, false);
				}
				if (abilClass.equals(MetalFragments.class)) {
					new MetalFragments(player);
				}
				if (abilClass.equals(MetalShred.class)) {
					new MetalShred(player);
				}
				if (abilClass.equals(MudSurge.class)) {
					new MudSurge(player);
				}
				if (abilClass.equals(SandBlast.class)) {
					new SandBlast(player);
				}
				if (abilClass.equals(Crevice.class)) {
					Crevice.closeCrevice(player);
				}
				if (abilClass.equals(LavaThrow.class)) {
					LavaThrow lt = CoreAbility.getAbility(player, LavaThrow.class);
					if (lt == null) {
						new LavaThrow(player);
					}
				}
			}

			if (coreAbil instanceof FireAbility && bPlayer.isElementToggled(Element.FIRE)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.Fire.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(Combustion.class)) {
					new Combustion(event.getPlayer());
				}
				if (abilClass.equals(FireBreath.class)) {
					new FireBreath(player);
				}
				if (abilClass.equals(FireComet.class)) {
					new FireComet(player);
				}
				if (abilClass.equals(FireJet.class)) {
					if (FireSki.isPunchActivated(player.getWorld())) {
						FireSki fs = CoreAbility.getAbility(player, FireSki.class);

						if (fs != null) {
							SchedulerUtil.ensureEntity(player, fs::remove);
						}
					} else {
						new FireSki(player);
					}
				}
				if (abilClass.equals(FireShots.class)) {
					new FireShots(player);
				}
				if (abilClass.equals(LightningBurst.class)) {
					new LightningBurst(player);
				}
			}

			if (coreAbil instanceof WaterAbility && bPlayer.isElementToggled(Element.WATER)) {
				if (GeneralMethods.isWeapon(player.getInventory().getItemInMainHand().getType()) && !ProjectKorra.plugin.getConfig().getBoolean("Properties.Water.CanBendWithWeapons")) {
					return;
				}
				if (abilClass.equals(com.jedk1.jedcore.ability.waterbending.Bloodbending.class)) {
					new com.jedk1.jedcore.ability.waterbending.Bloodbending(player);
				}
				if (abilClass.equals(BloodPuppet.class)) {
					new BloodPuppet(player);
				}
				if (abilClass.equals(FrostBreath.class)) {
					new FrostBreath(player);
				}
				if (abilClass.equals(IceClaws.class)) {
					new IceClaws(player);
					IceClaws.swapHands(player);
				}
				if (abilClass.equals(IceWall.class)) {
					new IceWall(player);
				}
				if (abilName.equals("Drain")) {
					new Drain(player);
				}
				if (abilClass.equals(WakeFishing.class)) {
					new WakeFishing(player);
				}
			}

			if (coreAbil instanceof AvatarAbility) {
				if (abilClass.equals(SpiritBeam.class)) {
					new SpiritBeam(player);
				}
			}
		}
	}

	@EventHandler
	public void onArrowHit(EntityDamageByEntityEvent event) {
		if (event.getDamager().getType() == EntityType.ARROW) {
			Arrow arrow = (Arrow) event.getDamager();
			if (arrow.getShooter() instanceof Player && arrow.hasMetadata("daggerthrow")) {
				Player player = (Player) arrow.getShooter();
				event.setDamage(0.0D);
				player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteraction(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			MetalFragments.shootFragment(event.getPlayer());
		}
	}

	@EventHandler
	public void onChange(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof FallingBlock) {
			if (event.getEntity().hasMetadata("magmablast")) event.setCancelled(true);
		}
	}
}
