package com.jedk1.jedcore.ability.waterbending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.util.WaterReturn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Drain extends WaterAbility implements AddonAbility {

	private final List<Location> locations = new ArrayList<>();
	static final Set<TempBlock> WATER_TEMPS = new HashSet<>();

	//Savannas are 1.0 temp with 0 humidity. Deserts are 2.0 temp with 0 humidity.
	private static float MAX_TEMP = 1.0F;
	private static float MIN_HUMIDITY = 0.01F;

	private long regenDelay;
	@Attribute(Attribute.DURATION)
	private long duration; // 2000
	@Attribute(Attribute.COOLDOWN)
	private long cooldown; // 2000
	private double absorbSpeed; // 0.1
	@Attribute(Attribute.RADIUS)
	private int radius; // 6
	@Attribute("Chance")
	private int chance; // 20
	private int absorbRate; // 6
	private int holdRange; // 2
	private boolean blastsEnabled; // true
	private int maxBlasts;
	private boolean keepSrc; // false
	private boolean useRain;
	private boolean usePlants;

	private double blastRange; // 20
	private double blastDamage; // 1.5
	private double blastSpeed; // 2

	private boolean drainTemps;

	private long endTime;
	private int absorbed = 0;
	private int charge = 7;
	private boolean noFill;
	private int blasts;
	private boolean hasCharge;
	private final Material[] fillables = { Material.GLASS_BOTTLE, Material.BUCKET };

	Random rand = new Random();

	public Drain(Player player) {
		super(player);
		if (!bPlayer.canBend(this) || hasAbility(player, Drain.class)) {
			return;
		}
		setFields();
		this.usePlants = bPlayer.canPlantbend();
		endTime = System.currentTimeMillis() + duration;
		if (!canFill()) {
			if (!blastsEnabled)
				return;
			noFill = true;
		}
		start();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		regenDelay = config.getLong("Abilities.Water.Drain.RegenDelay");
		duration = config.getLong("Abilities.Water.Drain.Duration");
		cooldown = config.getLong("Abilities.Water.Drain.Cooldown");
		absorbSpeed = config.getDouble("Abilities.Water.Drain.AbsorbSpeed");
		radius = config.getInt("Abilities.Water.Drain.Radius");
		chance = config.getInt("Abilities.Water.Drain.AbsorbChance");
		absorbRate = config.getInt("Abilities.Water.Drain.AbsorbRate");
		holdRange = config.getInt("Abilities.Water.Drain.HoldRange");
		blastsEnabled = config.getBoolean("Abilities.Water.Drain.BlastsEnabled");
		maxBlasts = config.getInt("Abilities.Water.Drain.MaxBlasts");
		keepSrc = config.getBoolean("Abilities.Water.Drain.KeepSource");
		blastRange = config.getDouble("Abilities.Water.Drain.BlastRange");
		blastDamage = config.getDouble("Abilities.Water.Drain.BlastDamage");
		blastSpeed = config.getDouble("Abilities.Water.Drain.BlastSpeed");
		useRain = config.getBoolean("Abilities.Water.Drain.AllowRainSource");
		drainTemps = config.getBoolean("Abilities.Water.Drain.DrainTempBlocks");

		applyModifiers();
	}

	private void applyModifiers() {
		if (isNight(player.getWorld())) {
			cooldown -= ((long) getNightFactor(cooldown) - cooldown);
			blastRange = getNightFactor(blastRange);
			blastDamage = getNightFactor(blastDamage);
		}
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}
		if (!noFill) {
			if (!player.isSneaking()) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}
			if (!canFill()) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}
			if (System.currentTimeMillis() > endTime) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}
			if (absorbed >= absorbRate) {
				fill();
				absorbed = 0;
			}
			checkForValidSource();
		} else {
			if (blasts >= maxBlasts) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}
			if (player.isSneaking()) {
				if (charge >= 2) {
					checkForValidSource();
				}
				if (absorbed >= absorbRate) {
					hasCharge = true;
					absorbed = 0;
					if (charge >= 3) {
						charge -= 2;
					}
				}
			} else if (!hasCharge || !keepSrc) {
				bPlayer.addCooldown(this);
				remove();
				return;
			}
			if (hasCharge) {
				displayWaterSource();
			}
		}
		dragWater();
	}

	public static void fireBlast(Player player) {
		if (hasAbility(player, Drain.class)) {
			getAbility(player, Drain.class).fireBlast();
		}
	}

	private void fireBlast() {
		if (charge <= 1) {
			hasCharge = false;
			charge = 7;
			blasts++;
			new DrainBlast(player, blastRange, blastDamage, blastSpeed, holdRange);
		}
	}

	private void displayWaterSource() {
		Location location = player.getEyeLocation().add(player.getLocation().getDirection().multiply(holdRange));
		Block block = location.getBlock();
		if (!GeneralMethods.isSolid(block) || isTransparent(block)) {
			TempBlock tb = new TempBlock(block, Material.WATER.createBlockData(bd -> ((Levelled) bd).setLevel(charge)), 100L);
			WATER_TEMPS.add(tb);
			tb.setRevertTask(() -> WATER_TEMPS.remove(tb));
		}
	}

	private boolean canFill() {
		for (ItemStack items : player.getInventory()) {
			if (items != null && Arrays.asList(fillables).contains(items.getType())) {
				return true;
			}
		}
		return false;
	}

	private void fill() {
		for (int x = 0; x < absorbed; x++) {
			for (Material fillable : fillables) {
				int slot = player.getInventory().first(fillable);
				if (slot == -1){
					continue;
				}
				if (player.getInventory().getItem(slot).getAmount() > 1) {
					player.getInventory().getItem(slot).setAmount(player.getInventory().getItem(slot).getAmount() - 1);

					ItemStack filled = getFilled(fillable);
					HashMap<Integer, ItemStack> cantfit = player.getInventory().addItem(filled);
					for (int id : cantfit.keySet()) {
						player.getWorld().dropItem(player.getEyeLocation(), cantfit.get(id));
					}
				} else {
					player.getInventory().setItem(slot, getFilled(fillable));
				}
				break;
			}
		}
	}

	private ItemStack getFilled(Material type) {
		ItemStack filled = null;
		if (type == Material.GLASS_BOTTLE) {
			filled = WaterReturn.waterBottleItem();
		} else if (type == Material.BUCKET) {
			filled = new ItemStack(Material.WATER_BUCKET, 1);
		}

		return filled;
	}

	private void checkForValidSource() {
		List<Location> locs = GeneralMethods.getCircle(player.getLocation(), radius, radius, false, true, 0);
		for (int i = 0; i < locs.size(); i++) {
			Block block = locs.get(rand.nextInt(locs.size()-1)).getBlock();
			if (rand.nextInt(chance) == 0) {
				Location pLoc = player.getLocation();
				World world = pLoc.getWorld();
				double temp = world.getTemperature(pLoc.getBlockX(), pLoc.getBlockY(), pLoc.getBlockZ());
				double humidity = world.getHumidity(pLoc.getBlockX(), pLoc.getBlockY(), pLoc.getBlockZ());
				if (block.getY() > world.getMinHeight() && block.getY() < world.getMaxHeight()) {
					Location bLoc = block.getLocation();
					if (useRain && world.hasStorm() && !(temp >= MAX_TEMP || humidity <= MIN_HUMIDITY)) {
						if (pLoc.getY() >= world.getHighestBlockAt(pLoc).getLocation().getY()) {
							if (bLoc.getY() >= world.getHighestBlockAt(pLoc).getLocation().getY()) {
								locations.add(bLoc.clone().add(.5, .5, .5));
								return;
							}
						}
					}
					if (usePlants && JCMethods.isSmallPlant(block) && !isObstructed(bLoc, player.getEyeLocation())) {
						drainPlant(block);
					} else if (usePlants && ElementalAbility.isPlant(block) && !isObstructed(bLoc, player.getEyeLocation())) {
						locations.add(bLoc.clone().add(.5, .5, .5));
						new TempBlock(block, Material.AIR.createBlockData(), regenDelay);
					} else if (isWater(block)) {
						TempBlock tb = TempBlock.get(block);
						if ((tb == null || (drainTemps && !WATER_TEMPS.contains(tb)))) {
							drainWater(block);
						}
					}
				}
			}
		}
	}

	private boolean isObstructed(Location location1, Location location2) {
		Vector loc1 = location1.toVector();
		Vector loc2 = location2.toVector();

		Vector direction = loc2.subtract(loc1);
		direction.normalize();

		Location loc;

		double max = location1.distance(location2);

		for (double i = 1; i <= max; i++) {
			loc = location1.clone().add(direction.clone().multiply(i));
			if (!isTransparent(loc.getBlock()))
				return true;
		}

		return false;
	}

	private void drainPlant(Block block) {
		if (JCMethods.isSmallPlant(block)) {
			if (JCMethods.isSmallPlant(block.getRelative(BlockFace.DOWN))) {
				if (JCMethods.isDoublePlant(block.getType())) {
					block = block.getRelative(BlockFace.DOWN);
					locations.add(block.getLocation().clone().add(.5, .5, .5));
					new TempBlock(block, Material.DEAD_BUSH.createBlockData(), regenDelay);
					return;
				}
				block = block.getRelative(BlockFace.DOWN);
			}
			locations.add(block.getLocation().clone().add(.5, .5, .5));
			new TempBlock(block, Material.DEAD_BUSH.createBlockData(), regenDelay);
		}
	}

	private void drainWater(Block block) {
		if (isTransparent(block.getRelative(BlockFace.UP)) && !isWater(block.getRelative(BlockFace.UP))) {
			locations.add(block.getLocation().clone().add(.5, .5, .5));
			if (block.getBlockData() instanceof Waterlogged) {
				new TempBlock(block, block.getType().createBlockData(bd -> ((Waterlogged) bd).setWaterlogged(false)), regenDelay);
			} else {
				TempBlock tb = new TempBlock(block, Material.WATER.createBlockData(bd -> ((Levelled) bd).setLevel(1)), regenDelay);
				WATER_TEMPS.add(tb);
				tb.setRevertTask(() -> WATER_TEMPS.remove(tb));
			}
		}
	}

	private void dragWater() {
		List<Integer> toRemove = new ArrayList<>();
		if (!locations.isEmpty()) {
			for (Location l : locations) {
				Location playerLoc = player.getLocation().add(0, 1, 0);
				if (noFill)
					playerLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(holdRange)).subtract(0, .8, 0);
				Vector dir = GeneralMethods.getDirection(l, playerLoc);
				l = l.add(dir.multiply(absorbSpeed));
				ParticleEffect.WATER_SPLASH.display(l, 1, 0, 0, 0, 0);
				GeneralMethods.displayColoredParticle("0099FF", l);
				if (l.distance(playerLoc) < 1) {
					toRemove.add(locations.indexOf(l));
					absorbed++;
				}
			}
		}
		if (!toRemove.isEmpty()) {
			for (int i : toRemove) {
				if (i < locations.size())
					locations.remove(i);
			}
			toRemove.clear();
		}
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
		return "Drain";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Water.Drain.Description");
	}

	@Override
	public List<Location> getLocations() {
		return locations;
	}

	public long getRegenDelay() {
		return regenDelay;
	}

	public void setRegenDelay(long regenDelay) {
		this.regenDelay = regenDelay;
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

	public double getAbsorbSpeed() {
		return absorbSpeed;
	}

	public void setAbsorbSpeed(double absorbSpeed) {
		this.absorbSpeed = absorbSpeed;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public int getChance() {
		return chance;
	}

	public void setChance(int chance) {
		this.chance = chance;
	}

	public int getAbsorbRate() {
		return absorbRate;
	}

	public void setAbsorbRate(int absorbRate) {
		this.absorbRate = absorbRate;
	}

	public int getHoldRange() {
		return holdRange;
	}

	public void setHoldRange(int holdRange) {
		this.holdRange = holdRange;
	}

	public boolean isBlastsEnabled() {
		return blastsEnabled;
	}

	public void setBlastsEnabled(boolean blastsEnabled) {
		this.blastsEnabled = blastsEnabled;
	}

	public int getMaxBlasts() {
		return maxBlasts;
	}

	public void setMaxBlasts(int maxBlasts) {
		this.maxBlasts = maxBlasts;
	}

	public boolean isKeepSrc() {
		return keepSrc;
	}

	public void setKeepSrc(boolean keepSrc) {
		this.keepSrc = keepSrc;
	}

	public boolean isUseRain() {
		return useRain;
	}

	public void setUseRain(boolean useRain) {
		this.useRain = useRain;
	}

	public boolean isUsePlants() {
		return usePlants;
	}

	public void setUsePlants(boolean usePlants) {
		this.usePlants = usePlants;
	}

	public double getBlastRange() {
		return blastRange;
	}

	public void setBlastRange(double blastRange) {
		this.blastRange = blastRange;
	}

	public double getBlastDamage() {
		return blastDamage;
	}

	public void setBlastDamage(double blastDamage) {
		this.blastDamage = blastDamage;
	}

	public double getBlastSpeed() {
		return blastSpeed;
	}

	public void setBlastSpeed(double blastSpeed) {
		this.blastSpeed = blastSpeed;
	}

	public boolean isDrainTemps() {
		return drainTemps;
	}

	public void setDrainTemps(boolean drainTemps) {
		this.drainTemps = drainTemps;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public int getAbsorbed() {
		return absorbed;
	}

	public void setAbsorbed(int absorbed) {
		this.absorbed = absorbed;
	}

	public int getCharge() {
		return charge;
	}

	public void setCharge(int charge) {
		this.charge = charge;
	}

	public boolean isNoFill() {
		return noFill;
	}

	public void setNoFill(boolean noFill) {
		this.noFill = noFill;
	}

	public int getBlasts() {
		return blasts;
	}

	public void setBlasts(int blasts) {
		this.blasts = blasts;
	}

	public boolean isHasCharge() {
		return hasCharge;
	}

	public void setHasCharge(boolean hasCharge) {
		this.hasCharge = hasCharge;
	}

	public Material[] getFillables() {
		return fillables;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.Drain.Enabled");
	}
}
