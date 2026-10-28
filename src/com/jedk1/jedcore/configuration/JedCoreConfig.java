package com.jedk1.jedcore.configuration;

import com.jedk1.jedcore.JedCore;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

public class JedCoreConfig {

	static JedCore plugin;
	
	public JedCoreConfig(JedCore plugin) {
		JedCoreConfig.plugin = plugin;
		loadConfigCore();
		addDeathMessages();
		setupElementSphereNames();
	}
	
	private void loadConfigCore() {
		FileConfiguration config;
		config = JedCore.plugin.getConfig();
		
		config.addDefault("Settings.Updater.Check", true);
		config.addDefault("Settings.Updater.Notify", true);
		config.addDefault("Properties.MobCollisions.Enabled", true);
		config.addDefault("Properties.AbilityCollisions.Enabled", true);
		config.addDefault("Properties.PerWorldConfig", true);
		config.addDefault("Properties.FireTickMethod", "larger");
		config.addDefault("Properties.LogDebug", false);

		config.addDefault("Properties.ChiRestrictor.Enabled", false);
		config.addDefault("Properties.ChiRestrictor.ResetCooldown", true);
		config.addDefault("Properties.ChiRestrictor.MeleeDistance", 7);
		config.addDefault("Properties.ChiRestrictor.Whitelist", new ArrayList<String>());
		
		config.addDefault("Abilities.Avatar.ElementSphere.Enabled", true);
		config.addDefault("Abilities.Avatar.ElementSphere.Description", "Create a floating sphere that lets you fly and attack with every element. "
				+ "Left-Click to start it. While active, hold Sneak (Default: Shift) to fly, and use hotbar slots 1-5 "
				+ "to choose Air, Earth, Fire, Water, or Stream. Left-Click to attack. "
				+ "Each element has limited uses, and Stream uses 10 of each. "
				+ "Sneak and double Left-Click to stop.");
		config.addDefault("Abilities.Avatar.ElementSphere.Cooldown", 180000);
		config.addDefault("Abilities.Avatar.ElementSphere.Duration", 60000);
		config.addDefault("Abilities.Avatar.ElementSphere.MaxControlledHeight", 40);
		config.addDefault("Abilities.Avatar.ElementSphere.FlySpeed", 1.5);
		config.addDefault("Abilities.Avatar.ElementSphere.Air.Cooldown", 500);
		config.addDefault("Abilities.Avatar.ElementSphere.Air.Range", 40);
		config.addDefault("Abilities.Avatar.ElementSphere.Air.Uses", 20);
		config.addDefault("Abilities.Avatar.ElementSphere.Air.Damage", 3.0);
		config.addDefault("Abilities.Avatar.ElementSphere.Air.Knockback", 2);
		config.addDefault("Abilities.Avatar.ElementSphere.Air.Speed", 3);
		config.addDefault("Abilities.Avatar.ElementSphere.Earth.Cooldown", 500);
		config.addDefault("Abilities.Avatar.ElementSphere.Earth.Uses", 20);
		config.addDefault("Abilities.Avatar.ElementSphere.Earth.Damage", 3.0);
		config.addDefault("Abilities.Avatar.ElementSphere.Earth.ImpactCraterSize", 3);
		config.addDefault("Abilities.Avatar.ElementSphere.Earth.ImpactRevert", 15000);
		config.addDefault("Abilities.Avatar.ElementSphere.Fire.Cooldown", 500);
		config.addDefault("Abilities.Avatar.ElementSphere.Fire.Range", 40);
		config.addDefault("Abilities.Avatar.ElementSphere.Fire.Uses", 20);
		config.addDefault("Abilities.Avatar.ElementSphere.Fire.Damage", 3.0);
		config.addDefault("Abilities.Avatar.ElementSphere.Fire.BurnDuration", 3000);
		config.addDefault("Abilities.Avatar.ElementSphere.Fire.Speed", 3);
		config.addDefault("Abilities.Avatar.ElementSphere.Fire.Controllable", false);
		config.addDefault("Abilities.Avatar.ElementSphere.Water.Cooldown", 500);
		config.addDefault("Abilities.Avatar.ElementSphere.Water.Range", 40);
		config.addDefault("Abilities.Avatar.ElementSphere.Water.Uses", 20);
		config.addDefault("Abilities.Avatar.ElementSphere.Water.Damage", 3.0);
		config.addDefault("Abilities.Avatar.ElementSphere.Water.Speed", 3);
		config.addDefault("Abilities.Avatar.ElementSphere.Stream.Cooldown", 500);
		config.addDefault("Abilities.Avatar.ElementSphere.Stream.Range", 40);
		config.addDefault("Abilities.Avatar.ElementSphere.Stream.Knockback", 2.0);
		config.addDefault("Abilities.Avatar.ElementSphere.Stream.Damage", 12.0);
		config.addDefault("Abilities.Avatar.ElementSphere.Stream.RequiredUses", 10);
		config.addDefault("Abilities.Avatar.ElementSphere.Stream.EndAbility", true);
		config.addDefault("Abilities.Avatar.ElementSphere.Stream.ImpactCraterSize", 3);
		config.addDefault("Abilities.Avatar.ElementSphere.Stream.ImpactRevert", 30000);
		
		config.addDefault("Abilities.Avatar.SpiritBeam.Enabled", true);
		config.addDefault("Abilities.Avatar.SpiritBeam.Description", "Fire a powerful spirit beam from your body. "
				+ "You must be in the AvatarState. Hold Sneak (Default: Shift) to use it. ");
		config.addDefault("Abilities.Avatar.SpiritBeam.Cooldown", 15000);
		config.addDefault("Abilities.Avatar.SpiritBeam.Duration", 1000);
		config.addDefault("Abilities.Avatar.SpiritBeam.Range", 40);
		config.addDefault("Abilities.Avatar.SpiritBeam.Damage", 10.0);
		config.addDefault("Abilities.Avatar.SpiritBeam.AvatarStateOnly", true);
		config.addDefault("Abilities.Avatar.SpiritBeam.BlockDamage.Enabled", true);
		config.addDefault("Abilities.Avatar.SpiritBeam.BlockDamage.Radius", 3);
		config.addDefault("Abilities.Avatar.SpiritBeam.BlockDamage.Regen", 20000);


		config.addDefault("Abilities.Air.AirBlade.Enabled", true);
		config.addDefault("Abilities.Air.AirBlade.Description", "Shoot a sharp blade of air that damages your target. "
				+ "Left-Click to use it.");
		config.addDefault("Abilities.Air.AirBlade.Cooldown", 3000);
		config.addDefault("Abilities.Air.AirBlade.Range", 30.0);
		config.addDefault("Abilities.Air.AirBlade.Damage", 4.0);
		config.addDefault("Abilities.Air.AirBlade.Speed", 1.0);
		config.addDefault("Abilities.Air.AirBlade.Knockback", 0.0);
		config.addDefault("Abilities.Air.AirBlade.EntityCollisionRadius", 1.0);
		config.addDefault("Abilities.Air.AirBlade.AbilityCollisionRadius", 1.0);
		config.addDefault("Abilities.Air.AirBlade.Collisions.FireBlast.Enabled", true);
		config.addDefault("Abilities.Air.AirBlade.Collisions.FireBlast.RemoveFirst", true);
		config.addDefault("Abilities.Air.AirBlade.Collisions.FireBlast.RemoveSecond", true);
		config.addDefault("Abilities.Air.AirBlade.Collisions.FireBlastCharged.Enabled", true);
		config.addDefault("Abilities.Air.AirBlade.Collisions.FireBlastCharged.RemoveFirst", true);
		config.addDefault("Abilities.Air.AirBlade.Collisions.FireBlastCharged.RemoveSecond", false);
		config.addDefault("Abilities.Air.AirBlade.BlockCutting.Enabled", true);
		config.addDefault("Abilities.Air.AirBlade.BlockCutting.Revert", true);
		config.addDefault("Abilities.Air.AirBlade.BlockCutting.RevertTime", 5000);
		config.addDefault("Abilities.Air.AirBlade.BlockCutting.Materials", Arrays.asList(
				"#small_flowers",
				"#flowers",
				"#crops",
				"#saplings",
				"BIG_DRIPLEAF_STEM",
				"BIG_DRIPLEAF",
				"CAVE_VINES",
				"CAVE_VINES_PLANT",
				"VINE",
				"FERN",
				"LARGE_FERN",
				"SHORT_GRASS",
				"TALL_GRASS",
				"DEAD_BUSH",
				"SEA_PICKLE",
				"CACTUS_FLOWER",
				"BERRY_BUSH",
				"RED_MUSHROOM",
				"BROWN_MUSHROOM",
				"PINK_PETALS",
				"TORCHFLOWER",
				"PITCHER_PLANT",
				"PITCHER_CROP",
				"PALE_HANGING_MOSS",
				"PALE_MOSS_CARPET",
				"EYEBLOSSOM",
				"LEAF_LITTER",
				"WILDFLOWERS",
				"FIREFLY_BUSH",
				"BUSH",
				"SHORT_DRY_GRASS",
				"TALL_DRY_GRASS",
				"OPEN_EYEBLOSSOM",
				"CLOSED_EYEBLOSSOM"
		));

		config.addDefault("Abilities.Air.AirBreath.Enabled", false); // ported to PK Core
		config.addDefault("Abilities.Air.AirBreath.Description", "Breathe a strong gust of wind that pushes enemies back and puts out fire. "
				+ "Hold Sneak (Default: Shift) to use it.");
		config.addDefault("Abilities.Air.AirBreath.Cooldown", 3000);
		config.addDefault("Abilities.Air.AirBreath.Duration", 3000);
		config.addDefault("Abilities.Air.AirBreath.Particles", 3);
		config.addDefault("Abilities.Air.AirBreath.AffectBlocks.Lava", true);
		config.addDefault("Abilities.Air.AirBreath.AffectBlocks.Fire", true);
		config.addDefault("Abilities.Air.AirBreath.ExtinguishEntities", true);
		config.addDefault("Abilities.Air.AirBreath.Damage.Enabled", false);
		config.addDefault("Abilities.Air.AirBreath.Damage.Player", 1.0);
		config.addDefault("Abilities.Air.AirBreath.Damage.Mob", 2.0);
		config.addDefault("Abilities.Air.AirBreath.Knockback", 0.8);
		config.addDefault("Abilities.Air.AirBreath.Range", 10);
		config.addDefault("Abilities.Air.AirBreath.LaunchPower", 1.0);
		config.addDefault("Abilities.Air.AirBreath.RegenTargetOxygen", true);
		config.addDefault("Abilities.Air.AirBreath.Avatar.Enabled", true);
		config.addDefault("Abilities.Air.AirBreath.Avatar.Range", 20);
		config.addDefault("Abilities.Air.AirBreath.Avatar.Knockback", 3.5);
		
		config.addDefault("Abilities.Air.AirGlide.Enabled", true);
		config.addDefault("Abilities.Air.AirGlide.Description", "Glide slowly and safely through the air. "
				+ "While falling, tap Sneak (Default: Shift). Tap Sneak again to stop gliding.");
		config.addDefault("Abilities.Air.AirGlide.Speed", 0.5);
		config.addDefault("Abilities.Air.AirGlide.FallSpeed", 0.1);
		config.addDefault("Abilities.Air.AirGlide.Particles", 4);
		config.addDefault("Abilities.Air.AirGlide.AllowAirSpout", false);
		config.addDefault("Abilities.Air.AirGlide.Cooldown", 0);
		config.addDefault("Abilities.Air.AirGlide.Duration", 0);
		config.addDefault("Abilities.Air.AirGlide.RequireGround", false);
		
		config.addDefault("Abilities.Air.AirPunch.Enabled", true);
		config.addDefault("Abilities.Air.AirPunch.Description", "Throw quick punches of compressed air that deal light damage. "
				+ "Left-Click rapidly to throw several punches before the cooldown starts.");
		config.addDefault("Abilities.Air.AirPunch.Cooldown", 5000);
		config.addDefault("Abilities.Air.AirPunch.Threshold", 500);
		config.addDefault("Abilities.Air.AirPunch.Shots", 4);
		config.addDefault("Abilities.Air.AirPunch.Range", 30);
		config.addDefault("Abilities.Air.AirPunch.Damage", 1.0);
		config.addDefault("Abilities.Air.AirPunch.Speed", 1.0);
		config.addDefault("Abilities.Air.AirPunch.EntityCollisionRadius", 1.0);
		config.addDefault("Abilities.Air.AirPunch.AbilityCollisionRadius", 1.0);
		config.addDefault("Abilities.Air.AirPunch.Collisions.FireBlast.Enabled", true);
		config.addDefault("Abilities.Air.AirPunch.Collisions.FireBlast.RemoveFirst", true);
		config.addDefault("Abilities.Air.AirPunch.Collisions.FireBlast.RemoveSecond", false);
		config.addDefault("Abilities.Air.AirPunch.Collisions.FireBlastCharged.Enabled",true);
		config.addDefault("Abilities.Air.AirPunch.Collisions.FireBlastCharged.RemoveFirst",true);
		config.addDefault("Abilities.Air.AirPunch.Collisions.FireBlastCharged.RemoveSecond",false);
		config.addDefault("Abilities.Air.AirPunch.Collisions.AirBlade.Enabled", true);
		config.addDefault("Abilities.Air.AirPunch.Collisions.AirBlade.RemoveFirst", true);
		config.addDefault("Abilities.Air.AirPunch.Collisions.AirBlade.RemoveSecond", false);
		
		config.addDefault("Abilities.Air.Meditate.Enabled", true);
		config.addDefault("Abilities.Air.Meditate.Description", "Meditate to gain speed, higher jumps, and extra health. "
				+ "Hold Sneak (Default: Shift) until you finish focusing, then release to gain the buffs. "
				+ "Taking damage breaks your focus.");
		config.addDefault("Abilities.Air.Meditate.UnfocusMessage", "You have become unfocused due to taking damage!");
		config.addDefault("Abilities.Air.Meditate.LossFocusMessage", true);
		config.addDefault("Abilities.Air.Meditate.ChargeTime", 5000);
		config.addDefault("Abilities.Air.Meditate.Cooldown", 60000);
		config.addDefault("Abilities.Air.Meditate.BoostDuration", 20000);
		config.addDefault("Abilities.Air.Meditate.ParticleDensity", 5);
		config.addDefault("Abilities.Air.Meditate.AbsorptionBoost", 2);
		config.addDefault("Abilities.Air.Meditate.SpeedBoost", 3);
		config.addDefault("Abilities.Air.Meditate.JumpBoost", 3);
		
		config.addDefault("Abilities.Air.SonicBlast.Enabled", true);
		config.addDefault("Abilities.Air.SonicBlast.Description", "Stun and disorient your target with a blast of sound. "
				+ "Hold Sneak (Default: Shift) while aiming at them. When particles appear, release Sneak to fire.");
		config.addDefault("Abilities.Air.SonicBlast.ChargeTime", 2000);
		config.addDefault("Abilities.Air.SonicBlast.Damage", 4.0);
		config.addDefault("Abilities.Air.SonicBlast.Effects.BlindnessDuration", 5000);
		config.addDefault("Abilities.Air.SonicBlast.Effects.NauseaDuration", 5000);
		config.addDefault("Abilities.Air.SonicBlast.Cooldown", 6000);
		config.addDefault("Abilities.Air.SonicBlast.EntityCollisionRadius", 1.3);
		config.addDefault("Abilities.Air.SonicBlast.AbilityCollisionRadius", 1.3);
		config.addDefault("Abilities.Air.SonicBlast.Range", 20);
		config.addDefault("Abilities.Air.SonicBlast.ChargeSwapping", true);
		config.addDefault("Abilities.Air.SonicBlast.Sound.Name", "ENTITY_GENERIC_EXPLODE");
		config.addDefault("Abilities.Air.SonicBlast.Sound.Volume", 1.0);
		config.addDefault("Abilities.Air.SonicBlast.Sound.Pitch", 0.0);
		
		config.addDefault("Abilities.Air.AirCombo.AirSlam.Enabled", true);
		config.addDefault("Abilities.Air.AirCombo.AirSlam.Description", "Launch your target into the air, then blast them away. "
				+ "This is an Air combo.");
		config.addDefault("Abilities.Air.AirCombo.AirSlam.Cooldown", 8000);
		config.addDefault("Abilities.Air.AirCombo.AirSlam.Power", 5.0);
		config.addDefault("Abilities.Air.AirCombo.AirSlam.Range", 8);
		config.addDefault("Abilities.Air.AirCombo.AirSlam.Damage", 0.0);
		config.addDefault("Abilities.Air.AirCombo.AirSlam.Combination", Arrays.asList("AirSwipe:SHIFT_DOWN", "AirBlast:SHIFT_UP", "AirBlast:SHIFT_DOWN"));
		config.addDefault("Abilities.Air.AirCombo.AirSlam.Instructions", "AirSwipe (Hold sneak) > AirBlast (Release sneak) > AirBlast (Hold sneak)");
		
		config.addDefault("Abilities.Air.AirCombo.SwiftStream.Enabled", true);
		config.addDefault("Abilities.Air.AirCombo.SwiftStream.Description", "Fly forward and drag nearby enemies along with you. "
				+ "This is an Air combo.");
		config.addDefault("Abilities.Air.AirCombo.SwiftStream.DragFactor", 1.5);
		config.addDefault("Abilities.Air.AirCombo.SwiftStream.Duration", 2000);
		config.addDefault("Abilities.Air.AirCombo.SwiftStream.Cooldown", 6000);
		config.addDefault("Abilities.Air.AirCombo.SwiftStream.Combination", Arrays.asList("Flight:SHIFT_DOWN", "Flight:SHIFT_UP", "Flight:SHIFT_DOWN", "Flight:SHIFT_UP"));
		config.addDefault("Abilities.Air.AirCombo.SwiftStream.Instructions", "Flight (Double tap sneak)");


		config.addDefault("Abilities.Earth.EarthArmor.Enabled", true);
		config.addDefault("Abilities.Earth.EarthArmor.Description", "When you raise EarthArmor from a metal block, "
				+ "it becomes metal armor.");
		config.addDefault("Abilities.Earth.EarthArmor.Resistance.Strength", 2);
		config.addDefault("Abilities.Earth.EarthArmor.Resistance.Duration", 4000);
		config.addDefault("Abilities.Earth.EarthArmor.UseIronArmor", false);

		config.addDefault("Abilities.Earth.EarthBlast.Enabled", false);
		config.addDefault("Abilities.Earth.EarthBlast.Description", "Launch a hovering block of earth at your target with a smooth, animated throw. "
				+ "Sneak (Default: Shift) while looking at an earthbendable block to select it, then Left-Click to launch. "
				+ "While it is flying, Left-Click to redirect it or Sneak and Left-Click to shatter it.");
		config.addDefault("Abilities.Earth.EarthBlast.SelectRange", 6.0);
		config.addDefault("Abilities.Earth.EarthBlast.LaunchRange", 32.0);
		config.addDefault("Abilities.Earth.EarthBlast.RaiseTicks", 5);
		config.addDefault("Abilities.Earth.EarthBlast.Speed", 1.45);
		config.addDefault("Abilities.Earth.EarthBlast.DirectDamage", 3.0);
		config.addDefault("Abilities.Earth.EarthBlast.FragmentDamage", 2.0);
		config.addDefault("Abilities.Earth.EarthBlast.Knockback", 1.3);
		config.addDefault("Abilities.Earth.EarthBlast.UseModernHighlight", false);
		config.addDefault("Abilities.Earth.EarthBlast.HighlightColor", "#FFFFFF");
		config.addDefault("Abilities.Earth.EarthBlast.CollisionRadius", 1.0);
		config.addDefault("Abilities.Earth.EarthBlast.Cooldown", 500);
		config.addDefault("Abilities.Earth.EarthBlast.RevertTime", 15000);
		config.addDefault("Abilities.Earth.EarthBlast.AllowFragments", true);
		config.addDefault("Abilities.Earth.EarthBlast.FragmentCount", 6);
		config.addDefault("Abilities.Earth.EarthBlast.FragmentSpeed", 0.3);
		config.addDefault("Abilities.Earth.EarthBlast.MaxHurls", 6);
		config.addDefault("Abilities.Earth.EarthBlast.TickDuration", 300);
		config.addDefault("Abilities.Earth.EarthBlast.CollisionParticles", false);
		config.addDefault("Abilities.Earth.EarthBlast.FragmentParticles", true);
		config.addDefault("Abilities.Earth.EarthBlast.UseRedirectEasing", true);
		config.addDefault("Abilities.Earth.EarthBlast.RedirectEasingFactor", 0.15);
		config.addDefault("Abilities.Earth.EarthBlast.AnimationStyle", "wobble"); // wobble or tumble

		config.addDefault("Abilities.Earth.EarthKick.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Description", "Kick a spray of earth blocks at your foe. "
				+ "Tap Sneak (Default: Shift) while looking at an earthbendable block.");
		config.addDefault("Abilities.Earth.EarthKick.Cooldown", 2000);
		config.addDefault("Abilities.Earth.EarthKick.EarthBlocks", 10);

		double oldDamage = 0.0;
		if (config.contains("Abilities.Earth.EarthKick.Damage")) {
			oldDamage = config.getDouble("Abilities.Earth.EarthKick.Damage");
			config.set("Abilities.Earth.EarthKick.Damage", null);
		}

		config.addDefault("Abilities.Earth.EarthKick.Damage.Normal", oldDamage > 0.0 ? oldDamage : 2.0);
		config.addDefault("Abilities.Earth.EarthKick.Damage.Metal", 3.0);
		config.addDefault("Abilities.Earth.EarthKick.EntityCollisionRadius", 1.5);
		config.addDefault("Abilities.Earth.EarthKick.AbilityCollisionRadius", 1.5);
		config.addDefault("Abilities.Earth.EarthKick.MultipleHits", true);
		config.addDefault("Abilities.Earth.EarthKick.SourceRange", 2.0);
		config.addDefault("Abilities.Earth.EarthKick.Spread", 20);
		config.addDefault("Abilities.Earth.EarthKick.Velocity", 0.7);
		config.addDefault("Abilities.Earth.EarthKick.AllowMetal", true);
		config.addDefault("Abilities.Earth.EarthKick.ReplaceSource", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.FireBlast.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.FireBlast.RemoveFirst", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.FireBlast.RemoveSecond", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.EarthBlast.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.EarthBlast.RemoveFirst", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.EarthBlast.RemoveSecond", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.WaterManipulation.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.WaterManipulation.RemoveFirst", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.WaterManipulation.RemoveSecond", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirSwipe.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirSwipe.RemoveFirst", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirSwipe.RemoveSecond", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.Combustion.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.Combustion.RemoveFirst", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.Combustion.RemoveSecond", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.WaterSpout.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.WaterSpout.RemoveFirst", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.WaterSpout.RemoveSecond", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirSpout.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirSpout.RemoveFirst", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirSpout.RemoveSecond", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirWheel.Enabled", true);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirWheel.RemoveFirst", false);
		config.addDefault("Abilities.Earth.EarthKick.Collisions.AirWheel.RemoveSecond", true);

		config.addDefault("Abilities.Earth.EarthLine.Enabled", true);
		config.addDefault("Abilities.Earth.EarthLine.Description", "Send a line of earth at your target. "
				+ "Look at an earthbendable block on the ground and tap Sneak (Default: Shift) to select it, "
				+ "then Left-Click to fire. Hold Sneak to steer the line.");
		config.addDefault("Abilities.Earth.EarthLine.Cooldown", 3000);
		config.addDefault("Abilities.Earth.EarthLine.PrepareCooldown", 0);
		config.addDefault("Abilities.Earth.EarthLine.Range", 30);
		config.addDefault("Abilities.Earth.EarthLine.PrepareRange", 3);
		config.addDefault("Abilities.Earth.EarthLine.SourceKeepRange", 7);
		config.addDefault("Abilities.Earth.EarthLine.AffectingRadius", 2);
		config.addDefault("Abilities.Earth.EarthLine.AllowChangeDirection", true);
		config.addDefault("Abilities.Earth.EarthLine.MaxDuration", 2500);
		config.addDefault("Abilities.Earth.EarthLine.Damage", 3.0);
		config.addDefault("Abilities.Earth.EarthLine.RemovalPolicy.SwappedSlots.Enabled", false);
		
		config.addDefault("Abilities.Earth.EarthPillar.Enabled", false); // todo: needs a new implementation tbh
		config.addDefault("Abilities.Earth.EarthPillar.Description", "Raise a pillar of earth from a surface. "
				+ "Tap Sneak (Default: Shift) while looking at an earthbendable block.");
		config.addDefault("Abilities.Earth.EarthPillar.Height", 6);
		config.addDefault("Abilities.Earth.EarthPillar.Range", 10);
		
		config.addDefault("Abilities.Earth.EarthShard.Enabled", true);
		config.addDefault("Abilities.Earth.EarthShard.Description", "Raise a small earth shard and launch it at your target. "
				+ "Tap Sneak (Default: Shift) at an earthbendable block, then Left-Click at your target. "
				+ "You can have several shards ready at once.");
		config.addDefault("Abilities.Earth.EarthShard.Cooldown", 1000);
		config.addDefault("Abilities.Earth.EarthShard.Damage.Normal", 1.0);
		config.addDefault("Abilities.Earth.EarthShard.Damage.Metal", 1.5);
		config.addDefault("Abilities.Earth.EarthShard.PrepareRange", 5);
		config.addDefault("Abilities.Earth.EarthShard.AbilityRange", 30);
		config.addDefault("Abilities.Earth.EarthShard.MaxShards", 3);
		config.addDefault("Abilities.Earth.EarthShard.KnockUp.Others.Allow", true);
		config.addDefault("Abilities.Earth.EarthShard.KnockUp.Others.Velocity", 1.0);
		config.addDefault("Abilities.Earth.EarthShard.KnockUp.Others.Range", 1.5);
		config.addDefault("Abilities.Earth.EarthShard.KnockUp.Self.Allow", true);
		config.addDefault("Abilities.Earth.EarthShard.KnockUp.Self.Velocity", 1.0);
		config.addDefault("Abilities.Earth.EarthShard.KnockUp.Self.Range", 1.5);
		config.addDefault("Abilities.Earth.EarthShard.AbilityCollisionRadius", 2.0);
		config.addDefault("Abilities.Earth.EarthShard.EntityCollisionRadius", 1.4);
		
		config.addDefault("Abilities.Earth.EarthSurf.Enabled", true);
		config.addDefault("Abilities.Earth.EarthSurf.Description", "Ride a wave of earth across the ground. "
				+ "Left-Click while on or just above earthbendable ground. Sneak (Default: Shift) to stop. "
				+ "Nearby entities can be carried along.");
		config.addDefault("Abilities.Earth.EarthSurf.Cooldown.Cooldown", 3000);
		config.addDefault("Abilities.Earth.EarthSurf.Cooldown.MinimumCooldown", 2000);
		config.addDefault("Abilities.Earth.EarthSurf.Cooldown.Scaled", true);
		config.addDefault("Abilities.Earth.EarthSurf.Cooldown.Enabled", false);
		config.addDefault("Abilities.Earth.EarthSurf.Duration.Duration", 7000);
		config.addDefault("Abilities.Earth.EarthSurf.Duration.Enabled", false);
		config.addDefault("Abilities.Earth.EarthSurf.RelaxedCollisions", true);
		config.addDefault("Abilities.Earth.EarthSurf.RemoveOnAnyDamage", false);
		config.addDefault("Abilities.Earth.EarthSurf.Speed", 0.55);
		config.addDefault("Abilities.Earth.EarthSurf.HeightTolerance", 3);
		config.addDefault("Abilities.Earth.EarthSurf.SpringStiffness", 0.35);
		
		config.addDefault("Abilities.Earth.Fissure.Enabled", true);
		config.addDefault("Abilities.Earth.Fissure.Description", "Open a wide crack of lava in the ground. "
				+ "Left-Click to start it. Tap Sneak (Default: Shift) to make it wider. "
				+ "When it is fully open, look at it and tap Sneak to close it.");
		config.addDefault("Abilities.Earth.Fissure.Cooldown", 20000);
		config.addDefault("Abilities.Earth.Fissure.Duration", 15000);
		config.addDefault("Abilities.Earth.Fissure.MaxWidth", 3);
		config.addDefault("Abilities.Earth.Fissure.SlapRange", 12);
		config.addDefault("Abilities.Earth.Fissure.SlapDelay", 50);
		
		config.addDefault("Abilities.Earth.LavaDisc.Enabled", true);
		config.addDefault("Abilities.Earth.LavaDisc.Description", "Create a disc of lava at your fingertips. "
				+ "Hold Sneak (Default: Shift) while looking at a lava or earth source block. "
				+ "Release Sneak to shoot the disc. Hold Sneak again to call it back.");
		String[] meltable = {
				Material.COBBLESTONE.name(), Material.OAK_LOG.name(), Material.SPRUCE_LOG.name(),
				Material.JUNGLE_LOG.name(), Material.DARK_OAK_LOG.name(), Material.BIRCH_LOG.name(),
				Material.ACACIA_LOG.name()
		};
		config.addDefault("Abilities.Earth.LavaDisc.Cooldown", 7000);
		config.addDefault("Abilities.Earth.LavaDisc.Duration", 1000);
		config.addDefault("Abilities.Earth.LavaDisc.Damage", 4.0);
		config.addDefault("Abilities.Earth.LavaDisc.Particles", 3);
		config.addDefault("Abilities.Earth.LavaDisc.ContinueAfterEntityHit", false);
		config.addDefault("Abilities.Earth.LavaDisc.RecallLimit", 3);
		config.addDefault("Abilities.Earth.LavaDisc.Destroy.RegenTime", 5000);
		config.addDefault("Abilities.Earth.LavaDisc.Destroy.BlockDamage", true);
		config.addDefault("Abilities.Earth.LavaDisc.Destroy.AdditionalMeltableBlocks", meltable);
		config.addDefault("Abilities.Earth.LavaDisc.Destroy.LavaTrail", true);
		config.addDefault("Abilities.Earth.LavaDisc.Destroy.TrailFlow", false);
		config.addDefault("Abilities.Earth.LavaDisc.Source.RegenTime", 10000);
		config.addDefault("Abilities.Earth.LavaDisc.Source.LavaOnly", false);
		config.addDefault("Abilities.Earth.LavaDisc.Source.Range", 4.0);
		config.addDefault("Abilities.Earth.LavaDisc.RemovalPolicy.SwappedSlots.Enabled", true);
		
		config.addDefault("Abilities.Earth.LavaFlux.Enabled", true);
		config.addDefault("Abilities.Earth.LavaFlux.Description", "Send a wave of lava forward that burns, hurts, and knocks back everything in its path. "
				+ "Left-Click to use it.");
		config.addDefault("Abilities.Earth.LavaFlux.Range", 12);
		config.addDefault("Abilities.Earth.LavaFlux.Cooldown", 8000);
		config.addDefault("Abilities.Earth.LavaFlux.Duration", 4000);
		config.addDefault("Abilities.Earth.LavaFlux.Cleanup", 1000);
		config.addDefault("Abilities.Earth.LavaFlux.Damage", 1.0);
		config.addDefault("Abilities.Earth.LavaFlux.Speed", 1);
		config.addDefault("Abilities.Earth.LavaFlux.Wave", true);
		config.addDefault("Abilities.Earth.LavaFlux.KnockUp", 1.0);
		config.addDefault("Abilities.Earth.LavaFlux.KnockBack", 1.0);
		
		config.addDefault("Abilities.Earth.LavaThrow.Enabled", true);
		config.addDefault("Abilities.Earth.LavaThrow.Description", "Throw splashes of lava at your target. "
				+ "Look at a lava pool and tap Sneak (Default: Shift) to take lava, then Left-Click to throw it. "
				+ "You can throw several in a row.");
		config.addDefault("Abilities.Earth.LavaThrow.Cooldown", 7000);
		config.addDefault("Abilities.Earth.LavaThrow.MaxShots", 6);
		config.addDefault("Abilities.Earth.LavaThrow.Range", 20);
		config.addDefault("Abilities.Earth.LavaThrow.Damage", 1.0);
		config.addDefault("Abilities.Earth.LavaThrow.SourceGrabRange", 4);
		config.addDefault("Abilities.Earth.LavaThrow.SourceRegenDelay", 10000);
		config.addDefault("Abilities.Earth.LavaThrow.FireTicks", 80);
		config.addDefault("Abilities.Earth.LavaThrow.CurveFactor", 0.5);

		config.addDefault("Abilities.Earth.MagnetShield.Enabled", true);
		config.addDefault("Abilities.Earth.MagnetShield.Description", "Create a magnetic shield that pushes away arrows and nearby metal, "
				+ "including enemies wearing metal gear. Hold Sneak (Default: Shift) or Left-Click to activate it.");
		config.addDefault("Abilities.Earth.MagnetShield.Materials", Arrays.asList(
				"IRON_INGOT",
				"IRON_HELMET",
				"IRON_CHESTPLATE",
				"IRON_LEGGINGS",
				"IRON_BOOTS",
				"IRON_BLOCK",
				"IRON_AXE",
				"IRON_PICKAXE",
				"IRON_SWORD",
				"IRON_HOE",
				"IRON_SHOVEL",
				"IRON_DOOR",
				"IRON_NUGGET",
				"IRON_BARS",
				"IRON_HORSE_ARMOR",
				"IRON_TRAPDOOR",
				"HEAVY_WEIGHTED_PRESSURE_PLATE",
				"GOLD_INGOT",
				"GOLDEN_HELMET",
				"GOLDEN_CHESTPLATE",
				"GOLDEN_LEGGINGS",
				"GOLDEN_BOOTS",
				"GOLD_BLOCK",
				"GOLD_NUGGET",
				"GOLDEN_AXE",
				"GOLDEN_PICKAXE",
				"GOLDEN_SHOVEL",
				"GOLDEN_SWORD",
				"GOLDEN_HOE",
				"GOLDEN_HORSE_ARMOR",
				"LIGHT_WEIGHTED_PRESSURE_PLATE",
				"CLOCK",
				"COMPASS",
				"RAW_GOLD_BLOCK",
				"RAW_IRON_BLOCK",
				"RAW_IRON",
				"RAW_GOLD",
				"ANVIL",
				"CHIPPED_ANVIL",
				"DAMAGED_ANVIL",
				"IRON_ORE",
				"GOLD_ORE",
				"DEEPSLATE_IRON_ORE",
				"DEEPSLATE_GOLD_ORE",
				"SHIELD"
		));
		config.addDefault("Abilities.Earth.MagnetShield.Duration", 6000);
		config.addDefault("Abilities.Earth.MagnetShield.Cooldowns.Shift", 5000);
		config.addDefault("Abilities.Earth.MagnetShield.Cooldowns.Click", 5000);
		config.addDefault("Abilities.Earth.MagnetShield.Range", 5.0);
		config.addDefault("Abilities.Earth.MagnetShield.RepelArrows", true);
		config.addDefault("Abilities.Earth.MagnetShield.RepelLivingEntities", true);
		config.addDefault("Abilities.Earth.MagnetShield.Velocity", 0.1);
		
		config.addDefault("Abilities.Earth.MetalFragments.Enabled", true);
		config.addDefault("Abilities.Earth.MetalFragments.Description", "Pull metal from up to 3 metal blocks and shoot it at your target. "
				+ "Tap Sneak (Default: Shift) at a metal block to lift it. Tap Sneak at more metal to add sources. "
				+ "Left-Click to fire fragments.");
		config.addDefault("Abilities.Earth.MetalFragments.Cooldown", 5000);
		config.addDefault("Abilities.Earth.MetalFragments.MaxSources", 3);
		config.addDefault("Abilities.Earth.MetalFragments.SourceRange", 5);
		config.addDefault("Abilities.Earth.MetalFragments.MaxFragments", 10);
		config.addDefault("Abilities.Earth.MetalFragments.Damage", 4.0);
		config.addDefault("Abilities.Earth.MetalFragments.Velocity", 2.0);
		
		config.addDefault("Abilities.Earth.MetalHook.Enabled", true);
		config.addDefault("Abilities.Earth.MetalHook.Description", "Shoot metal grappling hooks to swing across terrain. "
				+ "You need iron ingots or blocks in your inventory, or an iron or chainmail chestplate. "
				+ "Left-Click to fire a hook. Hold Sneak (Default: Shift) or Sprint to let go.");
		config.addDefault("Abilities.Earth.MetalHook.Cooldown", 3000);
		config.addDefault("Abilities.Earth.MetalHook.Range", 30);
		config.addDefault("Abilities.Earth.MetalHook.MaxHooks", 3);
		config.addDefault("Abilities.Earth.MetalHook.TotalHooks", 0);
		config.addDefault("Abilities.Earth.MetalHook.RequireItems", true);
		config.addDefault("Abilities.Earth.MetalHook.BarrierHooking", true);
		
		config.addDefault("Abilities.Earth.MetalShred.Enabled", true);
		config.addDefault("Abilities.Earth.MetalShred.Description", "Tear open a metal wall so you can pass through. "
				+ "Look at a metal surface and tap Sneak (Default: Shift). Sprint along the wall to pull the metal with you. "
				+ "Left-Click to unfold it in the direction you look.");
		config.addDefault("Abilities.Earth.MetalShred.SourceRange", 5);
        config.addDefault("Abilities.Earth.MetalShred.ExtendTick", 80);
        config.addDefault("Abilities.Earth.MetalShred.Damage", 6.0);
        
        config.addDefault("Abilities.Earth.MudSurge.Enabled", true);
		config.addDefault("Abilities.Earth.MudSurge.Description", "Send a wave of mud that knocks back enemies, damages them, and may blind them. "
				+ "Look at an earth source and tap Sneak (Default: Shift), then Left-Click in a direction.");
		config.addDefault("Abilities.Earth.MudSurge.Cooldown", 6000);
		config.addDefault("Abilities.Earth.MudSurge.Damage", 1.0);
		config.addDefault("Abilities.Earth.MudSurge.Waves", 5);
		config.addDefault("Abilities.Earth.MudSurge.SourceRange", 7);
		config.addDefault("Abilities.Earth.MudSurge.BlindChance", 10);
		config.addDefault("Abilities.Earth.MudSurge.WetSourceOnly", false);
		config.addDefault("Abilities.Earth.MudSurge.WaterSearchRadius", 5);
		config.addDefault("Abilities.Earth.MudSurge.BlindTicks", 60);
		config.addDefault("Abilities.Earth.MudSurge.CollisionRadius", 2.0);
		config.addDefault("Abilities.Earth.MudSurge.MultipleHits", true);
		config.addDefault("Abilities.Earth.MudSurge.AllowFallDamage", false);
		config.addDefault("Abilities.Earth.MudSurge.RemovalPolicy.SwappedSlots.Enabled", true);
		config.addDefault("Abilities.Earth.MudSurge.RemovalPolicy.OutOfRange.Enabled", true);
		config.addDefault("Abilities.Earth.MudSurge.RemovalPolicy.OutOfRange.Range", 25.0);
		
		config.addDefault("Abilities.Earth.SandBlast.Enabled", true);
		config.addDefault("Abilities.Earth.SandBlast.Description", "Blast sand at your target to damage and briefly blind them. "
				+ "Tap Sneak (Default: Shift) at a sand block, then Left-Click in a direction.");
		config.addDefault("Abilities.Earth.SandBlast.Cooldown", 3000);
		config.addDefault("Abilities.Earth.SandBlast.Damage", 3.0);
		config.addDefault("Abilities.Earth.SandBlast.SourceRange", 8);
		config.addDefault("Abilities.Earth.SandBlast.Range", 30);
		config.addDefault("Abilities.Earth.SandBlast.MaxSandBlocks", 10);
		
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.Enabled", true);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.Description", "Open a deep crevice in the ground. "
				+ "This is an Earth combo. You can close it by tapping Sneak (Default: Shift) with Shockwave while looking at it.");
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.Range", 50);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.RevertDelay", 7500);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.Depth", 5);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.Width", 3);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.AvatarStateDepth", 8);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.CloseWithSneak", true);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.OnlyUserCanClose", true);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.Cooldown", 10000);
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.Combination", Arrays.asList("Collapse:RIGHT_CLICK_BLOCK", "Shockwave:SHIFT_DOWN", "Shockwave:SHIFT_UP", "Shockwave:SHIFT_DOWN"));
		config.addDefault("Abilities.Earth.EarthCombo.Crevice.Instructions", "Collapse (Right-click a block) > Shockwave (Tap sneak) > Shockwave (Tap sneak)");

		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.Enabled", true);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.Description", "Fire balls of magma at your enemies. "
				+ "This is an Earth combo. Left-Click to fire each ball.");
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.MaxShots", 3);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.ImpactDamage", 2.0);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.SearchRange", 4);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.Cooldown", 6000);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.ShotCooldown", 1500);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.RequireLavaFlow", false);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.PlayerCollisions", true);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.EntitySelection", true);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.SelectRange", 30.0);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.ExplosionRadius", 2.0);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.FireSpeed", 1.5);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.MaxDuration", 15000);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.MaxDistanceFromSources", 15);
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.Combination", Arrays.asList("EarthBlast:SHIFT_DOWN", "LavaFlow:SHIFT_UP", "LavaFlow:SHIFT_DOWN", "LavaFlow:RIGHT_CLICK_BLOCK"));
		config.addDefault("Abilities.Earth.EarthCombo.MagmaBlast.Instructions", "EarthBlast (Hold sneak) > LavaFlow (Release sneak) > LavaFlow (Hold sneak) > LavaFlow (Right-click a block) > LavaFlow (Left-click multiple times)");


		ArrayList<String> combustionSkipMaterials = new ArrayList<>();
		combustionSkipMaterials.add("#bee_growables");
		combustionSkipMaterials.add("#cave_vines");
		combustionSkipMaterials.add("#crops");
		combustionSkipMaterials.add("#flowers");
		combustionSkipMaterials.add("vine");
		combustionSkipMaterials.add("pale_hanging_moss");

		config.addDefault("Abilities.Fire.Combustion.Enabled", true);
		config.addDefault("Abilities.Fire.Combustion.Description", "Charge and fire an explosive beam. "
				+ "Hold Sneak (Default: Shift) to charge, then release to fire. "
				+ "While the beam is flying, Left-Click to make it explode early.");
		config.addDefault("Abilities.Fire.Combustion.Damage", 4.0);
		config.addDefault("Abilities.Fire.Combustion.FireTick", 100);
		config.addDefault("Abilities.Fire.Combustion.MisfireModifier", -1);
		config.addDefault("Abilities.Fire.Combustion.Power", 3);
		config.addDefault("Abilities.Fire.Combustion.Range", 100);
		config.addDefault("Abilities.Fire.Combustion.Speed", 0.65);
		config.addDefault("Abilities.Fire.Combustion.Warmup", 1500);
		config.addDefault("Abilities.Fire.Combustion.Cooldown", 5000);
		config.addDefault("Abilities.Fire.Combustion.RegenTime", 10000);
		config.addDefault("Abilities.Fire.Combustion.EntityCollisionRadius", 1.3);
		config.addDefault("Abilities.Fire.Combustion.AbilityCollisionRadius", 1.3);
		config.addDefault("Abilities.Fire.Combustion.DamageBlocks", true);
		config.addDefault("Abilities.Fire.Combustion.RegenBlocks", true);
		config.addDefault("Abilities.Fire.Combustion.WaitForRegen", true);
		config.addDefault("Abilities.Fire.Combustion.InstantExplodeIfHit", true);
		config.addDefault("Abilities.Fire.Combustion.ExplodeOnDeath", true);
		config.addDefault("Abilities.Fire.Combustion.RemovalPolicy.SwappedSlots.Enabled", false);
		config.addDefault("Abilities.Fire.Combustion.SkipMaterials", combustionSkipMaterials);

		config.addDefault("Abilities.Fire.Discharge.Enabled", true);
		config.addDefault("Abilities.Fire.Discharge.Description", "Shoot bolts of electricity from your fingertips at whatever you hit. "
				+ "Left-Click to use it.");
		config.addDefault("Abilities.Fire.Discharge.Damage", 3.0);
		config.addDefault("Abilities.Fire.Discharge.Cooldown", 5000);
		config.addDefault("Abilities.Fire.Discharge.AvatarCooldown", 500);
		config.addDefault("Abilities.Fire.Discharge.Duration", 1000);
		config.addDefault("Abilities.Fire.Discharge.SlotSwapping", false);
		config.addDefault("Abilities.Fire.Discharge.EntityCollisionRadius", 1.0);
		config.addDefault("Abilities.Fire.Discharge.AbilityCollisionRadius", 1.0);
		config.addDefault("Abilities.Fire.Discharge.Sound.Volume", 0.6);
		config.addDefault("Abilities.Fire.Discharge.Sound.Interval", 6);
		config.addDefault("Abilities.Fire.Discharge.Stun.Chance", 0.01);
		config.addDefault("Abilities.Fire.Discharge.Stun.Duration", 1000);
		
		config.addDefault("Abilities.Fire.FireBall.Enabled", true);
		config.addDefault("Abilities.Fire.FireBall.Description", "Shoot a fireball at your target. "
				+ "Left-Click to use it.");
		config.addDefault("Abilities.Fire.FireBall.Cooldown", 3000);
		config.addDefault("Abilities.Fire.FireBall.Range", 50);
		config.addDefault("Abilities.Fire.FireBall.Damage", 3.0);
		config.addDefault("Abilities.Fire.FireBall.FireDuration", 2000);
		config.addDefault("Abilities.Fire.FireBall.Controllable", false);
		config.addDefault("Abilities.Fire.FireBall.FireTrail", true);
		config.addDefault("Abilities.Fire.FireBall.CollisionRadius", 1.1);
		config.addDefault("Abilities.Fire.FireBall.Collisions.FireShield.Enabled", true);
		config.addDefault("Abilities.Fire.FireBall.Collisions.FireShield.RemoveFirst", true);
		config.addDefault("Abilities.Fire.FireBall.Collisions.FireShield.RemoveSecond", false);
		config.addDefault("Abilities.Fire.FireBall.Collisions.AirShield.Enabled", true);
		config.addDefault("Abilities.Fire.FireBall.Collisions.AirShield.RemoveFirst", false);
		config.addDefault("Abilities.Fire.FireBall.Collisions.AirShield.RemoveSecond", false);
		config.addDefault("Abilities.Fire.FireBall.Collisions.AirShield.Reflect", true);
		
		config.addDefault("Abilities.Fire.FireBreath.Enabled", true);
		config.addDefault("Abilities.Fire.FireBreath.Description", "Breathe a stream of fire. "
				+ "Hold Sneak (Default: Shift) to use it. ");
				// rainbow fire chat message easter egg: Bring fire and light together as one and allow the breath of color
				// stop rainbow fire: Split the bond of fire and light and set the color free
		config.addDefault("Abilities.Fire.FireBreath.Cooldown", 5000);
		config.addDefault("Abilities.Fire.FireBreath.Duration", 3000);
		config.addDefault("Abilities.Fire.FireBreath.Particles", 3);
		config.addDefault("Abilities.Fire.FireBreath.Damage.Player", 1.0);
		config.addDefault("Abilities.Fire.FireBreath.Damage.Mob", 2.0);
		config.addDefault("Abilities.Fire.FireBreath.FireDuration", 3000);
		config.addDefault("Abilities.Fire.FireBreath.Range", 10);
		config.addDefault("Abilities.Fire.FireBreath.Avatar.FireEnabled", true);
		config.addDefault("Abilities.Fire.FireBreath.Melt.Enabled", true);
		config.addDefault("Abilities.Fire.FireBreath.Melt.Chance", 3);
		config.addDefault("Abilities.Fire.FireBreath.Melt.OnlyIce", true);
		config.addDefault("Abilities.Fire.FireBreath.RainbowBreath.Enabled", true);
		config.addDefault("Abilities.Fire.FireBreath.RainbowBreath.EnabledMessage", "You have bonded fire with light and can now breathe pure color.");
		config.addDefault("Abilities.Fire.FireBreath.RainbowBreath.DisabledMessage", "You have split your bond of color and light.");
		config.addDefault("Abilities.Fire.FireBreath.RainbowBreath.NoAccess", "You don't possess the power to bond light with fire.");
		
		config.addDefault("Abilities.Fire.FireComet.Enabled", true);
		config.addDefault("Abilities.Fire.FireComet.Description", "Create a huge ball of fire. "
				+ "Only usable during Sozin's Comet or in the AvatarState. "
				+ "Hold Sneak (Default: Shift) to charge, then release to launch it where you look.");
		config.addDefault("Abilities.Fire.FireComet.Cooldown", 45000);
		config.addDefault("Abilities.Fire.FireComet.ChargeUp", 7000);
		config.addDefault("Abilities.Fire.FireComet.Damage", 6.0);
		config.addDefault("Abilities.Fire.FireComet.BlastRadius", 3.0);
		config.addDefault("Abilities.Fire.FireComet.SozinsComet.Cooldown", 30000);
		config.addDefault("Abilities.Fire.FireComet.SozinsComet.ChargeUp", 5000);
		config.addDefault("Abilities.Fire.FireComet.SozinsComet.Damage", 12.0);
		config.addDefault("Abilities.Fire.FireComet.SozinsComet.BlastRadius", 5.0);
		config.addDefault("Abilities.Fire.FireComet.Range", 50);
		config.addDefault("Abilities.Fire.FireComet.RegenDelay", 15000);
		config.addDefault("Abilities.Fire.FireComet.DamageBlocks", true);
		config.addDefault("Abilities.Fire.FireComet.SozinsCometOnly", true);
		config.addDefault("Abilities.Fire.FireComet.AvatarStateBypassComet", true);
		
		config.addDefault("Abilities.Fire.FirePunch.Enabled", true);
		config.addDefault("Abilities.Fire.FirePunch.Description", "Charge your fist with fire and punch an enemy to ignite and damage them. "
				+ "Left-Click to ready it, then hit an enemy. Tap Sneak (Default: Shift) to switch hands.");
		config.addDefault("Abilities.Fire.FirePunch.Cooldown", 4000);
		config.addDefault("Abilities.Fire.FirePunch.FireTicks", 2000);
		config.addDefault("Abilities.Fire.FirePunch.Damage", 2.0);
		config.addDefault("Abilities.Fire.FirePunch.BurnsDroppedItems", true);

		config.addDefault("Abilities.Fire.FireShots.Enabled", true);
		config.addDefault("Abilities.Fire.FireShots.Description", "Summon several small fireballs and shoot them one by one. "
				+ "Tap Sneak (Default: Shift) to summon them, then Left-Click to shoot. "
				+ "Tap Sneak again to switch hands.");
		config.addDefault("Abilities.Fire.FireShots.Cooldown", 3000);
		config.addDefault("Abilities.Fire.FireShots.Range", 50);
		config.addDefault("Abilities.Fire.FireShots.FireBalls", 4);
		config.addDefault("Abilities.Fire.FireShots.FireDuration", 3000);
		config.addDefault("Abilities.Fire.FireShots.Damage", 2.0);
		config.addDefault("Abilities.Fire.FireShots.CollisionRadius", 0.9);
		config.addDefault("Abilities.Fire.FireShots.Collisions.FireShield.Enabled", true);
		config.addDefault("Abilities.Fire.FireShots.Collisions.FireShield.RemoveFirst", true);
		config.addDefault("Abilities.Fire.FireShots.Collisions.FireShield.RemoveSecond", false);
		config.addDefault("Abilities.Fire.FireShots.Collisions.AirShield.Enabled", true);
		config.addDefault("Abilities.Fire.FireShots.Collisions.AirShield.RemoveFirst", false);
		config.addDefault("Abilities.Fire.FireShots.Collisions.AirShield.RemoveSecond", false);
		config.addDefault("Abilities.Fire.FireShots.Collisions.AirShield.Reflect", true);
		
		config.addDefault("Abilities.Fire.FireSki.Enabled", true);
		config.addDefault("Abilities.Fire.FireSki.Cooldown", 6000);
		config.addDefault("Abilities.Fire.FireSki.Duration", 6000);
		config.addDefault("Abilities.Fire.FireSki.Speed", 0.7);
		config.addDefault("Abilities.Fire.FireSki.IgniteEntities", true);
		config.addDefault("Abilities.Fire.FireSki.FireTicks", 60);
		config.addDefault("Abilities.Fire.FireSki.RequiredHeight", 0.7);
		config.addDefault("Abilities.Fire.FireSki.PunchActivated", false);
		
		config.addDefault("Abilities.Fire.LightningBurst.Enabled", true);
		config.addDefault("Abilities.Fire.LightningBurst.Description", "Charge a sphere of lightning and release it to shock everyone nearby. "
				+ "Hold Sneak (Default: Shift) until blue sparks appear, then release.");
		config.addDefault("Abilities.Fire.LightningBurst.Cooldown", 25000);
		config.addDefault("Abilities.Fire.LightningBurst.ChargeUp", 4000);
		config.addDefault("Abilities.Fire.LightningBurst.AvatarCooldown", 1000);
		config.addDefault("Abilities.Fire.LightningBurst.AvatarChargeUp", 1000);
		config.addDefault("Abilities.Fire.LightningBurst.Radius", 12);
		config.addDefault("Abilities.Fire.LightningBurst.Damage", 9.0);
		config.addDefault("Abilities.Fire.LightningBurst.Sound.Volume", 0.6);
		config.addDefault("Abilities.Fire.LightningBurst.Sound.Interval", 6);
		config.addDefault("Abilities.Fire.LightningBurst.Stun.Chance", 0.1);
		config.addDefault("Abilities.Fire.LightningBurst.Stun.Duration", 1000);


		config.addDefault("Abilities.Water.Bloodbending.Enabled", true);
		config.addDefault("Abilities.Water.Bloodbending.Description", "Control an enemy's body by bending the water in their blood. "
				+ "Hold Sneak (Default: Shift) while looking at them. Left-Click to throw them where you are looking. "
				+ "Stop Sneak to release them.");
		config.addDefault("Abilities.Water.Bloodbending.NightOnly", false);
		config.addDefault("Abilities.Water.Bloodbending.FullMoonOnly", false);
		config.addDefault("Abilities.Water.Bloodbending.UndeadMobs", true);
		config.addDefault("Abilities.Water.Bloodbending.IgnoreWalls", false);
		config.addDefault("Abilities.Water.Bloodbending.RequireBound", false);
		config.addDefault("Abilities.Water.Bloodbending.AffectBloodbenders", false);
		config.addDefault("Abilities.Water.Bloodbending.Distance", 6);
		config.addDefault("Abilities.Water.Bloodbending.HoldTime", 10000);
		config.addDefault("Abilities.Water.Bloodbending.Cooldown", 4000);

		config.addDefault("Abilities.Water.BloodPuppet.Enabled", true);
		config.addDefault("Abilities.Water.BloodPuppet.Description", "Control an enemy's body and force them to attack for you. "
				+ "You must be a bloodbender. Hold Sneak (Default: Shift) while looking at them, then Left-Click to make them attack. "
				+ "Stop Sneak to release them.");
		config.addDefault("Abilities.Water.BloodPuppet.NightOnly", false);
		config.addDefault("Abilities.Water.BloodPuppet.FullMoonOnly", false);
		config.addDefault("Abilities.Water.BloodPuppet.UndeadMobs", true);
		config.addDefault("Abilities.Water.BloodPuppet.IgnoreWalls", false);
		config.addDefault("Abilities.Water.BloodPuppet.RequireBound", false);
		config.addDefault("Abilities.Water.BloodPuppet.AffectBloodbenders", false);
		config.addDefault("Abilities.Water.BloodPuppet.Distance", 6);
		config.addDefault("Abilities.Water.BloodPuppet.HoldTime", 10000);
		config.addDefault("Abilities.Water.BloodPuppet.Cooldown", 4000);
		
		config.addDefault("Abilities.Water.Drain.Enabled", true);
		config.addDefault("Abilities.Water.Drain.Description", "Drain water from plants into bottles or buckets, or shoot small water blasts. "
				+ "Hold Sneak (Default: Shift) near plants to drain them. If you have nothing to fill, hold Sneak and Left-Click to shoot a water blast. "
				+ "You can also fill bottles from water or rain.");
		config.addDefault("Abilities.Water.Drain.RegenDelay", 15000);
		config.addDefault("Abilities.Water.Drain.Duration", 2000);
		config.addDefault("Abilities.Water.Drain.Cooldown", 2000);
		config.addDefault("Abilities.Water.Drain.AbsorbSpeed", 0.1);
		config.addDefault("Abilities.Water.Drain.AbsorbChance", 20);
		config.addDefault("Abilities.Water.Drain.AbsorbRate", 6);
		config.addDefault("Abilities.Water.Drain.Radius", 6);
		config.addDefault("Abilities.Water.Drain.HoldRange", 2);
		config.addDefault("Abilities.Water.Drain.AllowRainSource", true);
		config.addDefault("Abilities.Water.Drain.BlastsEnabled", true);
		config.addDefault("Abilities.Water.Drain.KeepSource", false);
		config.addDefault("Abilities.Water.Drain.BlastSpeed", 1);
		config.addDefault("Abilities.Water.Drain.BlastDamage", 1.5);
		config.addDefault("Abilities.Water.Drain.BlastRange", 20);
		config.addDefault("Abilities.Water.Drain.MaxBlasts", 4);
		config.addDefault("Abilities.Water.Drain.DrainTempBlocks", true);
		
		config.addDefault("Abilities.Water.FrostBreath.Enabled", false); // ported to PK Core
		config.addDefault("Abilities.Water.FrostBreath.Description", "Breathe freezing air that freezes what it touches. "
				+ "Hold Sneak (Default: Shift) to use it.");
		config.addDefault("Abilities.Water.FrostBreath.Cooldown", 15000);
		config.addDefault("Abilities.Water.FrostBreath.Duration", 3000);
		config.addDefault("Abilities.Water.FrostBreath.Particles", 3);
		config.addDefault("Abilities.Water.FrostBreath.FrostDuration", 5000);
		config.addDefault("Abilities.Water.FrostBreath.FrozenWaterDuration", 10000);
		config.addDefault("Abilities.Water.FrostBreath.Range", 10);
		config.addDefault("Abilities.Water.FrostBreath.Snow", true);
		config.addDefault("Abilities.Water.FrostBreath.SnowDuration", 5000);
		config.addDefault("Abilities.Water.FrostBreath.SnowUnderEntities", true);
		config.addDefault("Abilities.Water.FrostBreath.BendableSnow", false);
		config.addDefault("Abilities.Water.FrostBreath.Damage.Enabled", false);
		config.addDefault("Abilities.Water.FrostBreath.Damage.Player", 1.0);
		config.addDefault("Abilities.Water.FrostBreath.Damage.Mob", 2.0);
		config.addDefault("Abilities.Water.FrostBreath.Slow.Enabled", true);
		config.addDefault("Abilities.Water.FrostBreath.Slow.Duration", 4000);
		config.addDefault("Abilities.Water.FrostBreath.RestrictBiomes", true);
		
		config.addDefault("Abilities.Water.HealingWaters.Enabled", true);
		config.addDefault("Abilities.Water.HealingWaters.Description", "Heal yourself and others with water. "
				+ "Stand in water to heal yourself. Hold Sneak (Default: Shift) while looking at a hurt ally in water to heal them. "
				+ "If you hold a water bottle or bucket, Sneak to heal yourself or the person you target.");
		config.addDefault("Abilities.Water.HealingWaters.Power", 1);
		config.addDefault("Abilities.Water.HealingWaters.Range", 5);
		config.addDefault("Abilities.Water.HealingWaters.DrainChance", 5);

		config.addDefault("Abilities.Water.IceClaws.Enabled", true);
		config.addDefault("Abilities.Water.IceClaws.Description", "Create ice claws on your fingertips. "
				+ "Hold Sneak (Default: Shift) until they form. "
				+ "Punch an enemy to slow and damage them, or Left-Click to throw the claws. "
		        + "Tap Sneak again to switch hands.");
		config.addDefault("Abilities.Water.IceClaws.Cooldown", 6000);
		config.addDefault("Abilities.Water.IceClaws.ChargeTime", 1000);
		config.addDefault("Abilities.Water.IceClaws.Range", 10);
		config.addDefault("Abilities.Water.IceClaws.Throwable", true);
		config.addDefault("Abilities.Water.IceClaws.Punch.Cooldown", 4000);
		config.addDefault("Abilities.Water.IceClaws.Punch.Damage", 2.0);
		config.addDefault("Abilities.Water.IceClaws.Punch.Slowness", 3);
		config.addDefault("Abilities.Water.IceClaws.Punch.SlowDuration", 5000);
		config.addDefault("Abilities.Water.IceClaws.Throw.Cooldown", 4000);
		config.addDefault("Abilities.Water.IceClaws.Throw.Damage", 2.0);
		config.addDefault("Abilities.Water.IceClaws.Throw.Slowness", 3);
		config.addDefault("Abilities.Water.IceClaws.Throw.SlowDuration", 5000);
		config.addDefault("Abilities.Water.IceClaws.Throw.Speed", 1.0);
		config.addDefault("Abilities.Water.IceClaws.Throw.CooldownOnThrow", true);
		config.addDefault("Abilities.Water.IceClaws.AllowHandSwap", true);
		
		config.addDefault("Abilities.Water.IceWall.Enabled", true);
		config.addDefault("Abilities.Water.IceWall.Description", "Raise a wall of ice from water, ice, or snow. "
				+ "Sneak (Default: Shift) while looking at it. Sneak at the wall again to break it. "
				+ "Nearby enemies can be hurt when it breaks.");
		config.addDefault("Abilities.Water.IceWall.Cooldown", 4000);
		config.addDefault("Abilities.Water.IceWall.Width", 6);
		config.addDefault("Abilities.Water.IceWall.MaxHeight", 5);
		config.addDefault("Abilities.Water.IceWall.MinHeight", 3);
		config.addDefault("Abilities.Water.IceWall.MaxWallHealth", 12);
		config.addDefault("Abilities.Water.IceWall.MinWallHealth", 8);
		config.addDefault("Abilities.Water.IceWall.Range", 8);
		config.addDefault("Abilities.Water.IceWall.Damage", 4.0);
		config.addDefault("Abilities.Water.IceWall.DamageRadius", 2.5);
		config.addDefault("Abilities.Water.IceWall.CanBreak", true);
		config.addDefault("Abilities.Water.IceWall.Stackable", false);
		config.addDefault("Abilities.Water.IceWall.CanSourceWall", false);
		config.addDefault("Abilities.Water.IceWall.LifeTime.Enabled", false);
		config.addDefault("Abilities.Water.IceWall.LifeTime.Duration", 10000);
		config.addDefault("Abilities.Water.IceWall.WallDamage", true);
		config.addDefault("Abilities.Water.IceWall.WallDamage.Torrent", 5);
		config.addDefault("Abilities.Water.IceWall.WallDamage.TorrentFreeze", 9);
		config.addDefault("Abilities.Water.IceWall.WallDamage.IceBlast", 8);
		config.addDefault("Abilities.Water.IceWall.WallDamage.Fireblast", 3);
		config.addDefault("Abilities.Water.IceWall.WallDamage.FireblastCharged", 5);
		config.addDefault("Abilities.Water.IceWall.WallDamage.Lightning", 12);
		config.addDefault("Abilities.Water.IceWall.WallDamage.Combustion", 12);
		config.addDefault("Abilities.Water.IceWall.WallDamage.EarthSmash", 8);
		config.addDefault("Abilities.Water.IceWall.WallDamage.AirBlast", 2);
		
		config.addDefault("Abilities.Water.WakeFishing.Enabled", true);
		config.addDefault("Abilities.Water.WakeFishing.Description", "Fish with waterbending. "
				+ "Hold Sneak (Default: Shift) and keep looking at a water block. "
				+ "After a moment, fish will jump out at you.");
		config.addDefault("Abilities.Water.WakeFishing.Cooldown", 10000);
		config.addDefault("Abilities.Water.WakeFishing.Duration", 20000);
		config.addDefault("Abilities.Water.WakeFishing.Range", 5);
		
		config.addDefault("Abilities.Water.WaterCombo.Maelstrom.Enabled", true);
		config.addDefault("Abilities.Water.WaterCombo.Maelstrom.Description", "Create a swirling whirlpool that pulls anything inside it to the bottom. "
				+ "This is a Water combo. You must use it on deep water.");
		config.addDefault("Abilities.Water.WaterCombo.Maelstrom.Cooldown", 25000);
		config.addDefault("Abilities.Water.WaterCombo.Maelstrom.Duration", 15000);
		config.addDefault("Abilities.Water.WaterCombo.Maelstrom.MaxDepth", 5);
		config.addDefault("Abilities.Water.WaterCombo.Maelstrom.Range", 10);
		config.addDefault("Abilities.Water.WaterCombo.Maelstrom.Combination", Arrays.asList("PhaseChange:SHIFT_DOWN", "Torrent:LEFT_CLICK", "Torrent:LEFT_CLICK"));
		config.addDefault("Abilities.Water.WaterCombo.Maelstrom.Instructions", "PhaseChange (Hold sneak) > Torrent (Left-click) > Torrent (Left-click)");
		
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Enabled", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Description", "Create a powerful stream of water that follows your aim and can carry you and others. "
				+ "This is a Water combo. Hold Sneak (Default: Shift) to pull it back. "
				+ "Left-Click with WaterManipulation to freeze it if you can icebend.");
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Cooldown", 8000);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Duration", 8000);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.MeltDelay", 5000);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.SourceRange", 10);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.MaxRange", 40);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.MinRange", 8);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Trail", 80);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.BottleSource", false);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.PlantSource", false);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.RemoveOnAnyDamage", false);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Size.Normal", 1);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Size.AvatarState", 3);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Size.FullmoonSmall", 2);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Size.FullmoonLarge", 3);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.IsAvatarStateToggle", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.AvatarStateDuration", 60000);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.PlayerStayNearSource", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.MaxDistanceFromSource", 100);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.FullMoon.Enabled", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.FullMoon.Modifier.Cooldown", 3);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.FullMoon.Modifier.Duration", 2);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.PlayerRideOwnFlow", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.RequireAdjacentSources", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Combination", Arrays.asList("WaterManipulation:SHIFT_DOWN", "WaterManipulation:SHIFT_UP", "Torrent:SHIFT_DOWN", "Torrent:SHIFT_UP", "Torrent:SHIFT_DOWN", "WaterManipulation:SHIFT_UP"));
		config.addDefault("Abilities.Water.WaterCombo.WaterFlow.Instructions", "WaterManipulation (Tap sneak) > Torrent (Tap sneak) > Torrent (Hold sneak) > WaterManipulation (Release sneak)");
		
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Enabled", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Description", "Create two spinning rings of water around you. "
				+ "This is a Water combo. Hold Sneak (Default: Shift) to keep them spinning, "
				+ "and Left-Click with WaterManipulation to shoot a ring at your enemies.");
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Cooldown", 7000);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Damage", 3.0);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.RingSize", 3.5);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Range", 40);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.SourceRange", 10);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Speed", 2);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.AnimationSpeed", 3);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.PlantSource", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.SnowSource", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.RequireAdjacentPlants", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.RequireAdjacentSources", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.BottleSource", false);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.AbilityCollisionRadius", 1.6);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.EntityCollisionRadius", 1.6);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Collisions.FireShield.Enabled", false);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Collisions.FireShield.RemoveFirst", true);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Collisions.FireShield.RemoveSecond", false);
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Combination", Arrays.asList("Torrent:SHIFT_DOWN", "Torrent:SHIFT_UP", "Torrent:SHIFT_DOWN", "Torrent:SHIFT_UP", "WaterManipulation:SHIFT_DOWN"));
		config.addDefault("Abilities.Water.WaterCombo.WaterGimbal.Instructions", "Torrent (Tap sneak) > Torrent (Tap sneak) > WaterManipulation (Hold sneak) > WaterManipulation (Left-click multiple times)");
		
		config.addDefault("Abilities.Water.Ice.Passive.Skate.Enabled", true);
		config.addDefault("Abilities.Water.Ice.Passive.Skate.SpeedFactor", 4);
		config.addDefault("Abilities.Water.Ice.Passive.Skate.LeaveIceDuration", 60);


		config.addDefault("Abilities.Chi.Backstab.Enabled", true);
		config.addDefault("Abilities.Chi.Backstab.Description", "Strike an enemy from behind to block their chi and deal heavy damage. "
				+ "Hit them in the back with a melee attack. It has a long cooldown and fails if you are not behind them.");
		config.addDefault("Abilities.Chi.Backstab.Cooldown", 8500);
		config.addDefault("Abilities.Chi.Backstab.Damage", 6.0);
		config.addDefault("Abilities.Chi.Backstab.MaxActivationAngle", 90);
		
		config.addDefault("Abilities.Chi.DaggerThrow.Enabled", true);
		config.addDefault("Abilities.Chi.DaggerThrow.Description", "Shoot arrows at your target. "
				+ "Left-Click quickly to throw several.");
		config.addDefault("Abilities.Chi.DaggerThrow.Cooldown", 3000);
		config.addDefault("Abilities.Chi.DaggerThrow.MaxDaggers.Enabled", true);
		config.addDefault("Abilities.Chi.DaggerThrow.MaxDaggers.Amount", 6);
		config.addDefault("Abilities.Chi.DaggerThrow.Damage", 1.0);
		config.addDefault("Abilities.Chi.DaggerThrow.ParticleTrail", true);
		config.addDefault("Abilities.Chi.DaggerThrow.RequireArrows", false);
		config.addDefault("Abilities.Chi.DaggerThrow.AllowPickup", false);
		config.addDefault("Abilities.Chi.DaggerThrow.AbilityCollisionRadius", 0.5);
		config.addDefault("Abilities.Chi.DaggerThrow.Interactions.WaterSpout.Enabled", true);
		config.addDefault("Abilities.Chi.DaggerThrow.Interactions.WaterSpout.Cooldown", 1000);
		config.addDefault("Abilities.Chi.DaggerThrow.Interactions.WaterSpout.HitsRequired", 1);
		config.addDefault("Abilities.Chi.DaggerThrow.Interactions.AirSpout.Enabled", true);
		config.addDefault("Abilities.Chi.DaggerThrow.Interactions.AirSpout.Cooldown", 1000);
		config.addDefault("Abilities.Chi.DaggerThrow.Interactions.AirSpout.HitsRequired", 1);

		String[] invalidWallRun = {Material.BARRIER.name()};
		config.addDefault("Abilities.Passives.WallRun.Enabled", true);
		config.addDefault("Abilities.Passives.WallRun.Cooldown", 6000);
		config.addDefault("Abilities.Passives.WallRun.Duration", 20000);
		config.addDefault("Abilities.Passives.WallRun.Particles", true);
		config.addDefault("Abilities.Passives.WallRun.Air", true);
		config.addDefault("Abilities.Passives.WallRun.Earth", false);
		config.addDefault("Abilities.Passives.WallRun.Water", false);
		config.addDefault("Abilities.Passives.WallRun.Fire", true);
		config.addDefault("Abilities.Passives.WallRun.Chi", true);
		config.addDefault("Abilities.Passives.WallRun.InvalidBlocks", invalidWallRun);
		
		config.options().copyDefaults(true);
		plugin.saveConfig();
	}

	private void addDeathMessages() {
		FileConfiguration lang = ConfigManager.languageConfig.get();

		//Fire
		lang.addDefault("Abilities.Fire.FireComet.DeathMessage", "{victim} was squashed under the pressure of {attacker}'s {ability}");
		lang.addDefault("Abilities.Fire.FireBall.DeathMessage", "{victim} burst from {attacker}'s {ability}");
		lang.addDefault("Abilities.Fire.FireBreath.DeathMessage", "{victim} was consumed {attacker}'s {ability}");
		lang.addDefault("Abilities.Fire.Discharge.DeathMessage", "{victim} couldn't take {attacker}'s {ability}");
		lang.addDefault("Abilities.Fire.FirePunch.DeathMessage", "{victim} punched out from {attacker}'s {ability}");
		lang.addDefault("Abilities.Fire.FireShots.DeathMessage", "{victim} was shot by {attacker}'s {ability}");
		lang.addDefault("Abilities.Fire.LightningBurst.DeathMessage", "{victim} crackled out of existence from {attacker}'s {ability}");

		//Water
		lang.addDefault("Abilities.Water.Drain.DeathMessage", "{victim} was blasted by {attacker}'s {ability}");
		lang.addDefault("Abilities.Water.FrostBreath.DeathMessage", "{victim} shattered from {attacker}'s {ability}");
		lang.addDefault("Abilities.Water.IceClaws.DeathMessage", "{victim} was ripped to shreds by {attacker}'s {ability}");
		lang.addDefault("Abilities.Water.IceWall.DeathMessage", "{victim} was collateral to {attacker}'s exploding {ability}");
		lang.addDefault("Abilities.Water.WaterBlast.DeathMessage", "{victim} was blasted by {attacker}'s {ability}");
		lang.addDefault("Abilities.Water.Combo.WaterGimbal.DeathMessage", "{victim} was ripped apart by {attacker}'s {ability}");

		//Earth
		lang.addDefault("Abilities.Earth.EarthKick.DeathMessage", "{victim} got too much dirt in their eye from {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.EarthLine.DeathMessage", "{victim} lost their footing from {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.EarthShard.DeathMessage", "{victim} got blasted apart {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.LavaDisc.DeathMessage", "{victim} sliced in half by {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.LavaFlux.DeathMessage", "{victim} couldn't take the heat from {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.LavaThrow.DeathMessage", "{victim} melted from {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.MetalFragments.DeathMessage", "{victim} was shredded apart from {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.MetalShred.DeathMessage", "{victim} was in the way of {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.MudSurge.DeathMessage", "{victim} drowned in mud from {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.SandBlast.DeathMessage", "{victim} was sandblasted to oblivion from {attacker}'s {ability}");
		lang.addDefault("Abilities.Earth.Combo.MagmaBlast.DeathMessage", "{victim} was obliterated by {attacker}'s {ability}");

		//Air
		lang.addDefault("Abilities.Air.AirBlade.DeathMessage", "{victim} was sliced in two by {attacker}'s {ability}");
		lang.addDefault("Abilities.Air.AirPunch.DeathMessage", "{victim} was exploded by {attacker}'s {ability}");
		lang.addDefault("Abilities.Air.SonicBlast.DeathMessage", "{victim}'s ears burst after {attacker}'s {ability}");

		//Chi
		lang.addDefault("Abilities.Chi.DaggerThrow.DeathMessage", "{victim} got stabbed too many times from {attacker}'s {ability}");
		lang.addDefault("Abilities.Chi.Backstab.DeathMessage", "{victim} fell victim to {attacker}'s {ability}");

		//Avatar
		lang.addDefault("Abilities.Avatar.SpiritBeam.DeathMessage", "{victim} was erased from existence by {attacker}'s {ability}");
		lang.addDefault("Abilities.Avatar.ElementSphereAir.DeathMessage", "{victim} was blasted apart by {attacker}'s \u00A75ElementSphere");
		lang.addDefault("Abilities.Avatar.ElementSphereFire.DeathMessage", "{victim} was burnt to cinders by {attacker}'s \u00A75ElementSphere");
		lang.addDefault("Abilities.Avatar.ElementSphereEarth.DeathMessage", "{victim} was crushed by {attacker}'s \u00A75ElementSphere");
		lang.addDefault("Abilities.Avatar.ElementSphereWater.DeathMessage", "{victim} was sliced apart by {attacker}'s \u00A75ElementSphere");
		lang.addDefault("Abilities.Avatar.ElementSphereStream.DeathMessage", "{victim} took the full force of {attacker}'s \u00A75ElementSphere");

		ConfigManager.languageConfig.save();
	}

	private void setupElementSphereNames() {
		FileConfiguration lang = ConfigManager.languageConfig.get();

		lang.addDefault("Abilities.Avatar.ElementSphereAir.Name", "Air");
		lang.addDefault("Abilities.Avatar.ElementSphereFire.Name", "Fire");
		lang.addDefault("Abilities.Avatar.ElementSphereEarth.Name", "Earth");
		lang.addDefault("Abilities.Avatar.ElementSphereWater.Name", "Water");
		lang.addDefault("Abilities.Avatar.ElementSphereStream.Name", "Stream");

		ConfigManager.languageConfig.save();
	}

	public static ConfigurationSection getConfig(Player player) {
		if (player == null)
			return getConfig((World)null);
		return getConfig(player.getWorld());
	}

	public static ConfigurationSection getConfig(World world) {
		boolean perWorldConfig = plugin.getConfig().getBoolean("Properties.PerWorldConfig");

		if (world == null || !perWorldConfig) {
			return plugin.getConfig();
		}

		String prefix = "Worlds." + world.getName();
		return new SubsectionConfigurationDecorator(plugin.getConfig(), prefix);
	}
}
