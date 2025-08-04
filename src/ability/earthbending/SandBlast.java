package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;

import com.jedk1.jedcore.util.TempFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class SandBlast extends SandAbility implements AddonAbility {

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.SELECT_RANGE)
	private double sourceRange;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute("MaxShots")
	private int maxBlasts;
	@Attribute(Attribute.DAMAGE)
	private static double damage;

	private Block source;
	private BlockData sourceData;
	private int blasts;
	private boolean blasting;
	private Vector direction;
	private TempBlock tempBlock;
	private final List<Entity> affectedEntities = new ArrayList<>();
	private final List<TempFallingBlock> fallingBlocks = new ArrayList<>();

	Random rand = new Random();

	public SandBlast(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		if (hasAbility(player, SandBlast.class)) {
			SandBlast sb = getAbility(player, SandBlast.class);
			sb.remove();
		}

		setFields();
		if (prepare()) {
			start();
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		cooldown = config.getLong("Abilities.Earth.SandBlast.Cooldown");
		sourceRange = config.getDouble("Abilities.Earth.SandBlast.SourceRange");
		range = config.getInt("Abilities.Earth.SandBlast.Range");
		maxBlasts = config.getInt("Abilities.Earth.SandBlast.MaxSandBlocks");
		damage = config.getDouble("Abilities.Earth.SandBlast.Damage");
	}

	private boolean prepare() {
		source = getEarthSourceBlock(sourceRange);

		if (source != null) {
			if (isSand(source) && ElementalAbility.isAir(source.getRelative(BlockFace.UP).getType())) {
				this.sourceData = source.getBlockData().clone();
				if (DensityShift.isPassiveSand(source)) {
					DensityShift.revertSand(source);
				}
				tempBlock = new TempBlock(source, Material.SANDSTONE.createBlockData());
				return true;
			}
		}
		return false;
	}

	@Override
	public void progress() {
		if (!hasAbility(player, SandBlast.class)) {
			return;
		}
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (player.getWorld() != source.getWorld()) {
			remove();
			return;
		}
		if (blasting) {
			if (blasts <= maxBlasts) {
				blastSand();
				blasts++;
			} else {
				if (TempFallingBlock.getFromAbility(this).isEmpty()) {
					remove();
					return;
				}
			}
			affect();
		}
	}

	@Override
	public void remove() {
		if (this.tempBlock != null) {
			this.tempBlock.revertBlock();
		}
		super.remove();
	}

	public static void blastSand(Player player) {
		if (hasAbility(player, SandBlast.class)) {
			SandBlast sb = getAbility(player, SandBlast.class);
			if (sb.blasting) {
				return;
			}
			sb.blastSand();
		}
	}

	private void blastSand() {
		if (!blasting) {
			blasting = true;
			direction = GeneralMethods.getDirection(source.getLocation().clone().add(0, 1, 0), GeneralMethods.getTargetedLocation(player, range)).multiply(0.07);
			this.bPlayer.addCooldown(this);
		}
		tempBlock.revertBlock();

		//FallingBlock fblock = source.getWorld().spawnFallingBlock(source.getLocation().clone().add(0, 1, 0), source.getType(), source.getData());

		if (rand.nextInt(2) == 0) {
			DensityShift.playSandbendingSound(source.getLocation().add(0, 1, 0));
		}

		double x = rand.nextDouble() / 10;
		double z = rand.nextDouble() / 10;

		x = (rand.nextBoolean()) ? -x : x;
		z = (rand.nextBoolean()) ? -z : z;

		//fblock.setVelocity(direction.clone().add(new Vector(x, 0.2, z)));
		//fblock.setDropItem(false);
		//fblocks.put(fblock, player);

		fallingBlocks.add(new TempFallingBlock(source.getLocation().add(0, 1, 0), sourceData, direction.clone().add(new Vector(x, 0.2, z)), this));

	}

	public void affect() {
		for (TempFallingBlock tfb : TempFallingBlock.getFromAbility(this)) {
			FallingBlock fblock = tfb.getFallingBlock();
			if (fblock.isDead()) {
				tfb.remove();
				continue;
			}

			if (RegionProtection.isRegionProtected(player, fblock.getLocation(), this)) {
				tfb.remove();
				continue;
			}

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(fblock.getLocation(), 1.5)) {
				if (entity instanceof LivingEntity && !(entity instanceof ArmorStand)) {
					if (entity == this.player) continue;
					if (affectedEntities.contains(entity)) continue;

					if (!entity.isDead()) {
						DamageHandler.damageEntity(entity, damage, this);

						affectedEntities.add(entity);

						LivingEntity le = (LivingEntity) entity;
						if (le.hasPotionEffect(PotionEffectType.BLINDNESS)) {
							le.removePotionEffect(PotionEffectType.BLINDNESS);
						}

						le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
					}
				}
			}
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
	public List<Location> getLocations() {
		return fallingBlocks.stream().map(TempFallingBlock::getLocation).collect(Collectors.toList());
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.isRemovingFirst()) {
			Location location = collision.getLocationFirst();

			Optional<TempFallingBlock> collidedObject = fallingBlocks.stream().filter(temp -> temp.getLocation().equals(location)).findAny();

			if (collidedObject.isPresent()) {
				fallingBlocks.remove(collidedObject.get());
				collidedObject.get().remove();
			}
		}
	}

	@Override
	public String getName() {
		return "SandBlast";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.SandBlast.Description");
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public double getSourceRange() {
		return sourceRange;
	}

	public void setSourceRange(double sourceRange) {
		this.sourceRange = sourceRange;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public int getMaxBlasts() {
		return maxBlasts;
	}

	public void setMaxBlasts(int maxBlasts) {
		this.maxBlasts = maxBlasts;
	}

	public static double getDamage() {
		return damage;
	}

	public static void setDamage(double damage) {
		SandBlast.damage = damage;
	}

	public Block getSource() {
		return source;
	}

	public void setSource(Block source) {
		this.source = source;
	}

	public BlockData getSourceData() {
		return sourceData;
	}

	public void setSourceData(BlockData sourceData) {
		this.sourceData = sourceData;
	}

	public int getBlasts() {
		return blasts;
	}

	public void setBlasts(int blasts) {
		this.blasts = blasts;
	}

	public boolean isBlasting() {
		return blasting;
	}

	public void setBlasting(boolean blasting) {
		this.blasting = blasting;
	}

	public Vector getDirection() {
		return direction;
	}

	public void setDirection(Vector direction) {
		this.direction = direction;
	}

	public TempBlock getTempBlock() {
		return tempBlock;
	}

	public void setTempBlock(TempBlock tempBlock) {
		this.tempBlock = tempBlock;
	}

	public List<Entity> getAffectedEntities() {
		return affectedEntities;
	}

	public List<TempFallingBlock> getFallingBlocks() {
		return fallingBlocks;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.SandBlast.Enabled");
	}
}
