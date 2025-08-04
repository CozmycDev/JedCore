package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.collision.AABB;
import com.jedk1.jedcore.collision.CollisionDetector;
import com.jedk1.jedcore.collision.CollisionUtil;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.BlockUtil;
import com.jedk1.jedcore.util.ThreadUtil;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.jedk1.jedcore.util.TempFallingBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class EarthShard extends EarthAbility implements AddonAbility {

	@Attribute(Attribute.RANGE)
	public static int range;

	public static int abilityRange;

	@Attribute(Attribute.DAMAGE)
	public static double normalDmg, metalDmg;

	@Attribute("MaxShots")
	public static int maxShards;

	@Attribute(Attribute.COOLDOWN)
	public static long cooldown;

	private boolean isThrown = false;
	private final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
	private Location origin;
	private double abilityCollisionRadius, entityCollisionRadius;
	private boolean allowKnockup;
	private double knockupVelocity;

	private final List<TempBlock> tblockTracker = new ArrayList<>();
	private final List<TempBlock> readyBlocksTracker = new ArrayList<>();
	private final List<TempFallingBlock> fallingBlocks = new ArrayList<>();

	public EarthShard(Player player) {
		super(player);
		if (!bPlayer.canBend(this)) return;

		if (hasAbility(player, EarthShard.class)) {
			for (EarthShard es : getAbilities(player, EarthShard.class)) {
				if (es.isThrown && System.currentTimeMillis() - es.getStartTime() >= 20000) {
					es.remove();
				} else {
					es.select();
					return;
				}
			}
		}

		setFields();
		origin = player.getLocation().clone();
		raiseEarthBlock(getEarthSourceBlock(range));
		start();
	}

	private void setFields() {
		ConfigurationSection c = JedCoreConfig.getConfig(player);
		range = c.getInt("Abilities.Earth.EarthShard.PrepareRange");
		abilityRange = c.getInt("Abilities.Earth.EarthShard.AbilityRange");
		normalDmg = c.getDouble("Abilities.Earth.EarthShard.Damage.Normal");
		metalDmg = c.getDouble("Abilities.Earth.EarthShard.Damage.Metal");
		maxShards = c.getInt("Abilities.Earth.EarthShard.MaxShards");
		cooldown = c.getLong("Abilities.Earth.EarthShard.Cooldown");
		abilityCollisionRadius = c.getDouble("Abilities.Earth.EarthShard.AbilityCollisionRadius");
		entityCollisionRadius = c.getDouble("Abilities.Earth.EarthShard.EntityCollisionRadius");
		allowKnockup = c.getBoolean("Abilities.Earth.EarthShard.KnockUp.Allow");
		knockupVelocity = c.getDouble("Abilities.Earth.EarthShard.KnockUp.Velocity");
	}

	public void select() {
		raiseEarthBlock(getEarthSourceBlock(range));
	}

	public void raiseEarthBlock(Block block) {
		if (block == null || tblockTracker.size() >= maxShards) return;

		Vector blockVec = block.getLocation().toVector(); blockVec.setY(0);
		for (TempBlock tb : tblockTracker) {
			Vector tempVec = tb.getLocation().toVector(); tempVec.setY(0);
			if (tempVec.equals(blockVec)) return;
		}

		for (int i = 1; i <= 3; i++) {
			if (!isTransparent(block.getRelative(BlockFace.UP, i))) return;
		}

		if (!isEarthbendable(block)) return;

		if (isMetal(block)) {
			playMetalbendingSound(block.getLocation());
		} else {
			ParticleEffect.BLOCK_CRACK.display(block.getLocation().add(0, 1, 0), 20, 0, 0, 0, 0, block.getBlockData());
			playEarthbendingSound(block.getLocation());
		}

		Material mat = getCorrectType(block);
		if (DensityShift.isPassiveSand(block)) {
			DensityShift.revertSand(block);
		}

		Location loc = block.getLocation().add(0.5, 0, 0.5);
		TempFallingBlock tfb = new TempFallingBlock(loc, mat.createBlockData(), new Vector(0, 0.8, 0), this);
		fallingBlocks.add(tfb);
		tblockTracker.add(new TempBlock(block, Material.AIR.createBlockData()));

		if (allowKnockup) {
			for (Entity e : block.getWorld().getNearbyEntities(block.getLocation(), 1.5, 1.5, 1.5)) {
				if (!(e instanceof FallingBlock) && e.getLocation().getY() >= block.getY()) {
					ThreadUtil.ensureEntity(e, () -> e.setVelocity(e.getVelocity().add(new Vector(0, knockupVelocity, 0))));
				}
			}
		}
	}

	private Material getCorrectType(Block b) {
		switch (b.getType()) {
			case SAND: return Material.SANDSTONE;
			case RED_SAND: return Material.RED_SANDSTONE;
			case GRAVEL: return Material.COBBLESTONE;
			default:
				if (b.getType().name().endsWith("CONCRETE_POWDER")) {
					return Material.getMaterial(b.getType().name().replace("_POWDER", ""));
				}
				return b.getType();
		}
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline() || player.isDead()) {
			remove(); return;
		}

		if (!isThrown) {
			if (!bPlayer.canBendIgnoreCooldowns(this) || tblockTracker.isEmpty()) {
				remove(); return;
			}

			for (TempFallingBlock tfb : new ArrayList<>(TempFallingBlock.getFromAbility(this))) {
				FallingBlock fb = tfb.getFallingBlock();
				if (fb == null || fb.isDead() || fb.getLocation().getBlockY() == origin.getBlockY() + 2) {
					tfb.remove();
					readyBlocksTracker.add(new TempBlock(fb.getLocation().getBlock(), fb.getBlockData()));
				}
			}
		} else {
			for (TempFallingBlock tfb : new ArrayList<>(TempFallingBlock.getFromAbility(this))) {
				FallingBlock fb = tfb.getFallingBlock();
				if (fb == null) continue;
				AABB collider = BlockUtil.getFallingBlockBoundsFull(fb).scale(entityCollisionRadius * 2.0);
				CollisionDetector.checkEntityCollisions(player, collider, e -> {
					DamageHandler.damageEntity(e, isMetal(fb.getBlockData().getMaterial()) ? metalDmg : normalDmg, this);
					if (e instanceof LivingEntity le) le.setNoDamageTicks(0);
					ParticleEffect.BLOCK_CRACK.display(fb.getLocation(), 20, 0, 0, 0, 0, fb.getBlockData());
					tfb.remove();
					return false;
				});
			}

			if (TempFallingBlock.getFromAbility(this).isEmpty()) {
				remove();
			}
		}
	}

	public void throwShard() {
		if (isThrown || readyBlocksTracker.isEmpty()) return;

		Location targetLoc = GeneralMethods.getTargetedLocation(player, abilityRange);
		Entity ent = GeneralMethods.getTargetedEntity(player, abilityRange, new ArrayList<>());
		if (ent != null) targetLoc = ent.getLocation();

		for (TempBlock tb : readyBlocksTracker) {
			Vector vel = GeneralMethods.getDirection(tb.getLocation(), targetLoc)
					.normalize().multiply(2).add(new Vector(0, 0.2, 0));
			TempFallingBlock tfb = new TempFallingBlock(tb.getLocation(), tb.getBlockData(), vel, this);
			fallingBlocks.add(tfb);
		}

		revertBlocks();
		isThrown = true;
		BendingPlayer.getBendingPlayer(player).addCooldown(this);
	}

	private void revertBlocks() {
		tblockTracker.forEach(TempBlock::revertBlock);
		readyBlocksTracker.forEach(TempBlock::revertBlock);
		tblockTracker.clear();
		readyBlocksTracker.clear();
	}

	@Override
	public void remove() {
		TempFallingBlock.getFromAbility(this).forEach(TempFallingBlock::remove);
		revertBlocks();
		super.remove();
	}

	@Override
	public long getCooldown() { return cooldown; }

	@Override
	public Location getLocation() { return null; }

	@Override
	public List<Location> getLocations() {
		return fallingBlocks.stream().map(TempFallingBlock::getLocation).collect(Collectors.toList());
	}

	public void handleCollision(Collision collision) {
		CollisionUtil.handleFallingBlockCollisions(collision, fallingBlocks);
	}

	@Override
	public double getCollisionRadius() { return abilityCollisionRadius; }

	@Override
	public String getName() { return "EarthShard"; }

	@Override
	public boolean isHarmlessAbility() { return false; }

	@Override
	public boolean isSneakAbility() { return true; }

	@Override
	public String getAuthor() { return JedCore.dev; }

	@Override
	public String getVersion() { return JedCore.version; }

	@Override
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.EarthShard.Description");
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.EarthShard.Enabled");
	}
}
