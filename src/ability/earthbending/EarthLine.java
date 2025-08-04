package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.policies.removal.*;
import com.jedk1.jedcore.util.RegenTempBlock;

import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.passive.DensityShift;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;

import com.projectkorra.projectkorra.util.TempFallingBlock;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class EarthLine extends EarthAbility implements AddonAbility {

	private Location location;
	private Location endLocation;
	private Block sourceBlock;
	private TempBlock sourceTempBlock;
	private Material sourceType;
	private boolean progressing;
	private boolean hitted;
	private int goOnAfterHit;
	private long removalTime = -1;

	private long useCooldown;
	private long prepareCooldown;
	@Attribute(Attribute.DURATION)
	private long maxDuration;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.SELECT_RANGE)
	private double prepareRange;
	private double sourceKeepRange;
	@Attribute(Attribute.RADIUS)
	private int affectingRadius;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	private boolean allowChangeDirection;
	private CompositeRemovalPolicy removalPolicy;

	public EarthLine(Player player) {
		super(player);

		if (!isEnabled()) return;

		if (!bPlayer.canBend(this)) {
			return;
		}
		goOnAfterHit = 1;

		setFields();
		if (prepare()) {
			start();
			if (!isRemoved() && prepareCooldown != 0) {
				bPlayer.addCooldown(this, prepareCooldown);
			}
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		this.removalPolicy = new CompositeRemovalPolicy(this,
				new CannotBendRemovalPolicy(this.bPlayer, this, true, true),
				new IsOfflineRemovalPolicy(this.player),
				new IsDeadRemovalPolicy(this.player),
				new SwappedSlotsRemovalPolicy<>(bPlayer, EarthLine.class)
		);

		this.removalPolicy.load(config);

		useCooldown = config.getLong("Abilities.Earth.EarthLine.Cooldown");
		prepareCooldown = config.getLong("Abilities.Earth.EarthLine.PrepareCooldown");
		range = config.getInt("Abilities.Earth.EarthLine.Range");
		prepareRange = config.getDouble("Abilities.Earth.EarthLine.PrepareRange");
		sourceKeepRange = config.getDouble("Abilities.Earth.EarthLine.SourceKeepRange");
		affectingRadius = config.getInt("Abilities.Earth.EarthLine.AffectingRadius");
		damage = config.getDouble("Abilities.Earth.EarthLine.Damage");
		allowChangeDirection = config.getBoolean("Abilities.Earth.EarthLine.AllowChangeDirection");
		maxDuration = config.getLong("Abilities.Earth.EarthLine.MaxDuration");
	}

    public boolean prepare() {
        final Block block = getEarthSourceBlock(this.range);
        if (block == null || !this.isEarthbendable(block)) {
            return false;
        } else if (TempBlock.isTempBlock(block) && !EarthAbility.isBendableEarthTempBlock(block)) {
            return false;
        }

        boolean selectedABlockInUse = false;
        for (final EarthLine el : getAbilities(this.player, EarthLine.class)) {
            if (!el.progressing) {
                el.remove();
            } else if (block.equals(el.sourceBlock)) {
                selectedABlockInUse = true;
            }
        }

        if (selectedABlockInUse) {
            return false;
        }

        if (block.getLocation().distanceSquared(this.player.getLocation()) > this.prepareRange * this.prepareRange) {
            return false;
        }

        this.sourceBlock = block;
        this.focusBlock();
        return true;
    }

	private void focusBlock() {
		if (DensityShift.isPassiveSand(this.sourceBlock)) {
			DensityShift.revertSand(this.sourceBlock);
		}

		if (this.sourceBlock.getType() == Material.SAND) {
			this.sourceType = Material.SAND;
			sourceTempBlock = new TempBlock(sourceBlock, Material.SANDSTONE.createBlockData());
			//this.sourceBlock.setType(Material.SANDSTONE);
		} else if (this.sourceBlock.getType() == Material.RED_SAND) {
			this.sourceType = Material.RED_SAND;
			sourceTempBlock = new TempBlock(sourceBlock, Material.RED_SANDSTONE.createBlockData());
			//this.sourceBlock.setType(Material.RED_SANDSTONE);
		} else if (this.sourceBlock.getType() == Material.STONE) {
			//this.sourceBlock.setType(Material.COBBLESTONE);
			this.sourceType = Material.STONE;
			sourceTempBlock = new TempBlock(sourceBlock, Material.COBBLESTONE.createBlockData());
		} else {
			this.sourceType = this.sourceBlock.getType();
			//this.sourceBlock.setType(Material.STONE);
			sourceTempBlock = new TempBlock(sourceBlock, Material.STONE.createBlockData());
		}

		this.location = this.sourceBlock.getLocation();
	}
	
	private void unfocusBlock() {
		//sourceBlock.setType(sourceType);
		sourceTempBlock.revertBlock();
	}

	@Override
	public void remove() {
//		if (sourceBlock != null && sourceBlock.getType() != Material.AIR) {
//			sourceBlock.setType(sourceType); // Ensure no duplication of the source block
//		}
		sourceTempBlock.revertBlock();
		super.remove();
	}

	private static Location getTargetLocation(Player player) {
		ConfigurationSection config = JedCoreConfig.getConfig(player);
		double range = config.getInt("Abilities.Earth.EarthLine.Range");
		Entity target = GeneralMethods.getTargetedEntity(player, range, player.getNearbyEntities(range, range, range));
		Location location;
		if (target == null) {
			location = GeneralMethods.getTargetedLocation(player, range);
		} else {
			location = ((LivingEntity) target).getEyeLocation();
		}
		return location;
	}

	public void shootLine(Location endLocation) {
		if (useCooldown != 0 && bPlayer.getCooldown(this.getName()) < useCooldown) bPlayer.addCooldown(this, useCooldown);
		if (maxDuration > 0) removalTime = System.currentTimeMillis() + maxDuration;
		this.endLocation = endLocation;
		progressing = true;
		sourceBlock.getWorld().playEffect(sourceBlock.getLocation(), Effect.GHAST_SHOOT, 0, 10);
	}

	public static void shootLine(Player player) {
		if (hasAbility(player, EarthLine.class)) {
			EarthLine el = getAbility(player, EarthLine.class);
			if (!el.progressing) {
				el.shootLine(getTargetLocation(player));
			}
		}
	}
	
	private boolean sourceOutOfRange() {
		return sourceBlock == null || sourceBlock.getLocation().add(0.5, 0.5, 0.5).distanceSquared(player.getLocation()) > sourceKeepRange * sourceKeepRange || sourceBlock.getWorld() != player.getWorld();
	}

	public void progress() {
		if (!progressing) {
			if (sourceOutOfRange()) {
				unfocusBlock();
				remove();
			}
			return;
		}

		if (removalPolicy.shouldRemove()) {
			remove();
			return;
		}

		if (sourceBlock == null || RegionProtection.isRegionProtected(this, location)) {
			remove();
			return;
		}

		if (removalTime > -1 && System.currentTimeMillis() > removalTime) {
			remove();
			return;
		}
		
		if (sourceOutOfRange()) {
			remove();
			return;
		}

		if (RegionProtection.isRegionProtected(player, location, this)) {
			remove();
			return;
		}

		if (allowChangeDirection && player.isSneaking() && bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthLine")) {
			endLocation = getTargetLocation(player);
		}

		double x1 = endLocation.getX();
		double z1 = endLocation.getZ();
		double x0 = sourceBlock.getX();
		double z0 = sourceBlock.getZ();
		Vector looking = new Vector(x1 - x0, 0.0D, z1 - z0);
		Vector push = new Vector(x1 - x0, 0.34999999999999998D, z1 - z0);
		if (location.distance(sourceBlock.getLocation()) < range) {
			Material cloneType = location.getBlock().getType();
			Location locationYUP = location.getBlock().getLocation().clone().add(0.5, 0.1, 0.5);

			playEarthbendingSound(location);

			if (isEarthbendable(location.getBlock())) {
				//new RegenTempBlock(location.getBlock(), Material.AIR, Material.AIR.createBlockData(), 700L);
				new TempBlock(location.getBlock(), Material.AIR.createBlockData(), 700L);
				new TempFallingBlock(locationYUP, cloneType.createBlockData(), new Vector(0.0, 0.35, 0.0), this);
			}

			location.add(looking.normalize());

			if (!climb()) {
				remove();
				return;
			}

			if (hitted) {
				if (goOnAfterHit != 0) {
					goOnAfterHit--;
				} else {
					remove();
					return;
				}
			} else {
				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, affectingRadius)) {
					if (RegionProtection.isRegionProtected(this, entity.getLocation()) || ((entity instanceof Player) && Commands.invincible.contains(entity.getName()))){
						return;
					}
					if ((entity instanceof LivingEntity) && entity.getEntityId() != player.getEntityId()) {
						GeneralMethods.setVelocity(this, entity, push.normalize().multiply(2));
						DamageHandler.damageEntity(entity, damage, this);
						hitted = true;
					}
				}
			}
		} else {
			remove();
			return;
		}

		if (!isEarthbendable(player, location.getBlock()) && !isTransparent(location.getBlock())) {
			remove();
		}
	}

	private boolean climb() {
		Block above = location.getBlock().getRelative(BlockFace.UP);

		if (!isTransparent(above)) {
			// Attempt to climb since the current location has a block above it.
			location.add(0, 1, 0);
			above = location.getBlock().getRelative(BlockFace.UP);

			// The new location must be earthbendable and have something transparent above it.
			return isEarthbendable(location.getBlock()) && isTransparent(above);
		} else if (isTransparent(location.getBlock()) ) {
			// Attempt to fall since the current location is transparent and the above block was transparent.
			location.add(0, -1, 0);

			// The new location must be earthbendable and we already know the block above it is transparent.
			return isEarthbendable(location.getBlock());
		}

		return true;
	}
	
	@Override
	public long getCooldown() {
		return useCooldown;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String getName() {
		return "EarthLine";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.EarthLine.Description");
	}

	public Location getEndLocation() {
		return endLocation;
	}

	public void setEndLocation(Location endLocation) {
		this.endLocation = endLocation;
	}

	public Block getSourceBlock() {
		return sourceBlock;
	}

	public void setSourceBlock(Block sourceBlock) {
		this.sourceBlock = sourceBlock;
	}

	public Material getSourceType() {
		return sourceType;
	}

	public void setSourceType(Material sourceType) {
		this.sourceType = sourceType;
	}

	public boolean isProgressing() {
		return progressing;
	}

	public void setProgressing(boolean progressing) {
		this.progressing = progressing;
	}

	public int getGoOnAfterHit() {
		return goOnAfterHit;
	}

	public void setGoOnAfterHit(int goOnAfterHit) {
		this.goOnAfterHit = goOnAfterHit;
	}

	public long getRemovalTime() {
		return removalTime;
	}

	public void setRemovalTime(long removalTime) {
		this.removalTime = removalTime;
	}

	public long getUseCooldown() {
		return useCooldown;
	}

	public void setUseCooldown(long useCooldown) {
		this.useCooldown = useCooldown;
	}

	public long getPrepareCooldown() {
		return prepareCooldown;
	}

	public void setPrepareCooldown(long prepareCooldown) {
		this.prepareCooldown = prepareCooldown;
	}

	public long getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(long maxDuration) {
		this.maxDuration = maxDuration;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public double getPrepareRange() {
		return prepareRange;
	}

	public void setPrepareRange(double prepareRange) {
		this.prepareRange = prepareRange;
	}

	public double getSourceKeepRange() {
		return sourceKeepRange;
	}

	public void setSourceKeepRange(double sourceKeepRange) {
		this.sourceKeepRange = sourceKeepRange;
	}

	public int getAffectingRadius() {
		return affectingRadius;
	}

	public void setAffectingRadius(int affectingRadius) {
		this.affectingRadius = affectingRadius;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public boolean isAllowChangeDirection() {
		return allowChangeDirection;
	}

	public void setAllowChangeDirection(boolean allowChangeDirection) {
		this.allowChangeDirection = allowChangeDirection;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.EarthLine.Enabled");
	}
}