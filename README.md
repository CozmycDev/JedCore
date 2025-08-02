[![GitHub Pre-Release](https://img.shields.io/github/release-pre/Hihelloy-main/JedCore.svg)](https://github.com/CozmycDev/JedCore/releases)
[![Github All Releases](https://img.shields.io/github/downloads/Hihelloy-main/JedCore/total.svg)](https://github.com/CozmycDev/JedCore/releases)
![Size](https://img.shields.io/github/repo-size/Hihelloy-main/JedCore.svg)

# JedCore
This is my fork of Cozymc's fork of plushmonkey's fork of jedk1's JedCore addon for ProjectKorra.  
Download releases [here](https://github.com/Hihelloy-main/JedCore/releases).
Join our [Discord]() to discuss the plugin, suggest changes, report bugs, etc (new Discord soon).

## Changelog

### 2.14.4
- Adds light to all fire abilities, SpiritBeam, ElementSphere, and HealingWaters.
  - `Properties.Fire.DynamicLight.Enabled`: Whether to display light for Fire, SpiritBeam, or ElementSphere.
  - `Properties.Fire.DynamicLight.Brightness`: A number 1-15 where higher is brighter.
  - `Properties.Fire.DynamicLight.KeepAlive`: Time in ms lights persist until they fade out.
  - `Abilities.Water.HealingWaters.DynamicLight.Enabled`
  - `Abilities.Water.HealingWaters.DynamicLight.Brightness`
  - `Abilities.Water.HealingWaters.DynamicLight.KeepAlive`
- Adds a travel speed option to Combustion, so range no longer affects the speed of the ability. Thank you for your report @BlackXLight!
  - `Abilities.Fire.Combustion.Speed`: The travel speed in blocks per tick.
- EarthLine sourcing is now spot-on, making it much more enjoyable.

### 2.14.3
- Fixes a critical issue with EarthLine. Please update to this version!
- EarthLine sourcing is now about as accurate as Core EarthBlast.
- Adds Cozmyc to the JedCore dev list and as the current maintainer.

### 2.14.2
- Reintroduces backwards support for MC <1.20.2+ and PK 1.11.1+
- Still works on 1.20.5+ too!

### 2.14.1
- For the one ProjectKorra server running 1.20.5!
- Still works on 1.20.6+, nothing else changed since 2.14.0 released less than an hour ago.

### 2.14.0
- Only works on MC 1.20.6+ and PK 1.11.3+
- Fixes particle effects causing errors on 1.20.6+

### 2.13.2
- Fixed Drain not activating and messily leaving water blocks behind

### 2.13.1
- Fixed board command overriding PK's when our board is disabled

### 2.13.0
- Added support for Spigot 1.16 (Dreig-Michihi)
- Fixed ElementSphere Water crash
- Added water bubbles to ElementSphere Water underwater
- Added FireShots hand swapping by tapping sneak (Dreig-Michihi)
- Added configurable pointer for bending board, with "> " as default, to fix flickering (Dreig-Michihi)
- Fixed MetalFragments hoppers dupe (Dreig-Michihi)
- Added item crack particles to MetalFragments (Dreig-Michihi)
- Optimized MetalFragments (Dreig-Michihi)
- Fixed error resulting from ambiguous ability names like EarthKick that are shared with other popular addons (Dreig-Michihi)
- Optimized extinguishing for AirBreath (Dreig-Michihi)
- Fixed bug where Combustion couldn't start when a previous Combustion was still reverting (Dreig-Michihi)
- Improved Combustion charge-up and cooldown add times (Dreig-Michihi)
- Fixed FirePunch allowing multiple instances (Dreig-Michihi)
- Fixed Fissure reversion (StrangeOne101)
- Fixed lava immunity bug (StrangeOne101)
- Fixed AirSlam using SwiftStream's instructions (MKieburtz)

### 2.12.1
- Fixed URL being outdated in /b version
- Fixed IceWall not working on snow and ice blocks from the config
- Fixed WaterFlow using the wrong instructions
- Fixed LavaFlux making permanent stone when duration is less than cleanup
- Fixed MudSurge duping the center block
- Fixed MudSurge creating the blocks offset of 0.5 blocks
- Added new supported blocks to MudSurge
- Made MudSurge's brown blocks appear in a random order
- Fixed ElementSphere Fire crash

### 2.12.0
- Added death messages for all abilities
- Made all combos configurable with configurable instructions as well
- Updated MagnetShield metal list
- Fixed bending board not supporting the easy hex color format
- Fixed LavaFlux making permanent stone, duplicating earth block items, and destroying plants
- Fixed LavaFlux not working outside of old world height bounds
- Fixed Backstab not dealing "bending" damage
- Fixed rainbow FireBreath message
- Fixed AirBreath not extinguishing soul fire
- Fixed some newer signs not being supported in sign checking
- Fixed NPE in ESEarth#getLocation
- Added Aztl to the JedCore dev list
- Removed pointless imports
- Replaced a lot of deprecated methods with the non-deprecated versions
- Optimized a few minor things


### 2.11.0
- Updated ProjectKorra dependency to 1.10.2
- Converted to PK's TempFallingBlock class
- Removed CooldownEnforcer, rendered unnecessary by recent PK changes
- Updated FrostBreath to use temperature and humidity in order to work in custom biomes
- Fixed error relating to switching worlds while damaging an entity

### 2.10.0
- Used World#getMinHeight and World#getMaxHeight instead of hard-coded Y-values for certain abilities' cancellations (for 1.18 world height changes)
- Added getters and setters to almost all abilities for improved data accessibility
- Fixed many warnings and instances of poor style
- Made almost all abilities adhere to standards concerning the possible cancellation of the AbilityStartEvent, so no irreversible changes take place if the event is cancelled
- Converted all instances of Entity#setVelocity to the new GeneralMethods#setVelocity, which calls its own event
- Removed unnecessary "time" variables that can be replaced with getStartTime() calls
- Made FirePunch not static

### 2.9.6.2
- Started using jitpack for ProjectKorra dependency

### 2.9.6.1
- Fixed bug where many combo abilities did not display in /b display

### 2.9.6
- Added blue fire to ElementSphere Stream
- Added Attribute annotations
- Added hit indicator sounds to DaggerThrow
- Added new biomes to FrostBreath and Bloodbending
- Added new plants and blocks to the necessary lists
- Fixed deprecated particle names
- Fixed a jukebox dupe bug
- Improved blue fire particle logic

### 2.9.5
- Added modifiers for blue fire, day factor, and night factor where applicable
- Added new nether biomes for FrostBreath and Drain's biome requirement list
- Changed version number to 2.9.5

### 2.9.5-alpha.1
- Added Blue Fire support for all fire abilities
- Changed WaterGimbal combo abilities to Torrent > Torrent > WaterManipulation
- Changed Maelstrom combo abilities to PhaseChange > Torrent > Torrent

### 2.9.3
- Fix a crash with Bloodpuppet and Bloodbending when IgnoreWalls is enabled.
  
### 2.9.2
- Fix LavaThrow blast direction. (Simplicitee)
- Fix no-volume AABB queries.
- Use FlightHandler with FireSki and EarthSurf to hopefully reduce the frequency of flight bugs.
- Improve MetalHook.
  - Fix a bug with MetalHook where the player wouldn't be pulled toward an attached hook.
  - Add config option to MetalHook for attaching to barriers.
- Fix EarthKick bugs.
  - Allow slot swapping once EarthKick is created. This fixes the bug where the falling blocks don't damage enemies if the player swapped slots before they get near an enemy.
  - Fix an issue with EarthKick where the wrong FallingBlocks could be removed from the tracking list.
  
### 2.9.1
- Fix AABB code so it works on Spigot 1.15.

### 2.9.0
- Add support for Spigot 1.14.4/ProjectKorra 1.8.9. It should continue to work with Spigot 1.13.2/ProjectKorra 1.8.8, but it hasn't been tested.
- Fix EarthLine render issue that has existed for a long time where the falling blocks aren't spawned at the center of the block.
- Improve EarthLine climbing/falling.

### 2.8.1
- Refactor FrostBreath code.
  - Use waterbendable TempBlocks for the created snow. This should fix some reversion issues that could cause permanent blocks.
  - Add a duration to the ice created from water.
  - Prevent PlantRegrowth from creating permanent snow.
- Add "-small-" or "-large-" to a collision list for the ability to be added to the collision initializer lists.
- Add invincible check for AirSlam. (PhanaticD)
- Remove Java 8 and Spigot checks. (PhanaticD)
- Probably fix ElementSphere fly glitch. (PhanaticD)
- Fix EarthShard concrete powder interaction. (PhanaticD)
- Fix some Bloodbending issues. (PhanaticD)
  - Attempt a fix for Bloodbending finite error. (PhanaticD)
  - Fix issue with Bloodbending returning a bad vector when inside a block. (PhanaticD)
  - Make Bloodbending ignore armor stands. (PhanaticD)
- Add missing plants to the small plants list. (PhanaticD)
- Allow Drain to use configured plants. (PhanaticD)
- Fix memory leak issue with the bending board. (PhanaticD)
- Improve EarthKick. It now works on RaiseEarth. (PhanaticD)
- Fix LavaThrow going through walls. (PhanaticD)
- Fix up MetalFragments. (PhanaticD)

### 2.8.0
- Update to ProjectKorra 1.8.8 and Spigot 1.13.2.
  - Support dropped for older versions of both ProjectKorra and Spigot due to large api changes.
- Fix a bunch of protection issues, such as WorldGuard item dropping. (PhanaticD)
- Fix Discharge and LightningBurst being spammable in AvatarState. (PhanaticD)
- Fix DaggerThrow so it cannot damage the user. (PhanaticD)
- Make bending board more responsive. (PhanaticD)
- Fix FirePunch so it's only activated from melee damage.
- Improve EarthLine. (0ct0ber)
  - Add more config options for dealing with select/prepare ranges.
  - Add config option for allowing direction change.
  - Add config option for max duration.
- Make slot swapping configurable for EarthLine and LavaDisc.
  
### 2.7.0
- Update to ProjectKorra 1.8.7. It should continue to work with previous versions as well.
- Improve MetalArmor and MetalFragments.
  - Fix MetalArmor so it properly detects if the player sourced metal blocks.
  - Add config option to MetalArmor for setting the duration of the resistance.
  - Fix MetalArmor so it actually reads the config values.
  - Fix NullPointerException with MetalArmor.
  - Fix MetalFragments so it aligns the falling block with the actual block. This fixes the bug where it would always drop the block into an item.
  - Fix NullPointerException with MetalFragments.
- Improve MetalHook.
  - Fix MetalHook cooldown activation.
  - Add configuration option for setting total hook count for MetalHook. This can be used to set a concrete action that ends MetalHook and forces it to go on cooldown. Set it to 0 to disable it.
  - Improve MetalHook so it can be activated while sprinting. Toggling sprint off and on will end the ability still.
- Improve FireComet region checks.
  - Add a region protection check to FireComet to prevent players from walking into protected regions while it's charged.
  - Add a region protection check to FireComet to prevent it from damaging players in protected areas.
- Add configuration option to not Drain TempBlocks. It could be used to create flowing water.
- Improve bending board.
  - When boards are disabled or if a player toggles their board off, set their scoreboard back to the main server-controlled scoreboard. This allows players to be found on teams when their board is disabled.
  - Fix an exception that occurs when bending board is disabled.
- Stop Backstab from activating out of melee range.
- Fix Combustion blowing up burning furnaces.
- Fix container duplication exploit.
- Handle offhand while removing items from inventory.
  - This fixes a bug where DaggerThrow could be used without consuming arrows.
  
### 2.6.5
- Fix version check so it works with Java 9.
- Significantly improve some collision checks.
- Use AABB/Sphere collision tests for AirPunch, Combustion, FireBall, and FireShots.
- Make ability and entity collision radii configurable for AirBlade, AirPunch, SonicBlast, Combustion, Discharge, and EarthKick.
- Make ability collision radius configurable for DaggerThrow.
- Improve AirBlade.
  - Improve AirBlade ability collisions by properly implementing getLocations and getCollisionRadius.
  - Change AirBlade to use Sphere-AABB entity collision detection. This allows it to hit the player's head when aimed above.
- Change SonicBlast to use Sphere-AABB entity collision detection.
- Improve Discharge.
  - Change Discharge to use Sphere-AABB entity collision detection.
  - Fix Discharge ability-ability collision detection by properly implementing getLocations and getCollisionRadius.
  - Make duration and slot swapping configurable for Discharge.
- Improve EarthKick.
  - Change EarthKick to using AABB-AABB entity collision detection.
  - Update EarthKick to use the new collision system. Everything is configurable under Abilities.Earth.EarthKick.Collisions. All of the old hardcoded collisions are set by default.
- Fix EarthShard and MudSurge collisions.
  - Check all falling blocks when these abilities collide. The collision system only detects a single collision from the location set every update, so they all must be checked. This fixes collisions with shields.
- Fix collision initialization for SubElement abilities.
- Add config option to EarthSurf and WaterFlow for removing them when any damage is taken instead of dropping below initial health.
- Fix FrostBreath lava interaction.
  - Don't create non-TempBlock snow layer when the block is a lava TempBlock. This fixes an interaction with LavaFlow where it could create a giant permanent hole in the ground.
- Improve WaterGimbal.
  - Improve WaterGimbal so the speed can be a double.
  - Fix WaterGimbal blasts so they properly end when colliding with blocks.
  - Change WaterGimbal blasts so they use AABB-AABB entity collision.
  - Implement getLocation and getCollisionRadius for WaterGimbal blasts. This fixes ability collisions.
  - Remove WaterBlast enabled configuration and set it to be a hidden ability.
  - Fix WaterGimbal collision initializer so it properly attaches collisions to WaterBlast.
- Make FireSki more configurable
  - Add configuration option for changing required height for activation.
  - Add configuration option for changing the activation to require punching. This makes it so FireSki only activates when the player jumps in the air and punches while also sneaking. This is to make it harder to accidentally activate.
- Fix CollisionInitializer so it properly loads element combo collisions.
- Fix CollisionInitializer so it doesn't override disabled collisions.
- Clean up log messages
  - Add configuration option for logging debug messages.
  - Hide collision initialization and fire tick method by default.
  - Remove the updater log output.

### 2.6.4
- Fix bending board toggle.
  - Ensure that the bending board isn't redrawn after being hidden.
  - Ignore FastSwim cooldown events because it constantly spams them.
- Refactor and improve LavaDisc.
  - Add config option for blocking lava spread from destroyed blocks. This will prevent players from creating large lava fountains.
  - Add config option for selecting source range.
  - Change config location for several options. THey are moved under Source or Destroy depending on what they are used for.
- Improve DaggerThrow by adding a config option for requiring multiple arrows to hit to activate DaggerThrow interactions.
- Improve MudSurge by adding a config option for earthbenders taking fall damage on MudSurge source blocks.
- Add CooldownEnforcer utility.
  - Copies over cooldowns from a player's previous session. This stops players from logging off to reset their cooldowns.
  - The cooldowns continue ticking down when the player is offline.
  - Add config option to enable this at Properties.CooldownEnforcer.Enabled. It is disabled by default.
  - Add config option for enabling it on plugin reload. This is useful to disable when needing to reset a cooldown to test it out.

### 2.6.3
- Should work with ProjectKorra 1.8.3 - 1.8.6.
- Should work with Spigot 1.9.4 - 1.12.2.
- Fix MetalFragments bypassing edge of protections. (PhanaticD)
- Improve DaggerThrow.
  - Add configuration section to DaggerThrow for destroying abilities when the target player is hit by an arrow. This is configurable under Abilities.Chi.DaggerThrow.Interactions.
  - Make DaggerThrow knock players off spouts. This is configurable under Abilities.Chi.DaggerThrow.Interactions. (StrangeOne101)
  - Fix a bug where DaggerThrow would automatically shoot an arrow when the target takes damage.
- Add per world configurations.
  - All ability configuration is first checked under the section Worlds.[worldName] before checking the global config section. It falls back to the global config if that section isn't defined.
    - Example: Worlds.world.Abilities.Earth.EarthSurf.Speed can be set to 1.0 and it will be 1.0 in the world "world", but it will be whatever Abilities.Earth.EarthSurf.Speed is set to in other worlds.
- Improve MudSurge.
  - Improve MudSurge by adding OutOfRange policy. This makes it so the ability is removed if the player moves too far away from the source block before surging. This is configurable.
  - Improve MudSurge by adding SwappedSlots policy. This makes it so the ability is removed if the player changes selected ability before surging. This is configurable.
- Improve Combustion.
  - Add config option for waiting for regen to finish. Setting this to false makes Combustion follow the cooldown instead of requiring the player to wait until regen is finished. This is set to true by default to keep it the way it was before.
  - Add removal policies. Add SwappedSlots removal policy. This makes it so you can't swap slots while charging. This is disabled by default to keep it the way it was before.
  - Add ExplodeOnDeath config option to Combustion. This causes the projectile to explode if the player controlling it dies. If this is disabled, it has old behavior where it will just disappear.
  - Fix bug where the projectile wouldn't collide with protected terrain.
  - Fix bug where the explosion would damage players in protected areas.
  - Fix bug where players would have to wait for RegenTime even if no blocks were destroyed.
  - Fix misfire calculation. Now properly reads the MisfireModifier.
- Add ChiRestrictor. This tries to stop chi abilities from being activated from ranged attacks. Only works on Chi abilities that have a public method with the signature "Entity getTarget()". This should work with most of the core Chi attacks.
  - Add config option for enabling ChiRestrictor. It is disabled by default.
  - Add config option for a whitelist. This makes it ignore certain Chi attacks. This is a comma separated array of ability names.
  - Add config option for resetting the cooldown. The PK Chi abilities will set the cooldown even if the AbilityStartEvent is cancelled. Setting this to true will reset the cooldown, so it will seem like nothing happened at all.
  - Add config option for melee distance. The ChiRestrictor will stop any Chi attacks that are activated farther away than this distance.
  
### 2.6.2
- Fix Discharge permission spelling. This will enable Discharge with the jedcore.fire permission node.
- Fix ElementSphere so it works for non-opped players. (PhanaticD)
- Fix exception with Bloodbending and BloodPuppet. (PhanaticD)

### 2.6.1
- Add support for ProjectKorra 1.8.6.
  - Continues to work with 1.8.5, 1.8.4 and 1.8.3.
- Continues to work with 1.9.4 - 1.12.
  - ProjectKorra 1.8.6 requires 1.10 to work.

### 2.6.0
- Add support for ProjectKorra 1.8.5.
  - Continues to work with 1.8.4 and 1.8.3.
- Add support for Spigot 1.12.1.
  - Continues to work with 1.9.4 - 1.12.
  - ProjectKorra 1.8.5 doesn't support 1.12.1, but a custom build might.
- Improve FireSki activation.
  - Use the new collision methods for detecting if player is on the ground before activating. This fixes the bug where FireSki would activate when standing on the edge of a block.
- Add option to start AirGlide cd when on ground .
  - Add config option Abilities.Air.AirGlide.RequireGround that makes it so AirGlide will continuously reset the cooldown until the user touches the ground. This is disabled by default.
  
### 2.5.6
- Improve EarthSurf
  - Add a config option for scaling the cooldown based on the duration.
  - Add a config option for a minimum cooldown when using scaling.
  - Add a config option for using relaxed collision detection. This will just check the block directly in front of the player instead of a column.
  - Use spring equation to keep the player at the target height. This is configurable with SpringStiffness.
  - Disable reverting allowFlight because it almost always resulted in giving the player real flight.
  - Improve the method for getting distance from ground by storing the temp air blocks and counting them as solid blocks.
- Fix EarthSurf lag 
  - Set TempFallingBlocks to expire when created from EarthSurf. This fixes the bug where they would be tracked in the instances list forever, causing a lot of lag.
  - Use MaterialUtil#isTransparent to avoid unnecessary region protection checks.
- Make EarthSurf height check more tolerant 
  - Add a height tolerance config value. This is used to smooth the player's height for the max height check. Larger values will allow the player to climb down hills without removing the ability.
  
### 2.5.5
- Improve MagmaBlast
  - Properly load the configuration values.
  - Only start cooldown if sources were successfully raised.
  - Improve the way random source blocks are selected. Max source blocks will always be selected if they are available.
  - Only use source blocks if they have the space to fly upwards.
  - Add configuration option for requiring LavaFlow to not be on cooldown.
  - Remove TempFallingBlocks when they are no longer active. This fixes a bug where MagmaBlast instance would not end until swapping slots.
  - Fix a bug where it would try to damage any entity instead of only LivingEntities.
- Make FrostBreath ice meltable 
  - Use Torrent as the container for the ice blocks. This will make it so the ice created from FrostBreath properly melts when the player moves far enough away from it. It also fixes the bug where HeatControl couldn't melt it.
- Improve WaterGimbal and WaterFlow sourcing 
  - Change WaterGimbal and WaterFlow so they can source from water bendable TempBlocks.
  - Add config option to WaterFlow for sourcing from plants.
  - Add config option to WaterGimbal for requiring adjacent sources. This is enabled by default to keep it the same as it was.
  - Add config option to both WaterFlow and WaterGimbal for using water bottles.
- Fix bending board lag
  - There's a bug with ProjectKorra 1.8.4 that never kills IceWave abiilty, so it spams cooldown event. This gets worse every time someone uses IceWave, and lasts until server restarts. Add a catch to the PlayerCooldownChangeEvent handler that ignores IceWave. This will fix bending board lag.

### 2.5.4
- Improve performance of WaterFlow
  - Use a custom isTransparent method that doesn't do a region protection check. The default pk version does the check with "WaterFlow" as the ability, but WaterFlow uses "Torrent". This results in a cache miss, doubling the amount of work that needs to be done.
- Change WaterGimbal so it can source from water bendable TempBlocks.

### 2.5.3
- Track hidden cooldowns on bending board.
  - The list of abilities to watch can be configured in boards.yml.
- Improve Backstab.
  - Change the implementation so it uses dot product instead of cardinal directions. This will make it more accurate.
  - Add configuration option for selecting the max activation angle. Configurable with Abilities.Chi.Backstab.MaxActivationAngle.
  
### 2.5.2
- Change FireShots so each instance tracks its own shots instead of a
  global list.
- Fix a bug where the last FireShot fired wouldn't collide.
- Change FireShots and FireBall to use the collision system when colliding
  with shields.
- Add a utility class for fire tick. This makes fire tick overriding
  configurable. This will default to only setting fire tick if the new
  amount is larger than the entity already has.
- Change all of the fire abilities that use fire tick to use the new
  utility class.
- Change default config so ability collisions are enabled by default.

### 2.5.1
- Add special case for FireBlast when initializing collisions.
- Add some FireBlastCharged examples to default config.

### 2.5.0
- JedCore 2.5.0 for ProjectKorra 1.8.3 and 1.8.4.
- Add ability collisions.
  - Add global configuration option to disable all JedCore ability collisions. They are disabled by default.
  - Collisions are configurable under Abilities.{element}.{abilityname}.Collisions.{abilityname}.
    - Some default examples were added to AirBlade and AirPunch.
  - This hasn't been tested with every ability, so it might not work with all of them.
  
### 2.4.3
- JedCore for PK 1.8.4.
- Fix Crevice bugs
  - Properly revert when used at low/high y levels.
  - Switch to using TempBlocks so no permanent changes are made if the server shuts down or plugin is reloaded.
- Read EarthLine, MetalFragments, and IceWall damage as double.
  
### 2.3.0
- Fork the original code base.
- Make LavaThrow fire ticks configurable.
- Fix Backstab NPE.
- Make FireSki fire ticks configurable.
- Improve SonicBlast.
  - Remove the ability after it does damage.
  - Add configuration option for allowing slot swapping while charging.
- Make AirGlide configurable.
  - Make cooldown configurable.
  - Make duration configurable.
- Improve MudSurge.
  - Change default damage to 1.
  - Make blind ticks configurable.
  - Add configuration option for having MudSurge hit an entity multiple times.
  - Change it so the cooldown starts on launch rather than on sourcing.
- Improve FireBall and FireShots.
  - Add configuration option for shield collisions for both FireBall and FireShots.
  - Reflect both of the abilities off of AirShield when ShieldCollisions is enabled.
  - Destroy both of the abilities in FireShield when ShieldCollisions is enabled.
  - Make FireBall blaze trail configurable.
  - Change FireBall blaze trail so it lags behind the attack.
- Revert passive sand blocks before using them. This stops abilities from turning the blocks into permanent sand.
- Make EarthSurf usable on transparent blocks.
- Improve EarthShard.
  - Prevent selecting positions that already have EarthShard TempBlock. This fixes the bug where the FallingBlock can't reach its destination, putting the instance into a broken state.
  - Fix a bug where damage wouldn't happen when swapping slots after launching.
  - Disable the destruction of the current instance when selecting an air block.
  - Read the damage as a double.
- Fix bending board combos.
  - Register the combos with the bending board after a delay on startup. This ensures all addons have registered their combos.
- Fix SandBlast.
  - Fix SandBlast so it doesn't get stuck in a broken state.
  - Fix SandBlast so it actually affects entities.
  - Change SandBlast so it reverts passive sand.
- Fix bending board ability name length
  - Remove the code that added multiple reset codes. This could make the
  board ability names so long that they would kick players offline.
- Fix IceWall so old instances don't block new ones 

