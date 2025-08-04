package com.jedk1.jedcore.ability.earthbending.combo;

import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.MaterialUtil;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.lava.LavaFlow;

import com.jedk1.jedcore.JedCore;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import com.projectkorra.projectkorra.util.TempFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class MagmaBlast extends LavaAbility implements AddonAbility, ComboAbility {
	private static final int PARTICLE_COUNT = 20;
	private static final int RAISE_HEIGHT = 3;
	private static final Random rand = new Random();

	private final Set<TempFallingBlock> sources = new HashSet<>();
	private final List<TempBlock> blocks = new ArrayList<>();
	private final List<TempFallingBlock> firedBlocks = new ArrayList<>();

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long maxDuration;
	private long shotCooldown;
	@Attribute("MaxSources")
	private int maxSources;
	@Attribute(Attribute.SELECT_RANGE)
	private int sourceRange;
	@Attribute(Attribute.SELECT_RANGE)
	private double selectRange;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	private double fireSpeed;
	// How far away the player is allowed to be from the sources before the ability is destroyed.
	private double maxDistanceFromSources;
	private float explosionRadius = 2.0f;
	// This will destroy the instance if LavaFlow is on cooldown.
	private boolean requireLavaFlow;
	private boolean playerCollisions;
	private boolean entitySelection;

	private Location origin;
	private int counter;
	private long canLavaFlowTime;
	private long lastShot;
	private boolean stopFiring;

	public MagmaBlast(Player player) {
		super(player);
		setFields();

		if (!bPlayer.canBendIgnoreBinds(this)) {
			return;
		}

		origin = player.getLocation().clone();

		if (raiseSources()) {
			start();
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		maxSources = config.getInt("Abilities.Earth.EarthCombo.MagmaBlast.MaxShots");
		sourceRange = config.getInt("Abilities.Earth.EarthCombo.MagmaBlast.SearchRange");
		damage = config.getDouble("Abilities.Earth.EarthCombo.MagmaBlast.ImpactDamage");
		cooldown = config.getInt("Abilities.Earth.EarthCombo.MagmaBlast.Cooldown");
		requireLavaFlow = config.getBoolean("Abilities.Earth.EarthCombo.MagmaBlast.RequireLavaFlow");
		playerCollisions = config.getBoolean("Abilities.Earth.EarthCombo.MagmaBlast.PlayerCollisions");
		entitySelection = config.getBoolean("Abilities.Earth.EarthCombo.MagmaBlast.EntitySelection");
		selectRange = config.getDouble("Abilities.Earth.EarthCombo.MagmaBlast.SelectRange");
		explosionRadius = (float) config.getDouble("Abilities.Earth.EarthCombo.MagmaBlast.ExplosionRadius");
		fireSpeed = config.getDouble("Abilities.Earth.EarthCombo.MagmaBlast.FireSpeed");
		maxDuration = config.getLong("Abilities.Earth.EarthCombo.MagmaBlast.MaxDuration");
		maxDistanceFromSources = config.getLong("Abilities.Earth.EarthCombo.MagmaBlast.MaxDistanceFromSources");
		shotCooldown = config.getLong("Abilities.Earth.EarthCombo.MagmaBlast.ShotCooldown");
	}

	// Select random nearby earth blocks as sources and raise them in the air.
	private boolean raiseSources() {
		List<Block> potentialBlocks = GeneralMethods.getBlocksAroundPoint(origin, sourceRange).stream().filter(ElementalAbility::isEarth).collect(Collectors.toList());

		Collections.shuffle(potentialBlocks);

		for (Block newSource : potentialBlocks) {
			if (!isValidSource(newSource)) continue;

			newSource.getWorld().playSound(newSource.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.3f);
			sources.add(new TempFallingBlock(newSource.getLocation().add(0, 1, 0), Material.MAGMA_BLOCK.createBlockData(), new Vector(0, 0.9, 0), this));

			if (sources.size() >= maxSources) {
				break;
			}
		}

		return !sources.isEmpty();
	}

	// Checks to make sure the source block has room to fly upwards.
	private boolean isValidSource(Block block) {
		for (int i = 0; i <= RAISE_HEIGHT; ++i) {
			if (!MaterialUtil.isTransparent(block.getRelative(BlockFace.UP, i + 1)) || block.isLiquid()) {
				return false;
			}
		}

		return true;
	}

	public boolean shouldBlockLavaFlow() {
		long time = System.currentTimeMillis();
		return time < canLavaFlowTime;
	}

	@Override
	public void progress() {
		stopFiring = false;
		if (player == null || !player.isOnline() || player.isDead()) {
			remove();
			return;
		}

		if (System.currentTimeMillis() > this.getStartTime() + maxDuration) {
			remove();
			return;
		}

		if (!bPlayer.canBendIgnoreBinds(this) || !(bPlayer.getBoundAbility() instanceof LavaFlow)) {
			remove();
			return;
		}

		if (requireLavaFlow && !bPlayer.canBend(getAbility("LavaFlow"))) {
			remove();
			return;
		}

		displayAnimation();
		handleSources();

		if (playerCollisions) {
			doPlayerCollisions();
		}

		if (sources.isEmpty() && firedBlocks.isEmpty() && blocks.isEmpty()) {
			remove();
			return;
		}

		if (hasBlocks() && this.player.getLocation().distanceSquared(origin) > maxDistanceFromSources * maxDistanceFromSources) {
			remove();
		}
	}

	@Override
	public void remove() {
		bPlayer.addCooldown(this);
		super.remove();

		for (TempFallingBlock ftb : sources) {
			ftb.remove();
		}

		for (TempBlock tb : blocks) {
			tb.revertBlock();
		}

		for (TempFallingBlock tfb : firedBlocks) {
			tfb.remove();
		}
	}

	private void handleSources() {
		if (sources.isEmpty()) return;

		for (Iterator<TempFallingBlock> iter = sources.iterator(); iter.hasNext();) {
			TempFallingBlock tfb = iter.next();

			if (tfb.getLocation().getBlockY() >= (origin.getBlockY() + RAISE_HEIGHT)) {
				blocks.add(new TempBlock(tfb.getLocation().getBlock(), Material.MAGMA_BLOCK.createBlockData()));
				iter.remove();
				tfb.remove();
			}
		}
	}

	private void displayAnimation() {
		if (++counter == 3) {
			counter = 0;
		} else {
			return;
		}

		for (Iterator<TempFallingBlock> iterator = firedBlocks.iterator(); iterator.hasNext();) {
			TempFallingBlock tfb = iterator.next();

			if (!tfb.getFallingBlock().isDead()) {
				playParticles(tfb.getLocation());
			} else {
				tfb.remove();
				iterator.remove();
			}
		}

		for (TempBlock tb : blocks) {
			playParticles(tb.getLocation());
		}
	}

	private void doPlayerCollisions() {
		List<TempFallingBlock> blocksCopy = new ArrayList<>(firedBlocks);

		for (TempFallingBlock tfb : blocksCopy) {
			if (!firedBlocks.contains(tfb)) continue;

			boolean didExplode = false;

			for (Entity e : GeneralMethods.getEntitiesAroundPoint(tfb.getLocation(), this.explosionRadius)) {
				if (!(e instanceof LivingEntity)) continue;
				if (e == this.player) continue;

				if (blast(tfb, true)) {
					didExplode = true;
				}
			}

			if (didExplode) {
				firedBlocks.remove(tfb);
			}
		}
	}

	private void playParticles(Location location) {
		location.add(.5,.5,.5);
		ParticleEffect.LAVA.display(location, 2, Math.random(), Math.random(), Math.random(), 0f);
		ParticleEffect.SMOKE_NORMAL.display(location, 2, Math.random(), Math.random(), Math.random(), 0f);
		for (int i = 0; i < 10; i++) {
			GeneralMethods.displayColoredParticle("FFA400", getOffsetLocation(location, 2));
			GeneralMethods.displayColoredParticle("FF8C00", getOffsetLocation(location, 2));
		}
	}

	// Returns true if any source blocks still exist. Returns false is all of the source blocks have been fired.
	public boolean hasBlocks() {
		return !sources.isEmpty() || !blocks.isEmpty();
	}

	private Location getOffsetLocation(Location loc, double offset) {
		return loc.clone().add((float) ((Math.random() - 0.5) * offset), (float) ((Math.random() - 0.5) * offset), (float) ((Math.random() - 0.5) * offset));
	}

	public static void performAction(Player player) {
		MagmaBlast mb = getAbility(player, MagmaBlast.class);

		if (mb != null) {
			mb.performAction();
		}
	}

	private void performAction() {
		long time = System.currentTimeMillis();

		if (blocks.isEmpty() || stopFiring || time < lastShot + shotCooldown) return;

		Location target = null;

		if (entitySelection) {
			Entity targetEntity = GeneralMethods.getTargetedEntity(player, selectRange);

			if (targetEntity instanceof LivingEntity) {
				target = ((LivingEntity) targetEntity).getEyeLocation();
			}
		}

		if (target == null) {
			target = GeneralMethods.getTargetedLocation(player, selectRange, Material.MAGMA_BLOCK);
		}

		TempBlock tb = getClosestSource(target);

		if (tb == null) return;

		stopFiring = true;
		canLavaFlowTime = time + 1000;
		blocks.remove(tb);

		Vector direction = GeneralMethods.getDirection(tb.getLocation().clone().add(0.5f, 0.5f, 0.5f), target).normalize();

		tb.revertBlock();

		FallingBlock fallingBlock = tb.getLocation().getWorld().spawnFallingBlock(
				tb.getLocation().clone().add(0.5, 0.5, 0.5),
				Material.MAGMA_BLOCK.createBlockData()
		);

		fallingBlock.setTicksLived(Integer.MAX_VALUE);
		fallingBlock.setMetadata("magmablast", new FixedMetadataValue(JedCore.plugin, "1"));
		fallingBlock.setDropItem(false);
		fallingBlock.setCancelDrop(true);

		TempFallingBlock tfb = new TempFallingBlock(tb.getLocation(), Material.MAGMA_BLOCK.createBlockData(), direction.multiply(fireSpeed), this, true);
		firedBlocks.add(tfb);

		lastShot = time;
	}

	// Get the closest fireable source block to the target location.
	private TempBlock getClosestSource(Location target) {
		double distanceSq = Double.MAX_VALUE;
		TempBlock closest = null;

		for (TempBlock tempBlock : blocks) {
			double currentDistSq = tempBlock.getLocation().distanceSquared(target);

			if (currentDistSq < distanceSq) {
				distanceSq = currentDistSq;
				closest = tempBlock;
			}
		}

		return closest;
	}

	public static void blast(TempFallingBlock tfb) {
		blast(tfb, false);
	}

	public static boolean blast(TempFallingBlock tfb, boolean entityCollision) {
		MagmaBlast mb = (MagmaBlast) tfb.getAbility();
		Location location = tfb.getLocation().clone().add(0.5, 0.5, 0.5);

		Block hitBlock = location.getBlock();
		if (hitBlock.getType().isSolid()) {
			blastEffects(location, mb);
			tfb.remove();
			mb.firedBlocks.remove(tfb);
			return true;
		}

		float radius = mb.explosionRadius;
		boolean didHit = false;

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, radius)) {
			if (!(entity instanceof LivingEntity)) continue;
			if (entity == mb.player) continue;

			DamageHandler.damageEntity(entity, mb.getDamage(), mb);
			((LivingEntity) entity).setNoDamageTicks(0);
			didHit = true;
		}

		if (didHit || entityCollision) {
			blastEffects(location, mb);
			tfb.remove();
			mb.firedBlocks.remove(tfb);
			return true;
		}

		return false;
	}

	private static void blastEffects(Location location, MagmaBlast mb) {
		float radius = mb.explosionRadius;
		float speed = 0.1f;

		ParticleEffect.FLAME.display(location, PARTICLE_COUNT, randomBinomial(radius), randomBinomial(radius), randomBinomial(radius), speed);
		ParticleEffect.SMOKE_LARGE.display(location, PARTICLE_COUNT, randomBinomial(radius), randomBinomial(radius), randomBinomial(radius), speed);
		ParticleEffect.FIREWORKS_SPARK.display(location, PARTICLE_COUNT, randomBinomial(radius), randomBinomial(radius), randomBinomial(radius), speed);

		location.getWorld().playSound(location,
				(rand.nextBoolean()) ? Sound.ENTITY_FIREWORK_ROCKET_BLAST : Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR,
				1f, 1f);
		location.getWorld().playSound(location,
				(rand.nextBoolean()) ? Sound.ENTITY_FIREWORK_ROCKET_TWINKLE : Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
				1f, 1f);
	}

	// Generates a random number between -max and max.
	private static float randomBinomial(float max) {
		return (rand.nextFloat() * max) - (rand.nextFloat() * max);
	}
	
	public double getDamage() {
		return damage;
	}
	
	public void setDamage(double damage) {
		this.damage = damage;
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
		return "MagmaBlast";
	}
	
	@Override
	public boolean isHiddenAbility() {
		return false;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public Object createNewComboInstance(Player player) {
		return new MagmaBlast(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		return ComboUtil.generateCombinationFromList(this, JedCoreConfig.getConfig(player).getStringList("Abilities.Earth.EarthCombo.MagmaBlast.Combination"));
	}

	@Override
	public String getInstructions() {
		return JedCoreConfig.getConfig(player).getString("Abilities.Earth.EarthCombo.MagmaBlast.Instructions");
	}

	@Override
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.EarthCombo.MagmaBlast.Description");
	}

	@Override
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	public Set<TempFallingBlock> getSources() {
		return sources;
	}

	public List<TempBlock> getBlocks() {
		return blocks;
	}

	public List<TempFallingBlock> getFiredBlocks() {
		return firedBlocks;
	}

	public long getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(long maxDuration) {
		this.maxDuration = maxDuration;
	}

	public long getShotCooldown() {
		return shotCooldown;
	}

	public void setShotCooldown(long shotCooldown) {
		this.shotCooldown = shotCooldown;
	}

	public int getMaxSources() {
		return maxSources;
	}

	public void setMaxSources(int maxSources) {
		this.maxSources = maxSources;
	}

	public int getSourceRange() {
		return sourceRange;
	}

	public void setSourceRange(int sourceRange) {
		this.sourceRange = sourceRange;
	}

	public double getSelectRange() {
		return selectRange;
	}

	public void setSelectRange(double selectRange) {
		this.selectRange = selectRange;
	}

	public double getFireSpeed() {
		return fireSpeed;
	}

	public void setFireSpeed(double fireSpeed) {
		this.fireSpeed = fireSpeed;
	}

	public double getMaxDistanceFromSources() {
		return maxDistanceFromSources;
	}

	public void setMaxDistanceFromSources(double maxDistanceFromSources) {
		this.maxDistanceFromSources = maxDistanceFromSources;
	}

	public float getExplosionRadius() {
		return explosionRadius;
	}

	public void setExplosionRadius(float explosionRadius) {
		this.explosionRadius = explosionRadius;
	}

	public boolean isRequireLavaFlow() {
		return requireLavaFlow;
	}

	public void setRequireLavaFlow(boolean requireLavaFlow) {
		this.requireLavaFlow = requireLavaFlow;
	}

	public boolean isPlayerCollisions() {
		return playerCollisions;
	}

	public void setPlayerCollisions(boolean playerCollisions) {
		this.playerCollisions = playerCollisions;
	}

	public boolean isEntitySelection() {
		return entitySelection;
	}

	public void setEntitySelection(boolean entitySelection) {
		this.entitySelection = entitySelection;
	}

	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(Location origin) {
		this.origin = origin;
	}

	public long getCanLavaFlowTime() {
		return canLavaFlowTime;
	}

	public void setCanLavaFlowTime(long canLavaFlowTime) {
		this.canLavaFlowTime = canLavaFlowTime;
	}

	public long getLastShotTime() {
		return lastShot;
	}

	public void setLastShotTime(long lastShot) {
		this.lastShot = lastShot;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.EarthCombo.MagmaBlast.Enabled");
	}
}