package com.jedk1.jedcore.ability.earthbending;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.Information;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.firebending.util.FireDamageTimer;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

public class LavaFlux extends LavaAbility implements AddonAbility {

	@Attribute(Attribute.SPEED)
	private int speed;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	private long cleanup;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	private boolean wave;

	private Location location;
	private int step;
	private int counter;
	private long time;
	private boolean complete;

	private double knockUp;
	private double knockBack;

	Random rand = new Random();

	private static final BlockData LAVA = Material.LAVA.createBlockData(bd -> ((Levelled)bd).setLevel(1));

	private final List<Location> flux = new ArrayList<>();

	private Map<Block, TempBlock> blocks = new HashMap<>();
	private Map<Block, TempBlock> above = new HashMap<>();

	public LavaFlux(Player player) {
		super(player);

		if (!bPlayer.canBend(this) || !bPlayer.canLavabend()) {
			return;
		}

		setFields();
		time = System.currentTimeMillis();
		if (prepareLine()) {
			start();
			if (!isRemoved()) {
				bPlayer.addCooldown(this);
			}
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		speed = config.getInt("Abilities.Earth.LavaFlux.Speed");
		if (speed < 1) speed = 1;
		range = config.getInt("Abilities.Earth.LavaFlux.Range");
		cooldown = config.getLong("Abilities.Earth.LavaFlux.Cooldown");
		duration = config.getLong("Abilities.Earth.LavaFlux.Duration");
		cleanup = config.getLong("Abilities.Earth.LavaFlux.Cleanup");
		damage = config.getDouble("Abilities.Earth.LavaFlux.Damage");
		wave = config.getBoolean("Abilities.Earth.LavaFlux.Wave");
		knockUp = config.getDouble("Abilities.Earth.LavaFlux.KnockUp");
		knockBack = config.getDouble("Abilities.Earth.LavaFlux.KnockBack");
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}
		counter++;
		if (!complete) {
			if (speed <= 1 || counter % speed == 0) {
				for (int i = 0; i <= 2; i++) {
					step++;
					progressFlux();
				}
			}
		} else if (duration > cleanup) {
			if (System.currentTimeMillis() > time + duration) {
				for (TempBlock tb : blocks.values()) {
					if (!tb.isReverted()) tb.setType(Material.STONE);
				}
				remove();
			}
		}
	}

	private boolean prepareLine() {
		Vector direction = player.getEyeLocation().getDirection().setY(0).normalize();
		Vector blockdirection = direction.clone().setX(Math.round(direction.getX()));
		blockdirection = blockdirection.setZ(Math.round(direction.getZ()));
		Location origin = player.getLocation().add(0, -1, 0).add(blockdirection.multiply(2));
		if (isEarthbendable(player, origin.getBlock())) {
			BlockIterator bi = new BlockIterator(player.getWorld(), origin.toVector(), direction, 0, range);

			while (bi.hasNext()) {
				Block b = bi.next();

				if (b.getY() > b.getWorld().getMinHeight() && b.getY() < b.getWorld().getMaxHeight() && !RegionProtection.isRegionProtected(this, b.getLocation()) && !EarthAbility.getMovedEarth().containsKey(b)) {
					if (isWater(b)) break;
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
						flux.add(b.getLocation());
						Block left = b.getRelative(getLeftBlockFace(GeneralMethods.getCardinalDirection(blockdirection)), 1);
						expand(left);
						Block right = b.getRelative(getLeftBlockFace(GeneralMethods.getCardinalDirection(blockdirection)).getOppositeFace(), 1);
						expand(right);
					} else {
						break;
					}
				}
			}
			return true;
		}
		return false;
	}

	private void progressFlux() {
		for (Location location : flux) {
			if (flux.indexOf(location) <= step) {
				if (!blocks.containsKey(location.getBlock())) { //Make a new temp block if we haven't made one there before
					blocks.put(location.getBlock(), new TempBlock(location.getBlock(), LAVA, duration + cleanup, this));
				}

				//new RegenTempBlock(location.getBlock(), Material.LAVA, LAVA, duration + cleanup);
				this.location = location;
				if (flux.indexOf(location) == step) {
					Block above = location.getBlock().getRelative(BlockFace.UP);
					ParticleEffect.LAVA.display(above.getLocation(), 2, Math.random(), Math.random(), Math.random(), 0);
					applyDamageFromWave(above.getLocation());

					if (isPlant(above) || isSnow(above)) {
						final Block above2 = above.getRelative(BlockFace.UP);
						if (isPlant(above) || isSnow(above)) {
							TempBlock tb = new TempBlock(above, Material.AIR.createBlockData(), duration + cleanup, this);
							this.above.put(above, tb);
							if (isPlant(above2) && above2.getBlockData() instanceof Bisected) {
								TempBlock tb2 = new TempBlock(above2, Material.AIR.createBlockData(), duration + cleanup + 30_000, this);
								tb.addAttachedBlock(tb2);
							}
						}
					} else if (wave && isTransparent(above)) {
						new TempBlock(location.getBlock().getRelative(BlockFace.UP), LAVA, speed * 150L, this);
					}
				}
			}
		}
		if (step >= flux.size()) {
			wave = false;
			complete = true;
			time = System.currentTimeMillis();

			for (TempBlock tb : blocks.values()) { //Make sure they all revert at the same time because it looks nice
				long time = duration + cleanup + rand.nextInt(1000);
				tb.setRevertTime(time);

				if (this.above.containsKey(tb.getBlock().getRelative(BlockFace.UP))) {
					this.above.get(tb.getBlock().getRelative(BlockFace.UP)).setRevertTime(time);
				}
			}
		}
	}

	private void applyDamageFromWave(Location location) {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
			if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId()) {
				LivingEntity livingEntity = (LivingEntity) entity;

				DamageHandler.damageEntity(entity, damage, this);
				new FireDamageTimer(entity, player, this);

				Vector direction = livingEntity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
				Vector knockbackVelocity = direction.multiply(knockBack).setY(knockUp);

				livingEntity.setVelocity(knockbackVelocity);
			}
		}
	}

	private void expand(Block block) {
		if (block != null && block.getY() > block.getWorld().getMinHeight() && block.getY() < block.getWorld().getMaxHeight() && !RegionProtection.isRegionProtected(this, block.getLocation())) {
			if (EarthAbility.getMovedEarth().containsKey(block)){
				Information info = EarthAbility.getMovedEarth().get(block);
				if(!info.getBlock().equals(block)) {
					return;
				}
			}

			if (isWater(block)) return;
			while (!isEarthbendable(block)) {
				block = block.getRelative(BlockFace.DOWN);
				if (block.getY() < block.getWorld().getMinHeight() || block.getY() > block.getWorld().getMaxHeight()) {
					break;
				}
				if (isEarthbendable(block)) {
					break;
				}
			}

			while (!isTransparent(block.getRelative(BlockFace.UP))) {
				block = block.getRelative(BlockFace.UP);
				if (block.getY() < block.getWorld().getMinHeight() || block.getY() > block.getWorld().getMaxHeight()) {
					break;
				}
				if (isEarthbendable(block.getRelative(BlockFace.UP))) {
					break;
				}
			}

			if (isEarthbendable(block)) {
				flux.add(block.getLocation());
			}
		}
	}

	public BlockFace getLeftBlockFace(BlockFace forward) {
		switch (forward) {
			case NORTH:
				return BlockFace.WEST;
			case SOUTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.SOUTH;
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
		return "LavaFlux";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.LavaFlux.Description");
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getCleanup() {
		return cleanup;
	}

	public void setCleanup(long cleanup) {
		this.cleanup = cleanup;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public boolean isWave() {
		return wave;
	}

	public void setWave(boolean wave) {
		this.wave = wave;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public List<Location> getFlux() {
		return flux;
	}

	@Override
	public void load() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		if (config.get("Abilities.Earth.LavaFlux.Speed") instanceof String) {
			config.set("Abilities.Earth.LavaFlux.Speed", 1);
			JedCore.plugin.saveConfig();
			JedCore.plugin.reloadConfig();
		}
	}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.LavaFlux.Enabled");
	}
}