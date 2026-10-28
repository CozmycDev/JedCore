package com.jedk1.jedcore.util;

import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegenTempBlock {

	public static Map<Block, RegenBlockData> blocks = new HashMap<>();
	public static Map<Block, TempBlock> temps = new HashMap<>();
	public static Map<Block, BlockState> states = new HashMap<>();

	/**
	 * Creates a TempBlock that reverts after a delay.
	 * @param block Block to be updated/reverted.
	 * @param material Material to be changed.
	 * @param data Data to be changed.
	 * @param delay Delay until block regens.
	 */
	public RegenTempBlock(Block block, Material material, BlockData data, long delay) {
		this(block, material, data, delay, true);
	}

	/**
	 * Creates a TempBlock or a State of a block that reverts after a certain time.
	 * @param block Block to be updated/reverted.
	 * @param material Material to be changed.
	 * @param data Data to be changed.
	 * @param delay Delay until block regens.
	 * @param temp Use TempBlock or BlockState.
	 */
	public RegenTempBlock(Block block, Material material, BlockData data, long delay, boolean temp) {
		this(block, material, data, delay, temp, null);
	}

	public RegenTempBlock(Block block, Material material, BlockData data, long delay, boolean temp, RegenCallback callback) {
		if (DensityShift.isPassiveSand(block)) {
			DensityShift.revertSand(block);
		}
		if (block.getState() instanceof InventoryHolder || block.getType() == Material.JUKEBOX) {
			return;
		}
		if (blocks.containsKey(block)) {
			blocks.replace(block, new RegenBlockData(System.currentTimeMillis() + delay, callback));
			block.setBlockData(data.clone());
		} else {
			blocks.put(block, new RegenBlockData(System.currentTimeMillis() + delay, callback));
			if (TempBlock.isTempBlock(block)) {
				TempBlock.get(block).revertBlock();
			}
			if (temp) {
				TempBlock tb = new TempBlock(block, data.clone());
				temps.put(block, tb);
			} else {
				states.put(block, block.getState());
				if (material != null) {
					block.setBlockData(data.clone());
				}
			}
		}
	}

	/**
	 * Manages blocks to be reverted.
	 */
	public static void manage() {
		List<Map.Entry<Block, RegenBlockData>> due = new ArrayList<>();

		for (Map.Entry<Block, RegenBlockData> entry : blocks.entrySet()) {
			if (System.currentTimeMillis() >= entry.getValue().endTime) {
				due.add(entry);
			}
		}

		// Restore lower blocks first so attached blocks (such as crops) have their support back.
		due.sort((a, b) -> Integer.compare(a.getKey().getY(), b.getKey().getY()));

		for (Map.Entry<Block, RegenBlockData> entry : due) {
			Block b = entry.getKey();
			RegenBlockData blockData = entry.getValue();

			TempBlock tb = temps.get(b);
			if (tb != null) {
				tb.revertBlock();
				temps.remove(b);
			}

			BlockState bs = states.get(b);
			if (bs != null) {
				bs.update(true);
				states.remove(b);
			}

			blocks.remove(b);

			if (blockData.callback != null) {
				blockData.callback.onRegen(b);
			}
		}
	}

	/**
	 * Reverts an individual block.
	 * @param block
	 */
	public static void revert(Block block) {
		if (blocks.containsKey(block)) {
			if (TempBlock.isTempBlock(block) && temps.containsKey(block)) {
				TempBlock tb = TempBlock.get(block);
				tb.revertBlock();
				temps.remove(block);
			}
			if (states.containsKey(block)) {
				states.get(block).update(true);
				states.remove(block);
			}
			blocks.remove(block);
		}
	}

	/**
	 * Reverts all blocks.
	 */
	public static void revertAll() {
		List<Block> sorted = new ArrayList<>(blocks.keySet());
		sorted.sort((a, b) -> Integer.compare(a.getY(), b.getY()));

		for (Block b : sorted) {
			if (temps.containsKey(b)) {
				TempBlock tb = temps.get(b);
				tb.revertBlock();
			}
			if (states.containsKey(b)) {
				states.get(b).update(true);
			}
		}
		temps.clear();
		states.clear();
		blocks.clear();
	}
	
	/**
	 * Returns true if the block is a RegenTempBlock.
	 * @param block
	 * @return
	 */
	public static boolean hasBlock(Block block) {
        return blocks.containsKey(block);
    }
	
	/**
	 * Returns true if the block is stored as a temp block.
	 * @param block
	 * @return
	 */
	public static boolean isTempBlock(Block block) {
        return temps.containsKey(block);
    }
	
	/**
	 * Returns true if the block is stored as a block state.
	 * @param block
	 * @return
	 */
	public static boolean isBlockState(Block block) {
        return states.containsKey(block);
    }
	
	private static class RegenBlockData {
		long endTime;
		RegenCallback callback;

		public RegenBlockData(long endTime, RegenCallback callback) {
			this.endTime = endTime;
			this.callback = callback;
		}
	}

	public interface RegenCallback {
		void onRegen(Block block);
	}
}
