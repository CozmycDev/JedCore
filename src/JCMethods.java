package com.jedk1.jedcore;

import java.util.*;

import com.jedk1.jedcore.util.*;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.region.RegionProtection;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.util.TempBlock;

public class JCMethods {

	// todo: either use PKs isPlant or the registry/tags (with config)
	private static final ArrayList<Material> SMALL_PLANTS = new ArrayList<Material>(){{
		addAll(Arrays.asList(Material.GRASS, Material.FERN, Material.POPPY, Material.DANDELION, Material.OAK_SAPLING,
				Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING,
				Material.DARK_OAK_SAPLING, Material.ALLIUM, Material.ORANGE_TULIP, Material.PINK_TULIP, Material.RED_TULIP,
				Material.WHITE_TULIP, Material.ROSE_BUSH, Material.BLUE_ORCHID, Material.LILAC, Material.OXEYE_DAISY,
				Material.AZURE_BLUET, Material.PEONY, Material.SUNFLOWER, Material.LARGE_FERN, Material.RED_MUSHROOM,
				Material.BROWN_MUSHROOM, Material.PUMPKIN_STEM, Material.MELON_STEM, Material.WHEAT, Material.TALL_GRASS,
				Material.BEETROOTS, Material.CARROTS, Material.POTATOES, Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS,
				Material.BAMBOO, Material.BAMBOO_SAPLING));

		int serverVersion = GeneralMethods.getMCVersion();
		if (serverVersion >= 1170) {
			add(Material.getMaterial("AZALEA"));
			add(Material.getMaterial("FLOWERING_AZALEA"));
			add(Material.getMaterial("FLOWERING_AZALEA_LEAVES"));
			add(Material.getMaterial("AZALEA_LEAVES"));
			add(Material.getMaterial("BIG_DRIPLEAF"));
			add(Material.getMaterial("BIG_DRIPLEAF_STEM"));
			add(Material.getMaterial("SMALL_DRIPLEAF"));
			add(Material.getMaterial("HANGING_ROOTS"));
			add(Material.getMaterial("GLOW_LICHEN"));
			add(Material.getMaterial("CAVE_VINES"));
			add(Material.getMaterial("CAVE_VINES_PLANT"));
		}
	}};

	private static List<String> worlds = new ArrayList<>();
	private static List<String> combos = new ArrayList<>();

	public static List<String> getDisabledWorlds() {
		return JCMethods.worlds;
	}

	public static void registerDisabledWorlds() {
		worlds.clear();
		List<String> registeredworlds = ProjectKorra.plugin.getConfig().getStringList("Properties.DisabledWorlds");
		if (!registeredworlds.isEmpty()) {
			worlds.addAll(registeredworlds);
		}
	}
	
	public static boolean isDisabledWorld(World world) {
		return getDisabledWorlds().contains(world.getName());
	}

	public static List<String> getCombos() {
		return JCMethods.combos;
	}

	public static void registerCombos() {
		combos.clear();
		combos.addAll(ComboManager.getComboAbilities().keySet());
	}

	/**
	 * Gets the points of a line between two points.
	 * @param startLoc
	 * @param endLoc
	 * @param points
	 * @return locations
	 */
	public static List<Location> getLinePoints(Location startLoc, Location endLoc, int points){
		List<Location> locations = new ArrayList<Location>();
		Location diff = endLoc.subtract(startLoc);
		double diffX = diff.getX() / points;
		double diffY = diff.getY() / points;
		double diffZ = diff.getZ() / points;
		Location loc = startLoc;
		for(int i = 0; i < points; i++){
			loc.add(new Location(startLoc.getWorld(), diffX, diffY, diffZ));
			locations.add(loc.clone());
		}
		return locations;
	}

	public static List<Location> getCirclePoints(Location location, int points, double size) {
		return getCirclePoints(location, points, size, 0);
	}

	/**
	 * Gets points in a circle.
	 * @param location
	 * @param points
	 * @param size
	 * @return
	 */
	public static List<Location> getCirclePoints(Location location, int points, double size, double startangle){
		List<Location> locations = new ArrayList<Location>();
		for(int i = 0; i < 360; i += 360/points){
			double angle = (i * Math.PI / 180);
			double x = size * Math.cos(angle + startangle);
			double z = size * Math.sin(angle + startangle);
			Location loc = location.clone();
			loc.add(x, 0, z);
			locations.add(loc);
		}
		return locations;
	}

	/**
	 * Gets points in a vertical circle.
	 * @param location
	 * @param points
	 * @param size
	 * @param yawOffset
	 * @return
	 */
	public static List<Location> getVerticalCirclePoints(Location location, int points, double size, float yawOffset) {
		List<Location> locations = new ArrayList<>();
		Location fakeLoc = location.clone();
		fakeLoc.setPitch(0);
		fakeLoc.setYaw(yawOffset);
		Vector direction = fakeLoc.getDirection();

		for(double j = -180; j <= 180; j += points){
			Location tempLoc = fakeLoc.clone();
			Vector newDir = direction.clone().multiply(size * Math.cos(Math.toRadians(j)));
			tempLoc.add(newDir);
			tempLoc.setY(tempLoc.getY() + size + (size * Math.sin(Math.toRadians(j))));
			locations.add(tempLoc.clone());
		}
		return locations;
	}

	/**
	 * Remove an item from a players inventory.
	 * @param player
	 * @param material
	 * @param amount
	 * @return
	 */
	public static boolean removeItemFromInventory(Player player, Material material, int amount) {
		for (ItemStack item : player.getInventory().getContents()) {
			if (item != null && item.getType() == material) {
				if (item.getAmount() == amount) {
					Map<Integer, ? extends ItemStack> remaining = player.getInventory().removeItem(item);

					if (!remaining.isEmpty()) {
						ItemStack offhand = player.getInventory().getItemInOffHand();

						// Spigot seems to not handle offhand correctly with removeItem, so try to manually remove it.
						if (offhand != null && offhand.getType() == material && offhand.getAmount() == amount) {
							player.getInventory().setItemInOffHand(null);
						}
					}
				} else if (item.getAmount() > amount) {
					item.setAmount(item.getAmount() - amount);
				}
				
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Gets points in a spiral shape.
	 * @param location
	 * @param points
	 * @param spiralCount
	 * @param startAngle
	 * @param startSize
	 * @param finalSize
	 * @param noClip
	 * @return
	 */
	public static List<Location> getSpiralPoints(Location location, int points, int spiralCount, int startAngle, double startSize, double finalSize, boolean noClip){
		return getSpiralPoints(location, points, spiralCount, 0.0D, startAngle, startSize, finalSize, noClip);
	}

	/**
	 * Gets points in a vertical spiral shape, could be used for a tornado.
	 * @param location
	 * @param points
	 * @param spiralCount
	 * @param height
	 * @param startAngle
	 * @param startSize
	 * @param finalSize
	 * @param noClip
	 * @return
	 */
	public static List<Location> getSpiralPoints(Location location, int points, int spiralCount, double height, int startAngle, double startSize, double finalSize, boolean noClip){
		List<Location> locations = new ArrayList<Location>();

		points = points/spiralCount;
		double sizeIncr = ((finalSize - startSize) / points)/spiralCount;
		double hightIncr = (height/points)/spiralCount;
		double size = startSize;
		for(int i = 0; i < spiralCount; i++){
			for(int j = 0; j < 360; j += 360/points){
				hightIncr = hightIncr + ((height/points)/spiralCount);
				size = size + sizeIncr;
				double angle = (j * Math.PI / 180);
				double x = size * Math.cos(angle + startAngle);
				double z = size * Math.sin(angle + startAngle);
				Location loc = location.clone();
				loc.add(x, hightIncr, z);
				if(!noClip && ElementalAbility.isAir(loc.getBlock().getType()))
					locations.add(loc);
				else if(noClip)
					locations.add(loc);
			}
		}

		return locations;
	}

	public static void extinguishBlocks(Player player, String ability, int range, int radius, boolean fire, boolean lava){
		for (Block block : GeneralMethods.getBlocksAroundPoint(player.getTargetBlock(null, range).getLocation(), radius)) {
			Material mat = block.getType();
			if(mat != Material.FIRE && mat != Material.LAVA && mat != Material.SOUL_FIRE)
				continue;
			if (RegionProtection.isRegionProtected(player, block.getLocation(), ability))
				continue;
			if (((mat == Material.FIRE || mat == Material.SOUL_FIRE) && fire)||(mat == Material.LAVA && lava)) {
				block.setType(mat != Material.LAVA ? Material.AIR : isLiquidSource(block) ? Material.OBSIDIAN : Material.COBBLESTONE);
				block.getWorld().playEffect(block.getLocation(), Effect.EXTINGUISH, 0);
			}
		}
	}

	/**
	 * Checks if 3 blocks around the block are of the required type.
	 * @param block
	 * @param type
	 * @return
	 */
	public static boolean isAdjacentToThreeOrMoreSources(Block block, Material type) {
		if (TempBlock.isTempBlock(block)) {
			return false;
		}
		int sources = 0;
		BlockFace[] faces = { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN };
		for (BlockFace face : faces) {
			Block blocki = block.getRelative(face);
			if ((blocki.getType() == type)) {
				sources++;
			}
		}
		if (sources >= 2)
			return true;
		return false;
	}

	static Material[] unbreakables = { Material.BEDROCK, Material.BARRIER,
		Material.NETHER_PORTAL, Material.END_PORTAL,
		Material.END_PORTAL_FRAME, Material.OBSIDIAN};

	public static boolean isUnbreakable(Block block) {
		if (block.getState() instanceof InventoryHolder) {
			return true;
		}
		if (Arrays.asList(unbreakables).contains(block.getType()))
			return true;
		return false;
	}

	public static boolean isLiquidSource(Block block) {
		if (!block.isLiquid()) {
			return false;
		}

		if (!(block.getBlockData() instanceof Levelled)) {
			return false;
		}

		Levelled levelData = (Levelled) block.getBlockData();

		return levelData.getLevel() == 0;
	}

	// TODO: Should this be reimplemented or has the rpg plugin been abandoned?
	public static boolean isSozinsComet(World world) {
		return false;
	}

	// TODO: Should this be reimplemented or has the rpg plugin been abandoned?
	public static boolean isLunarEclipse(World world) {
		return false;
	}

	// todo: see SMALL_PLANTS
	public static boolean isDoublePlant(Material material) {
		return material == Material.SUNFLOWER || material == Material.LILAC || material == Material.TALL_GRASS ||
				material == Material.LARGE_FERN || material == Material.ROSE_BUSH || material == Material.PEONY;
	}

	public static boolean isSmallPlant(Block block) {
		return isSmallPlant(block.getType());
	}

	public static boolean isSmallPlant(Material material) {
		return WaterAbility.isPlant(material);
		//return SMALL_PLANTS.contains(material);
	}

	public static void displayColoredParticles(String hex, Location location, int amount, double offsetX, double offsetY, double offsetZ, double extra) {
		displayColoredParticles(hex, location, amount, offsetX, offsetY, offsetZ, extra, 255);
	}

	public static void displayColoredParticles(String hex, Location location, int amount, double offsetX, double offsetY, double offsetZ, double extra, int alpha) {
		JedCore.plugin.getParticleAdapter().displayColoredParticles(hex, location, amount, offsetX, offsetY, offsetZ, extra, alpha);
	}

	public static void emitLight(Location loc) {
		ConfigurationSection config = JedCoreConfig.getConfig((Player)null);
		if (config.getBoolean("Properties.Fire.DynamicLight.Enabled")) {
			int brightness = config.getInt("Properties.Fire.DynamicLight.Brightness");
			long keepAlive = config.getLong("Properties.Fire.DynamicLight.KeepAlive");

			LightManager.createLight(loc).brightness(brightness).timeUntilFadeout(keepAlive).emit();
		}
	}

	public static void reload() {
		JedCore.log.info("JedCore Reloaded.");
		JedCore.plugin.reloadConfig();
		JedCore.logDebug = JedCoreConfig.getConfig((World)null).getBoolean("Properties.LogDebug");
		CoreAbility.registerPluginAbilities(JedCore.plugin, "com.jedk1.jedcore.ability");
		registerDisabledWorlds();
		registerCombos();
		RegenTempBlock.revertAll();
		JedCore.plugin.initializeCollisions();
		FireTick.loadMethod();
	}
}
