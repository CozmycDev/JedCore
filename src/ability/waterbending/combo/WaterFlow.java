package com.jedk1.jedcore.ability.waterbending.combo;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.MaterialUtil;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.airbending.AirSpout;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.Catapult;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;
import com.projectkorra.projectkorra.waterbending.WaterSpout;

import com.projectkorra.projectkorra.waterbending.plant.PlantRegrowth;
import com.projectkorra.projectkorra.waterbending.util.WaterReturn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class WaterFlow extends WaterAbility implements AddonAbility, ComboAbility {

	@Attribute(Attribute.SELECT_RANGE)
	private int sourceRange; //10
	@Attribute(Attribute.RANGE)
	private int maxRange; //40
	private int minRange; //8
	@Attribute(Attribute.DURATION)
	private long duration; //10000
	@Attribute(Attribute.COOLDOWN)
	private long cooldown; //15000
	private long meltDelay; //5000
	@Attribute("Length")
	private long trail; //80
	private boolean avatar; //true
	private boolean stayAtSource; //true
	private int stayRange; //100
	private boolean fullMoonEnabled;
	private int fullMoonCooldown;
	private int fullMoonDuration;
	private boolean playerRideOwnFlow;
	private int size; //1;
	private int avatarSize; //3;
	private int fullMoonSizeSmall; //2;
	private int fullMoonSizeLarge; //3;
	private long avatarDuration; //60000;
	private boolean canUseBottle;
	private boolean canUsePlants;
	private boolean removeOnAnyDamage;
	
	private long time;
	private Location origin;
	private Location head;
	private int range;
	private Block sourceBlock;
	private boolean frozen;
	private double prevHealth;
	private int headSize;
	private boolean usingBottle;
	private final ConcurrentHashMap<Block, Location> directions = new ConcurrentHashMap<>();
	private final List<Block> blocks = new ArrayList<>();

	Random rand = new Random();

	public WaterFlow(Player player) {
		super(player);
		if (!bPlayer.canBendIgnoreBinds(this)) {
			return;
		}
		if (JCMethods.isLunarEclipse(player.getWorld())) {
			return;
		}
		if (hasAbility(player, WaterFlow.class)) {
			getAbility(player, WaterFlow.class).remove();
			return;
		}
		setFields();

		usingBottle = false;

		if (prepare()) {
			headSize = size;
			trail = trail * size;
			range = maxRange;
			prevHealth = player.getHealth();
			time = System.currentTimeMillis();

			int augment = (int) Math.round(getNightFactor(player.getWorld()));
			if (isFullMoon(player.getWorld()) && fullMoonEnabled && sourceBlock != null) {
				List<Block> sources = getNearbySources(sourceBlock, 3);
				if (sources.size() > 9) {
					headSize = fullMoonSizeSmall;
				}
				if (sources.size() > 36) {
					headSize = fullMoonSizeLarge;
				}
				trail = trail * augment;
				range = range - (range / 3);
				maxRange = range;
				duration = duration * fullMoonDuration;
				cooldown = cooldown * fullMoonCooldown;
			}
			if (bPlayer.isAvatarState()) {
				headSize = avatarSize;
				if (avatar) {
					duration = 0;
				} else {
					duration = avatarDuration;
				}
			}
			start();
			if (hasAbility(player, WaterManipulation.class)) {
				WaterManipulation manip = getAbility(player, WaterManipulation.class);
				manip.remove();
			}
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		sourceRange = config.getInt("Abilities.Water.WaterCombo.WaterFlow.SourceRange");
		maxRange = config.getInt("Abilities.Water.WaterCombo.WaterFlow.MaxRange");
		minRange = config.getInt("Abilities.Water.WaterCombo.WaterFlow.MinRange");
		duration = config.getLong("Abilities.Water.WaterCombo.WaterFlow.Duration");
		cooldown = config.getInt("Abilities.Water.WaterCombo.WaterFlow.Cooldown");
		meltDelay = config.getInt("Abilities.Water.WaterCombo.WaterFlow.MeltDelay");
		trail = config.getInt("Abilities.Water.WaterCombo.WaterFlow.Trail");
		avatar = config.getBoolean("Abilities.Water.WaterCombo.WaterFlow.IsAvatarStateToggle");
		avatarDuration = config.getLong("Abilities.Water.WaterCombo.WaterFlow.AvatarStateDuration");
		stayAtSource = config.getBoolean("Abilities.Water.WaterCombo.WaterFlow.PlayerStayNearSource");
		stayRange = config.getInt("Abilities.Water.WaterCombo.WaterFlow.MaxDistanceFromSource");
		canUseBottle = config.getBoolean("Abilities.Water.WaterCombo.WaterFlow.BottleSource");
		canUsePlants = config.getBoolean("Abilities.Water.WaterCombo.WaterFlow.PlantSource");
		removeOnAnyDamage = config.getBoolean("Abilities.Water.WaterCombo.WaterFlow.RemoveOnAnyDamage");
		fullMoonCooldown = config.getInt("Abilities.Water.WaterCombo.WaterFlow.FullMoon.Modifier.Cooldown");
		fullMoonDuration = config.getInt("Abilities.Water.WaterCombo.WaterFlow.FullMoon.Modifier.Duration");
		fullMoonEnabled = config.getBoolean("Abilities.Water.WaterCombo.WaterFlow.FullMoon.Enabled");
		playerRideOwnFlow = config.getBoolean("Abilities.Water.WaterCombo.WaterFlow.PlayerRideOwnFlow");
		size = config.getInt("Abilities.Water.WaterCombo.WaterFlow.Size.Normal");
		avatarSize = config.getInt("Abilities.Water.WaterCombo.WaterFlow.Size.AvatarState");
		fullMoonSizeSmall = config.getInt("Abilities.Water.WaterCombo.WaterFlow.Size.FullmoonSmall");
		fullMoonSizeLarge = config.getInt("Abilities.Water.WaterCombo.WaterFlow.Size.FullmoonLarge");
		
		applyModifiers();
	}

	private void applyModifiers() {
		if (isNight(player.getWorld())) {
			maxRange = (int) getNightFactor(maxRange);
			cooldown -= ((long) getNightFactor(cooldown) - cooldown);
		}
	}
	
	public static List<Block> getNearbySources(Block block, int searchRange) {
		List<Block> sources = new ArrayList<>();
		for (Location l : GeneralMethods.getCircle(block.getLocation(), searchRange, 2, false, false, -1)) {
			Block blockI = l.getBlock();
			if (isWater(block)) {
				if (blockI.getType() == Material.WATER && JCMethods.isLiquidSource(blockI) && WaterManipulation.canPhysicsChange(blockI)) {
					sources.add(blockI);
				}
			}
			if (isLava(block)) {
				if (blockI.getType() == Material.LAVA && JCMethods.isLiquidSource(blockI) && WaterManipulation.canPhysicsChange(blockI)) {
					sources.add(blockI);
				}
			}
		}
		return sources;
	}

	private boolean prepare() {
		sourceBlock = BlockSource.getWaterSourceBlock(player, sourceRange, ClickType.SHIFT_DOWN, true, bPlayer.canIcebend(), canUsePlants);
		if (sourceBlock != null) {
			boolean isGoodSource = GeneralMethods.isAdjacentToThreeOrMoreSources(sourceBlock, false) || (TempBlock.isTempBlock(sourceBlock) && WaterAbility.isBendableWaterTempBlock(sourceBlock));

			// canUsePlants needs to be checked here due to a bug with PK dynamic source caching.
			// getWaterSourceBlock can return a plant even if canUsePlants is passed as false.
			if (isGoodSource || (canUsePlants && isPlant(sourceBlock))) {
				head = sourceBlock.getLocation().clone();
				origin = sourceBlock.getLocation().clone();
				if (isPlant(sourceBlock)) {
					new PlantRegrowth(player, sourceBlock);
				}
				return true;
			}
		}

		if (canUseBottle && WaterReturn.hasWaterBottle(player)){
			Location eye = player.getEyeLocation();
			Location forward = eye.clone().add(eye.getDirection());

			if (isTransparent(eye.getBlock()) && isTransparent(forward.getBlock())) {
				head = forward.clone();
				origin = forward.clone();
				usingBottle = true;
				WaterReturn.emptyWaterBottle(player);
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreBinds(this) || !bPlayer.canBendIgnoreCooldowns(getAbility("WaterManipulation"))) {
			remove();
			return;
		}
		if (duration > 0 && System.currentTimeMillis() > time + duration) {
			remove();
			return;
		}
		if ((stayAtSource && player.getLocation().distance(origin) >= stayRange) || head.getY() > head.getWorld().getMaxHeight() || head.getY() < head.getWorld().getMinHeight()) {
			remove();
			return;
		}
		if (RegionProtection.isRegionProtected(player, head, this)) {
			remove();
			return;
		}
		if (AirAbility.isWithinAirShield(head)) {
			remove();
			return;
		}
		if (prevHealth > player.getHealth()) {
			remove();
			return;
		}

		if (removeOnAnyDamage) {
			// Only update the previous health if any damage should remove it.
			prevHealth = player.getHealth();
		}

		if (!frozen) {
			if (player.isSneaking()) {
				if (range >= minRange) {
					range -= 2;
				}
				//BlockSource.update(player, sourceRange, ClickType.RIGHT_CLICK);
			} else {
				if (range < maxRange) {
					range += 2;
				}
			}
			moveWater();
			//updateBlocks();
			manageLength();
		}
	}

	private void manageLength() {
		int pos = 0;
		int ids = 0;
		List<Block> tempList = new ArrayList<>(blocks);
		for (Block block : tempList) {

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(block.getLocation(), 2.8)) {
				if (entity.getEntityId() == player.getEntityId() && !playerRideOwnFlow) {
					continue;
				}
				boolean isPlayer = entity instanceof Player;
				if (isPlayer) {
					if (getPlayers(AirSpout.class).contains(entity)) {
						continue;
					} else if (getPlayers(WaterSpout.class).contains(entity)) {
						continue;
					} else if (getPlayers(Catapult.class).contains(entity)) {
						continue;
					}
				}
				if (RegionProtection.isRegionProtected(this, entity.getLocation()) || (isPlayer && Commands.invincible.contains(entity.getName()))) {
					continue;
				}
				Location temp = directions.get(block);
				Vector dir = GeneralMethods.getDirection(entity.getLocation(), directions.get(block).add(temp.getDirection().multiply(1.5)));
				GeneralMethods.setVelocity(this, entity, dir.clone().normalize().multiply(1));
				entity.setFallDistance(0f);
			}

			if (!MaterialUtil.isTransparent(block) || RegionProtection.isRegionProtected(player, block.getLocation(), "Torrent")) {
				blocks.remove(block);
				directions.remove(block);
				if (TempBlock.isTempBlock(block)) {
					TempBlock.revertBlock(block, Material.AIR);
				}
			} else {
				if (!isWater(block)) {
					new TempBlock(block, Material.WATER.createBlockData(bd -> ((Levelled) bd).setLevel(0)));
				}
			}
			pos++;
			if (pos > trail) {
				ids++;
			}
		}
		for (int i = 0; i < ids; i++) {
			if (i >= blocks.size()) {
				break;
			}
			Block block = blocks.get(i);
			blocks.remove(block);
			directions.remove(block);
			if (TempBlock.isTempBlock(block)) {
				TempBlock.revertBlock(block, Material.AIR);
			}
		}
		tempList.clear();
	}

	private void moveWater() {
		if (!MaterialUtil.isTransparent(head.getBlock()) || RegionProtection.isRegionProtected(player, head, "Torrent")) {
			range -= 2;
		}
		Vector direction = GeneralMethods.getDirection(head, GeneralMethods.getTargetedLocation(player, range, Material.WATER)).normalize();
		head = head.add(direction.clone().multiply(1));
		head.setDirection(direction);
		playWaterbendingSound(head);
		for (Block block : GeneralMethods.getBlocksAroundPoint(head, headSize)) {
			if (directions.containsKey(block)) {
				directions.replace(block, head.clone());
			} else {
				directions.put(block, head.clone());
				blocks.add(block);
			}
		}
	}

	private void removeBlocks() {
		for (Block block : directions.keySet()) {
			if (TempBlock.isTempBlock(block)) {
				TempBlock.revertBlock(block, Material.AIR);
			}
		}
	}

	public static void freeze(Player player) {
		if (hasAbility(player, WaterFlow.class)) {
			WaterFlow wf = getAbility(player, WaterFlow.class);
			if (!wf.bPlayer.canIcebend()) return;
			if (!wf.frozen) {
				wf.bPlayer.addCooldown(wf);
				wf.freeze();
			}
		}
	}

	private void freeze() {
		frozen = true;
		for (Block block : directions.keySet()) {
			if (TempBlock.isTempBlock(block)) {
				if (rand.nextInt(5) == 0) {
					playIcebendingSound(block.getLocation());
				}
				new RegenTempBlock(block, Material.ICE, Material.ICE.createBlockData(), randInt((int) meltDelay - 250, (int) meltDelay + 250));
			}
		}
	}
	
	public int randInt(int min, int max) {
		return rand.nextInt(max - min) + min;
	}

	@Override
	public void remove() {
		if (player.isOnline() && cooldown > 0) {
			bPlayer.addCooldown(this);
		}
		if (!frozen) {
			removeBlocks();
		}

		if (usingBottle) {
			new WaterReturn(player, head.getBlock());
		}
		super.remove();
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return head;
	}

	@Override
	public String getName() {
		return "WaterFlow";
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
		return new WaterFlow(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		return ComboUtil.generateCombinationFromList(this, JedCoreConfig.getConfig(player).getStringList("Abilities.Water.WaterCombo.WaterFlow.Combination"));
	}

	@Override
	public String getInstructions() {
		return JedCoreConfig.getConfig(player).getString("Abilities.Water.WaterCombo.WaterFlow.Instructions");
	}

	@Override
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
	   return "* JedCore Addon *\n" + config.getString("Abilities.Water.WaterCombo.WaterFlow.Description");
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

	public int getMaxRange() {
		return maxRange;
	}

	public void setMaxRange(int maxRange) {
		this.maxRange = maxRange;
	}

	public int getMinRange() {
		return minRange;
	}

	public void setMinRange(int minRange) {
		this.minRange = minRange;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getMeltDelay() {
		return meltDelay;
	}

	public void setMeltDelay(long meltDelay) {
		this.meltDelay = meltDelay;
	}

	public long getTrail() {
		return trail;
	}

	public void setTrail(long trail) {
		this.trail = trail;
	}

	public boolean isAvatar() {
		return avatar;
	}

	public void setAvatar(boolean avatar) {
		this.avatar = avatar;
	}

	public boolean isStayAtSource() {
		return stayAtSource;
	}

	public void setStayAtSource(boolean stayAtSource) {
		this.stayAtSource = stayAtSource;
	}

	public int getStayRange() {
		return stayRange;
	}

	public void setStayRange(int stayRange) {
		this.stayRange = stayRange;
	}

	public boolean isFullMoonEnabled() {
		return fullMoonEnabled;
	}

	public void setFullMoonEnabled(boolean fullMoonEnabled) {
		this.fullMoonEnabled = fullMoonEnabled;
	}

	public int getFullMoonCooldown() {
		return fullMoonCooldown;
	}

	public void setFullMoonCooldown(int fullMoonCooldown) {
		this.fullMoonCooldown = fullMoonCooldown;
	}

	public int getFullMoonDuration() {
		return fullMoonDuration;
	}

	public void setFullMoonDuration(int fullMoonDuration) {
		this.fullMoonDuration = fullMoonDuration;
	}

	public boolean isPlayerRideOwnFlow() {
		return playerRideOwnFlow;
	}

	public void setPlayerRideOwnFlow(boolean playerRideOwnFlow) {
		this.playerRideOwnFlow = playerRideOwnFlow;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getAvatarSize() {
		return avatarSize;
	}

	public void setAvatarSize(int avatarSize) {
		this.avatarSize = avatarSize;
	}

	public int getFullMoonSizeSmall() {
		return fullMoonSizeSmall;
	}

	public void setFullMoonSizeSmall(int fullMoonSizeSmall) {
		this.fullMoonSizeSmall = fullMoonSizeSmall;
	}

	public int getFullMoonSizeLarge() {
		return fullMoonSizeLarge;
	}

	public void setFullMoonSizeLarge(int fullMoonSizeLarge) {
		this.fullMoonSizeLarge = fullMoonSizeLarge;
	}

	public long getAvatarDuration() {
		return avatarDuration;
	}

	public void setAvatarDuration(long avatarDuration) {
		this.avatarDuration = avatarDuration;
	}

	public boolean isCanUseBottle() {
		return canUseBottle;
	}

	public void setCanUseBottle(boolean canUseBottle) {
		this.canUseBottle = canUseBottle;
	}

	public boolean isCanUsePlants() {
		return canUsePlants;
	}

	public void setCanUsePlants(boolean canUsePlants) {
		this.canUsePlants = canUsePlants;
	}

	public boolean isRemoveOnAnyDamage() {
		return removeOnAnyDamage;
	}

	public void setRemoveOnAnyDamage(boolean removeOnAnyDamage) {
		this.removeOnAnyDamage = removeOnAnyDamage;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(Location origin) {
		this.origin = origin;
	}

	public Location getHead() {
		return head;
	}

	public void setHead(Location head) {
		this.head = head;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public Block getSourceBlock() {
		return sourceBlock;
	}

	public void setSourceBlock(Block sourceBlock) {
		this.sourceBlock = sourceBlock;
	}

	public boolean isFrozen() {
		return frozen;
	}

	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	public double getPrevHealth() {
		return prevHealth;
	}

	public void setPrevHealth(double prevHealth) {
		this.prevHealth = prevHealth;
	}

	public int getHeadSize() {
		return headSize;
	}

	public void setHeadSize(int headSize) {
		this.headSize = headSize;
	}

	public boolean isUsingBottle() {
		return usingBottle;
	}

	public void setUsingBottle(boolean usingBottle) {
		this.usingBottle = usingBottle;
	}

	public ConcurrentHashMap<Block, Location> getDirections() {
		return directions;
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.WaterCombo.WaterFlow.Enabled");
	}
}
