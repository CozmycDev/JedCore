package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.collision.AABB;
import com.jedk1.jedcore.collision.CollisionDetector;
import com.jedk1.jedcore.collision.CollisionUtil;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.BlockUtil;
import com.jedk1.jedcore.util.TempFallingBlock;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class EarthKick extends EarthAbility implements AddonAbility {
	private final List<TempFallingBlock> temps = new ArrayList<>();

	private BlockData materialData;
	private Location location;
	private final Random rand = new Random();

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute("MaxShots")
	private int earthBlocks;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.DAMAGE)
	private double metalDmg;
	@Attribute("CollisionRadius")
	private double entityCollisionRadius;
	private Block block;

	private boolean multipleHits;
	private int sourceRange;
	private int spread;
	private double velocity;
	private boolean allowMetal;

	private Set<UUID> hitEntities = new HashSet<>();

	public EarthKick(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		setFields();
		location = player.getLocation();
		if ((player.getLocation().getPitch() > -5) && prepare()) {
			if (RegionProtection.isRegionProtected(this, block.getLocation())) {
				return;
			}
			launchBlocks();
			start();
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		cooldown = config.getLong("Abilities.Earth.EarthKick.Cooldown");
		earthBlocks = config.getInt("Abilities.Earth.EarthKick.EarthBlocks");
		damage = config.getDouble("Abilities.Earth.EarthKick.Damage.Normal");
		metalDmg = config.getDouble("Abilities.Earth.EarthKick.Damage.Metal");
		entityCollisionRadius = config.getDouble("Abilities.Earth.EarthKick.EntityCollisionRadius");

		multipleHits = config.getBoolean("Abilities.Earth.EarthKick.MultipleHits");
		sourceRange = config.getInt("Abilities.Earth.EarthKick.SourceRange");
		spread = config.getInt("Abilities.Earth.EarthKick.Spread");
		velocity = config.getDouble("Abilities.Earth.EarthKick.Velocity");
		allowMetal = config.getBoolean("Abilities.Earth.EarthKick.AllowMetal");

		if (entityCollisionRadius < 1.0) {
			entityCollisionRadius = 1.0;
		}
	}

	private boolean prepare() {
		block = player.getTargetBlock(getTransparentMaterialSet(), sourceRange);

		if (!isEarthbendable(player, block)) {
			return false;
		}

		if (TempBlock.isTempBlock(block)) {
			TempBlock.get(block).revertBlock();
		}

		if (DensityShift.isPassiveSand(block)) {
			DensityShift.revertSand(block);
		}

		if (block != null && (allowMetal || !isMetal(block))) {
			materialData = block.getBlockData().clone();
			location.setX(block.getX() + 0.5);
			location.setY(block.getY());
			location.setZ(block.getZ() + 0.5);

			return true;
		}

		return false;
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			remove();
			return;
		}

		bPlayer.addCooldown(this);
		track();

		if (temps.isEmpty()) {
			remove();
		}
	}

	private void launchBlocks() {
		if (getMovedEarth().containsKey(block)) {
			block.setType(Material.AIR);
		}
		if (block.getType() != Material.AIR) {
			TempBlock air = new TempBlock(block, Material.AIR);
			air.setRevertTime(5000L);
		}

		location.setPitch(0);
		location.add(location.getDirection());

		if (!isAir(location.getBlock().getType())) {
			location.setY(location.getY() + 1.0);
		}

		ParticleEffect.CRIT.display(location, 10, Math.random(), Math.random(), Math.random(), 0.1);

		int yaw = Math.round(location.getYaw());

		playEarthbendingSound(location);

		for (int i = 0; i < earthBlocks; i++) {
			location.setYaw(yaw + rand.nextInt((spread * 2) + 1) - spread);
			location.setPitch(rand.nextInt(25) - 45);

			Vector v = location.clone().add(0, 0.8, 0).getDirection().normalize();
			Location location1 = location.clone().add(new Vector(v.getX() * 2, v.getY(), v.getZ() * 2));
			Vector dir = location1.setDirection(location.getDirection()).getDirection().multiply(velocity);

			temps.add(new TempFallingBlock(location, materialData, dir, this));
		}
	}

	public void track() {
		List<TempFallingBlock> destroy = new ArrayList<>();

		for (TempFallingBlock tfb : temps) {
			FallingBlock fb = tfb.getFallingBlock();

			if (fb == null || fb.isDead()) {
				destroy.add(tfb);
				continue;
			}

			for (int i = 0; i < 2; i++) {
				ParticleEffect.BLOCK_CRACK.display(fb.getLocation(), 1, 0.0, 0.0, 0.0, 0.1, materialData);
				ParticleEffect.BLOCK_CRACK.display(fb.getLocation(), 1, 0.0, 0.0, 0.0, 0.2, materialData);
			}

			AABB collider = BlockUtil.getFallingBlockBoundsFull(fb).scale(entityCollisionRadius * 2.0);

			CollisionDetector.checkEntityCollisions(player, collider, (entity) -> {
				UUID uuid = entity.getUniqueId();
				if (this.multipleHits || hitEntities.add(uuid)) {
					DamageHandler.damageEntity(entity, isMetal(fb.getBlockData().getMaterial()) ? metalDmg : damage, this);
				}
				return false;
			});
		}

		temps.removeAll(destroy);
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
	public List<Location> getLocations() {
		return temps.stream().map(TempFallingBlock::getLocation).collect(toList());
	}

	@Override
	public double getCollisionRadius() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getDouble("Abilities.Earth.EarthKick.AbilityCollisionRadius");
	}

	@Override
	public void handleCollision(Collision collision) {
		CollisionUtil.handleFallingBlockCollisions(collision, temps);
	}

	@Override
	public String getName() {
		return "EarthKick";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.EarthKick.Description");
	}

	public List<TempFallingBlock> getTemps() {
		return temps;
	}

	public BlockData getMaterialData() {
		return materialData;
	}

	public void setMaterialData(BlockData materialData) {
		this.materialData = materialData;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public int getEarthBlocksQuantity() {
		return earthBlocks;
	}

	public void setEarthBlocksQuantity(int earthBlocks) {
		this.earthBlocks = earthBlocks;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public double getMetalDmg() {
		return metalDmg;
	}

	public void setMetalDmg(double metalDmg) {
		this.metalDmg = metalDmg;
	}

	public double getEntityCollisionRadius() {
		return entityCollisionRadius;
	}

	public void setEntityCollisionRadius(double entityCollisionRadius) {
		this.entityCollisionRadius = entityCollisionRadius;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.EarthKick.Enabled");
	}
}
