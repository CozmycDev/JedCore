package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.Collapse;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class EarthPillar extends EarthAbility implements AddonAbility {

	private static final ConcurrentHashMap<Block, EarthPillar> AFFECTED_BLOCKS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<EarthPillar, List<Block>> AFFECTED = new ConcurrentHashMap<>();

	private Block block;
	private BlockFace face;
	@Attribute(Attribute.HEIGHT)
	private int height;
	@Attribute(Attribute.RANGE)
	private int range;
	private int step;

	private final List<Block> blocks = new ArrayList<>();

	public EarthPillar(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		setFields();
		Block target = BlockSource.getEarthSourceBlock(player, range, ClickType.SHIFT_DOWN);
		if (target != null && !AFFECTED_BLOCKS.containsKey(target)) {
			List<Block> blocks = player.getLastTwoTargetBlocks(null, range);
			if (blocks.size() > 1) {
				this.player = player;
				face = blocks.get(1).getFace(blocks.get(0));
				block = blocks.get(1);
				height = getEarthbendableBlocksLength(block, getDirection(face).clone().multiply(-1), height);
				start();
			}
		} else if (target != null && AFFECTED_BLOCKS.containsKey(target)) {
			List<Block> blocks = AFFECTED.get(AFFECTED_BLOCKS.get(target));
			if (blocks != null && !blocks.isEmpty()) {
				for (Block b : blocks) {
					Collapse.revertBlock(b);
				}
				playEarthbendingSound(target.getLocation());
				AFFECTED.remove(AFFECTED_BLOCKS.get(target));
			}
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		height = config.getInt("Abilities.Earth.EarthPillar.Height");
		range = config.getInt("Abilities.Earth.EarthPillar.Range");
	}

	@Override
	public void progress() {
		if (step < height) {
			step++;
			movePillar();
		} else {
			AFFECTED.put(this, blocks);
			remove();
		}
	}

	private void movePillar() {
		moveEarth(block, getDirection(face), height);
		block = block.getRelative(face);
		AFFECTED_BLOCKS.put(block, this);
		blocks.add(block);
	}

	private Vector getDirection(BlockFace face) {
		switch (face) {
		case UP:
			return new Vector(0, 1, 0);
		case DOWN:
			return new Vector(0, -1, 0);
		case NORTH:
			return new Vector(0, 0, -1);
		case SOUTH:
			return new Vector(0, 0, 1);
		case EAST:
			return new Vector(1, 0, 0);
		case WEST:
			return new Vector(-1, 0, 0);
		default:
			return null;
		}
	}

	public static void progressAll() {
		for (Block block : AFFECTED_BLOCKS.keySet()) {
			if (!EarthAbility.isEarthbendable(AFFECTED_BLOCKS.get(block).getPlayer(), block)) {
				AFFECTED_BLOCKS.remove(block);
			}
		}
	}
	
	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return block != null ? block.getLocation() : null;
	}

	@Override
	public String getName() {
		return "EarthPillar";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.EarthPillar.Description");
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public BlockFace getFace() {
		return face;
	}

	public void setFace(BlockFace face) {
		this.face = face;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
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
		return config.getBoolean("Abilities.Earth.EarthPillar.Enabled");
	}
}