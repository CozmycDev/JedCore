package com.jedk1.jedcore.ability.waterbending.combo;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.ability.waterbending.WaterBlast;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.CollisionInitializer;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;
import com.projectkorra.projectkorra.waterbending.Torrent;

import com.projectkorra.projectkorra.waterbending.ice.PhaseChange;
import com.projectkorra.projectkorra.waterbending.plant.PlantRegrowth;
import com.projectkorra.projectkorra.waterbending.util.WaterReturn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WaterGimbal extends WaterAbility implements AddonAbility, ComboAbility {

	@Attribute(Attribute.SELECT_RANGE)
	private int sourceRange;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute("Width")
	private double ringSize;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.SPEED)
	private double speed;
	private int animSpeed;
	private boolean plantSourcing;
	private boolean snowSourcing;
	private boolean requireAdjacentPlants;
	private boolean canUseBottle;
	private double abilityCollisionRadius;
	private double entityCollisionRadius;
	
	private int step;
	private boolean initializing;
	private boolean leftVisible = true;
	private boolean rightVisible = true;
	private boolean rightConsumed = false;
	private boolean leftConsumed = false;
	private Block sourceBlock;
	private TempBlock source;
	private Location sourceLoc;
	private Location origin1;
	private Location origin2;
	private boolean usingBottle;
	
	private final Random rand = new Random();

	static {
		CollisionInitializer.abilityMap.put("WaterGimbal", "");
	}

	public WaterGimbal(Player player) {
		super(player);
		if (!bPlayer.canBendIgnoreBinds(this)) {
			return;
		}
		if (JCMethods.isLunarEclipse(player.getWorld())) {
			return;
		}
		if (hasAbility(player, WaterGimbal.class)) {
			return;
		}
		setFields();
		usingBottle = false;
		if (grabSource()) {
			start();
			initializing = true;
			if (hasAbility(player, Torrent.class)) {
				getAbility(player, Torrent.class).remove();
			}
			if (hasAbility(player, WaterManipulation.class)) {
				getAbility(player, WaterManipulation.class).remove();
			}
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		sourceRange = config.getInt("Abilities.Water.WaterCombo.WaterGimbal.SourceRange");
		cooldown = config.getLong("Abilities.Water.WaterCombo.WaterGimbal.Cooldown");
		ringSize = config.getDouble("Abilities.Water.WaterCombo.WaterGimbal.RingSize");
		range = config.getDouble("Abilities.Water.WaterCombo.WaterGimbal.Range");
		damage = config.getDouble("Abilities.Water.WaterCombo.WaterGimbal.Damage");
		speed = config.getDouble("Abilities.Water.WaterCombo.WaterGimbal.Speed");
		animSpeed = config.getInt("Abilities.Water.WaterCombo.WaterGimbal.AnimationSpeed");
		plantSourcing = config.getBoolean("Abilities.Water.WaterCombo.WaterGimbal.PlantSource");
		snowSourcing = config.getBoolean("Abilities.Water.WaterCombo.WaterGimbal.SnowSource");
		requireAdjacentPlants = config.getBoolean("Abilities.Water.WaterCombo.WaterGimbal.RequireAdjacentPlants");
		canUseBottle = config.getBoolean("Abilities.Water.WaterCombo.WaterGimbal.BottleSource");
		abilityCollisionRadius = config.getDouble("Abilities.Water.WaterCombo.WaterGimbal.AbilityCollisionRadius");
		entityCollisionRadius = config.getDouble("Abilities.Water.WaterCombo.WaterGimbal.EntityCollisionRadius");
		
		applyModifiers();
	}

	private void applyModifiers() {
		cooldown -= ((long) getNightFactor(cooldown) - cooldown);
		range = getNightFactor(range);
		damage = getNightFactor(damage);
	}

	@Override
	public void progress() {
		if (player == null || player.isDead() || !player.isOnline() || !player.isSneaking()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreBinds(this) || !bPlayer.canBendIgnoreCooldowns(getAbility("WaterManipulation"))) {
			remove();
			return;
		}
		if (hasAbility(player, WaterManipulation.class)) {
			getAbility(player, WaterManipulation.class).remove();
		}
		if (leftConsumed && rightConsumed) {
			remove();
			return;
		}
		if (!initializing) {
			getGimbalBlocks(player.getLocation());
			if (!leftVisible && !leftConsumed && origin1 != null) {
				if (origin1.getBlockY() <= player.getEyeLocation().getBlockY()) {
					new WaterBlast(player, origin1, range, damage, speed, entityCollisionRadius, abilityCollisionRadius, this);
					leftConsumed = true;
				}
			}

			if (!rightVisible && !rightConsumed && origin2 != null) {
				if (origin2.getBlockY() <= player.getEyeLocation().getBlockY()) {
					new WaterBlast(player, origin2, range, damage, speed, entityCollisionRadius, abilityCollisionRadius, this);
					rightConsumed = true;
				}
			}
		} else {
			Vector direction = GeneralMethods.getDirection(sourceLoc, player.getEyeLocation());
			sourceLoc = sourceLoc.add(direction.multiply(1).normalize());

			if (source == null || !sourceLoc.getBlock().getLocation().equals(source.getLocation())) {
				if (source != null) {
					source.revertBlock();
				}
				if (isTransparent(sourceLoc.getBlock())) {
					source = new TempBlock(sourceLoc.getBlock(), Material.WATER.createBlockData(bd -> ((Levelled) bd).setLevel(0)));
				}
			}

			if (source != null && source.getLocation().distance(player.getLocation()) < 2.5) {
				source.revertBlock();
				initializing = false;
			}
		}
	}

	private boolean grabSource() {
		sourceBlock = BlockSource.getWaterSourceBlock(player, sourceRange, ClickType.SHIFT_DOWN, true, true, plantSourcing, snowSourcing, false);
		if (sourceBlock != null) {
			// All of these extra checks need to be done because PK sourcing system is buggy.
			boolean usingSnow = snowSourcing && (sourceBlock.getType() == Material.SNOW_BLOCK || sourceBlock.getType() == Material.SNOW);

			if (isPlant(sourceBlock) || usingSnow) {
				if (usingSnow || !requireAdjacentPlants || JCMethods.isAdjacentToThreeOrMoreSources(sourceBlock, sourceBlock.getType())) {
					playFocusWaterEffect(sourceBlock);
					sourceLoc = sourceBlock.getLocation();

					new PlantRegrowth(this.player, sourceBlock);
					sourceBlock.setType(Material.AIR);

					return true;
				}
			} else if (!ElementalAbility.isSnow(sourceBlock)) {
				boolean isTempBlock = TempBlock.isTempBlock(sourceBlock);

				if (GeneralMethods.isAdjacentToThreeOrMoreSources(sourceBlock, false) || (isTempBlock && WaterAbility.isBendableWaterTempBlock(sourceBlock))) {
					playFocusWaterEffect(sourceBlock);
					sourceLoc = sourceBlock.getLocation();

					if (isTempBlock) {
						PhaseChange.thaw(sourceBlock);
					}

					return true;
				}
			}
		}

		// Try to use bottles if no source blocks nearby.
		// todo: works the first time, requires actual sources and still consumes water bottle afterwards
		if (canUseBottle && hasWaterBottle(player)){
			Location eye = player.getEyeLocation();
			Location forward = eye.clone().add(eye.getDirection());

			if (isTransparent(eye.getBlock()) && isTransparent(forward.getBlock())) {
				sourceLoc = forward;
				sourceBlock = sourceLoc.getBlock();
				usingBottle = true;
				WaterReturn.emptyWaterBottle(player);
				return true;
			}
		}
		return false;
	}

	// Custom function to see if player has bottle.
	// This is to get around the WaterReturn limitation since OctopusForm will currently be using the bottle.
	private boolean hasWaterBottle(Player player) {
		PlayerInventory inventory = player.getInventory();
		return JedCore.plugin.getPotionEffectAdapter().hasWaterPotion(inventory);
	}

	public static void prepareBlast(Player player) {
		if (hasAbility(player, WaterGimbal.class)) {
			getAbility(player, WaterGimbal.class).prepareBlast();
			if (hasAbility(player, WaterManipulation.class)) {
				getAbility(player, WaterManipulation.class).remove();
			}
		}
	}

	public void prepareBlast() {
		if (leftVisible) {
			leftVisible = false;
			return;
		}
		if (rightVisible) {
			rightVisible = false;
		}
	}

	private void getGimbalBlocks(Location location) {
		List<Block> ring1 = new ArrayList<>();
		List<Block> ring2 = new ArrayList<>();
		Location l = location.clone().add(0, 1, 0);
		int count = 0;

		while (count < animSpeed) {
			boolean completed = false;
			double velocity = 0.15;
			double angle =  3.0 + this.step * velocity;
			double xRotation = Math.PI / 2.82 * 2.1;
			Vector v1 = new Vector(Math.cos(angle), Math.sin(angle), 0.0D).multiply(ringSize);
			Vector v2 = new Vector(Math.cos(angle), Math.sin(angle), 0.0D).multiply(ringSize);
			rotateAroundAxisX(v1, xRotation);
			rotateAroundAxisX(v2, -xRotation);
			rotateAroundAxisY(v1, -((location.getYaw() * Math.PI / 180) - 1.575));
			rotateAroundAxisY(v2, -((location.getYaw() * Math.PI / 180) - 1.575));

			if (!ring1.contains(l.clone().add(v1).getBlock()) && !leftConsumed) {
				completed = true;
				Block block = l.clone().add(v1).getBlock();
				if (isTransparent(block)) {
					ring1.add(block);
				} else {
					for (int i = 0; i < 4; i++) {
						if (isTransparent(block.getRelative(BlockFace.UP, i))) {
							ring1.add(block.getRelative(BlockFace.UP, i));
							break;
						}
					}
				}
			}

			if (!ring2.contains(l.clone().add(v2).getBlock()) && !rightConsumed) {
				completed = true;
				Block block = l.clone().add(v2).getBlock();
				if (isTransparent(block)) {
					ring2.add(block);
				} else {
					for (int i = 0; i < 4; i++) {
						if (isTransparent(block.getRelative(BlockFace.UP, i))) {
							ring2.add(block.getRelative(BlockFace.UP, i));
							break;
						}
					}
				}
			}

			if (completed) {
				count++;
			}

			if (leftConsumed && rightConsumed) {
				break;
			}

			this.step++;
		}

		if (!leftConsumed) {
			if (!ring1.isEmpty()) {
				Collections.reverse(ring1);
				origin1 = ring1.get(0).getLocation();
			}
			for (Block block : ring1) {
				new RegenTempBlock(block, Material.WATER, Material.WATER.createBlockData(bd -> ((Levelled) bd).setLevel(0)), 150L);
				if (rand.nextInt(10) == 0) {
					playWaterbendingSound(block.getLocation());
				}
			}
		}

		if (!rightConsumed) {
			if (!ring2.isEmpty()) {
				Collections.reverse(ring2);
				origin2 = ring2.get(0).getLocation();
			}
			for (Block block : ring2) {
				new RegenTempBlock(block, Material.WATER, Material.WATER.createBlockData(bd -> ((Levelled) bd).setLevel(0)), 150L);
				if (rand.nextInt(10) == 0) {
					playWaterbendingSound(block.getLocation());
				}
			}
		}
	}

	private void rotateAroundAxisX(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double y = v.getY() * cos - v.getZ() * sin;
		double z = v.getY() * sin + v.getZ() * cos;
		v.setY(y).setZ(z);
	}

	private void rotateAroundAxisY(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double x = v.getX() * cos + v.getZ() * sin;
		double z = v.getX() * -sin + v.getZ() * cos;
		v.setX(x).setZ(z);
	}

	@Override
	public void remove() {
		if (source != null) {
			source.revertBlock();
		}
		if (player.isOnline() && !initializing) {
			bPlayer.addCooldown(this);
		}

		if (usingBottle) {
			new WaterReturn(player, sourceBlock);
		}
		super.remove();
	}
	
	public Player getPlayer() {
		return player;
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "WaterGimbal";
	}
	
	@Override
	public boolean isHiddenAbility() {
		return false;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public Object createNewComboInstance(Player player) {
		return new WaterGimbal(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		return ComboUtil.generateCombinationFromList(this, JedCoreConfig.getConfig(player).getStringList("Abilities.Water.WaterCombo.WaterGimbal.Combination"));
	}

	@Override
	public String getInstructions() {
		return JedCoreConfig.getConfig(player).getString("Abilities.Water.WaterCombo.WaterGimbal.Instructions");
	}

	@Override
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
	   return "* JedCore Addon *\n" + config.getString("Abilities.Water.WaterCombo.WaterGimbal.Description");
	}
	
	@Override
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	public int getSourceRange() {
		return sourceRange;
	}

	public void setSourceRange(int sourceRange) {
		this.sourceRange = sourceRange;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public double getRingSize() {
		return ringSize;
	}

	public void setRingSize(double ringSize) {
		this.ringSize = ringSize;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public int getAnimSpeed() {
		return animSpeed;
	}

	public void setAnimSpeed(int animSpeed) {
		this.animSpeed = animSpeed;
	}

	public boolean isPlantSourcing() {
		return plantSourcing;
	}

	public void setPlantSourcing(boolean plantSourcing) {
		this.plantSourcing = plantSourcing;
	}

	public boolean isSnowSourcing() {
		return snowSourcing;
	}

	public void setSnowSourcing(boolean snowSourcing) {
		this.snowSourcing = snowSourcing;
	}

	public boolean isRequireAdjacentPlants() {
		return requireAdjacentPlants;
	}

	public void setRequireAdjacentPlants(boolean requireAdjacentPlants) {
		this.requireAdjacentPlants = requireAdjacentPlants;
	}

	public boolean isCanUseBottle() {
		return canUseBottle;
	}

	public void setCanUseBottle(boolean canUseBottle) {
		this.canUseBottle = canUseBottle;
	}

	public double getAbilityCollisionRadius() {
		return abilityCollisionRadius;
	}

	public void setAbilityCollisionRadius(double abilityCollisionRadius) {
		this.abilityCollisionRadius = abilityCollisionRadius;
	}

	public double getEntityCollisionRadius() {
		return entityCollisionRadius;
	}

	public void setEntityCollisionRadius(double entityCollisionRadius) {
		this.entityCollisionRadius = entityCollisionRadius;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public boolean isInitializing() {
		return initializing;
	}

	public void setInitializing(boolean initializing) {
		this.initializing = initializing;
	}

	public boolean isLeftVisible() {
		return leftVisible;
	}

	public void setLeftVisible(boolean leftVisible) {
		this.leftVisible = leftVisible;
	}

	public boolean isRightVisible() {
		return rightVisible;
	}

	public void setRightVisible(boolean rightVisible) {
		this.rightVisible = rightVisible;
	}

	public boolean isRightConsumed() {
		return rightConsumed;
	}

	public void setRightConsumed(boolean rightConsumed) {
		this.rightConsumed = rightConsumed;
	}

	public boolean isLeftConsumed() {
		return leftConsumed;
	}

	public void setLeftConsumed(boolean leftConsumed) {
		this.leftConsumed = leftConsumed;
	}

	public Block getSourceBlock() {
		return sourceBlock;
	}

	public void setSourceBlock(Block sourceBlock) {
		this.sourceBlock = sourceBlock;
	}

	public TempBlock getSource() {
		return source;
	}

	public void setSource(TempBlock source) {
		this.source = source;
	}

	public Location getSourceLoc() {
		return sourceLoc;
	}

	public void setSourceLoc(Location sourceLoc) {
		this.sourceLoc = sourceLoc;
	}

	public Location getOrigin1() {
		return origin1;
	}

	public void setOrigin1(Location origin1) {
		this.origin1 = origin1;
	}

	public Location getOrigin2() {
		return origin2;
	}

	public void setOrigin2(Location origin2) {
		this.origin2 = origin2;
	}

	public boolean isUsingBottle() {
		return usingBottle;
	}

	public void setUsingBottle(boolean usingBottle) {
		this.usingBottle = usingBottle;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.WaterCombo.WaterGimbal.Enabled");
	}
}
