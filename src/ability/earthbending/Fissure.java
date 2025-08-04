package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.Information;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Fissure extends LavaAbility implements AddonAbility {

	@Attribute(Attribute.RANGE)
	private int slapRange;
	@Attribute(Attribute.WIDTH)
	private int maxWidth;
	private long slapDelay;
	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;

	private Location location;
	private Vector direction;
	private Vector blockDirection;
	private long time;
	private long step;
	private int slap;
	private int width;
	private boolean progressed;
	
	static Random rand = new Random();

	private final List<Location> centerSlap = new ArrayList<>();
	private final List<Block> blocks = new ArrayList<>();
	private final List<TempBlock> tempblocks = new ArrayList<>();

	public Fissure(Player player) {
		super(player);
		
		if (!bPlayer.canBend(this) || hasAbility(player, Fissure.class) || !bPlayer.canLavabend()) {
			return;
		}

		setFields();
		time = System.currentTimeMillis();
		step = System.currentTimeMillis() + slapDelay;
		location = player.getLocation().clone();
		location.setPitch(0);
		direction = location.getDirection();
		blockDirection = this.direction.clone().setX(Math.round(this.direction.getX()));
		blockDirection = blockDirection.setZ(Math.round(direction.getZ()));
		if (prepareLine()) {
			start();
			if (!isRemoved()) {
				bPlayer.addCooldown(this);
			}
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		slapRange = config.getInt("Abilities.Earth.Fissure.SlapRange");
		maxWidth = config.getInt("Abilities.Earth.Fissure.MaxWidth");
		slapDelay = config.getInt("Abilities.Earth.Fissure.SlapDelay");
		duration = config.getInt("Abilities.Earth.Fissure.Duration");
		cooldown = config.getInt("Abilities.Earth.Fissure.Cooldown");
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > step && slap <= centerSlap.size()) {
			time = System.currentTimeMillis();
			step = System.currentTimeMillis() + slapDelay;
			slapCenter();
			slap++;
		}
		if (System.currentTimeMillis() > time + duration) {
			remove();
		}
	}

	private boolean prepareLine() {
		direction = player.getEyeLocation().getDirection().setY(0).normalize();
		blockDirection = this.direction.clone().setX(Math.round(this.direction.getX()));
		blockDirection = blockDirection.setZ(Math.round(direction.getZ()));
		Location origin = player.getLocation().add(0, -1, 0).add(blockDirection.multiply(2));
		if (isEarthbendable(player, origin.getBlock())) {
			BlockIterator bi = new BlockIterator(player.getWorld(), origin.toVector(), direction, 0, slapRange);

			while (bi.hasNext()) {
				Block b = bi.next();

				if (b.getY() > 1 && b.getY() < 255 && !RegionProtection.isRegionProtected(this, b.getLocation())) {
					if (EarthAbility.getMovedEarth().containsKey(b)){
						Information info = EarthAbility.getMovedEarth().get(b);
						if(!info.getBlock().equals(b)) {
							continue;
						}
					}

					while (!isEarthbendable(player, b)) {
						b = b.getRelative(BlockFace.DOWN);
						if (b.getY() < b.getWorld().getMinHeight() || b.getY() > b.getWorld().getMaxHeight()) {
							break;
						}
						if (isEarthbendable(player, b)) {
							break;
						}
					}

					while (!isTransparent(b.getRelative(BlockFace.UP))) {
						b = b.getRelative(BlockFace.UP);
						if (b.getY() < b.getWorld().getMinHeight() || b.getY() > b.getWorld().getMaxHeight()) {
							break;
						}
						if (isEarthbendable(player, b.getRelative(BlockFace.UP))) {
							break;
						}
					}

					if (isEarthbendable(player, b)) {
						centerSlap.add(b.getLocation());
					} else {
						break;
					}
				}
			}
			return true;
		}
		return false;
	}

	private void slapCenter() {
		for (Location location : centerSlap) {
			if (centerSlap.indexOf(location) == slap) {
				addTempBlock(location.getBlock(), Material.LAVA);
			}
		}
		if (slap >= centerSlap.size()) {
			progressed = true;
		}
	}
	
	public static void performAction(Player player) {
		if (hasAbility(player, Fissure.class)) {
			getAbility(player, Fissure.class).performAction();
		}
	}
	
	private void performAction() {
		if (width < maxWidth) {
			expandFissure();
		} else if (blocks.contains(player.getTargetBlock(null, 10))) {
			forceRevert();
		}
	}

	private void expandFissure() {
		if (progressed && width <= maxWidth) {
			width++;
			for (Location location : centerSlap) {
				Block left = location.getBlock().getRelative(getLeftBlockFace(GeneralMethods.getCardinalDirection(blockDirection)), width);
				expand(left);

				Block right = location.getBlock().getRelative(getLeftBlockFace(GeneralMethods.getCardinalDirection(blockDirection)).getOppositeFace(), width);
				expand(right);
			}
		}
		Collections.reverse(blocks);
	}

	private void expand(Block block) {
		if (block != null && block.getY() > 1 && block.getY() < 255 && !RegionProtection.isRegionProtected(this, block.getLocation())) {
			if (EarthAbility.getMovedEarth().containsKey(block)){
				Information info = EarthAbility.getMovedEarth().get(block);
				if(!info.getBlock().equals(block)) {
					return;
				}
			}

			while (!isEarthbendable(player, block)) {
				block = block.getRelative(BlockFace.DOWN);
				if (block.getY() < 1 || block.getY() > 255) {
					break;
				}
				if (isEarthbendable(player, block)) {
					break;
				}
			}

			while (!isTransparent(player, block.getRelative(BlockFace.UP))) {
				block = block.getRelative(BlockFace.UP);
				if (block.getY() < 1 || block.getY() > 255) {
					break;
				}
				if (isEarthbendable(player, block.getRelative(BlockFace.UP))) {
					break;
				}
			}

			if (isEarthbendable(player, block)) {
				addTempBlock(block, Material.LAVA);
			}
		}
	}

	private void addTempBlock(Block block, Material material) {
		ParticleEffect.LAVA.display(block.getLocation(), 0, 0, 0, 0, 1);
		playEarthbendingSound(block.getLocation());
		if (DensityShift.isPassiveSand(block)) {
            DensityShift.revertSand(block);
		}
		tempblocks.add(new TempBlock(block, material.createBlockData(), this));
		blocks.add(block);
	}

	public BlockFace getLeftBlockFace(BlockFace forward) {
		switch (forward) {
		case NORTH:
			return BlockFace.WEST;
		case SOUTH:
			return BlockFace.EAST;
		case WEST:
			return BlockFace.SOUTH;
		case EAST:
			return BlockFace.NORTH;
		case NORTH_WEST:
			return BlockFace.SOUTH_WEST;
		case NORTH_EAST:
			return BlockFace.NORTH_WEST;
		case SOUTH_WEST:
			return BlockFace.SOUTH_EAST;
		case SOUTH_EAST:
			return BlockFace.NORTH_EAST;
		default:
			return BlockFace.NORTH;
		}
	}
	
	private void forceRevert() {
		coolLava();
	}
	
	private void coolLava() {
		tempblocks.forEach(TempBlock::revertBlock);
		for (Block block : blocks) {
			new TempBlock(block, Material.STONE.createBlockData(), 500 + (long) rand.nextInt((int) 1000));
		}
		blocks.clear();
		tempblocks.clear();
	}

	@Override
	public void remove() {
		coolLava();
		super.remove();
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String getName() {
		return "Fissure";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.Fissure.Description");
	}

	public int getSlapRange() {
		return slapRange;
	}

	public void setSlapRange(int slapRange) {
		this.slapRange = slapRange;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}

	public long getSlapDelay() {
		return slapDelay;
	}

	public void setSlapDelay(long slapDelay) {
		this.slapDelay = slapDelay;
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

	public void setLocation(Location location) {
		this.location = location;
	}

	public Vector getDirection() {
		return direction;
	}

	public void setDirection(Vector direction) {
		this.direction = direction;
	}

	public Vector getBlockDirection() {
		return blockDirection;
	}

	public void setBlockDirection(Vector blockDirection) {
		this.blockDirection = blockDirection;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getStep() {
		return step;
	}

	public void setStep(long step) {
		this.step = step;
	}

	public int getSlap() {
		return slap;
	}

	public void setSlap(int slap) {
		this.slap = slap;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public boolean isProgressed() {
		return progressed;
	}

	public void setProgressed(boolean progressed) {
		this.progressed = progressed;
	}

	public List<Location> getCenterSlap() {
		return centerSlap;
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
		return config.getBoolean("Abilities.Earth.Fissure.Enabled");
	}
}