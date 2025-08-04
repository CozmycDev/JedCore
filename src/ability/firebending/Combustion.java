package com.jedk1.jedcore.ability.firebending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.collision.CollisionDetector;
import com.jedk1.jedcore.collision.Sphere;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.policies.removal.*;
import com.jedk1.jedcore.util.FireTick;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import com.jedk1.jedcore.util.MaterialUtil;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CombustionAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Combustion extends CombustionAbility implements AddonAbility {

	private State state;
	private Location location;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private CompositeRemovalPolicy removalPolicy;

	private ArrayList<String> skipMaterials; // use a configured list of blocks to skip through

	public Combustion(Player player) {
		super(player);

		if (!isEnabled()) return;

		if (this.player == null || !bPlayer.canBend(this) || !bPlayer.canCombustionbend()) {
			return;
		}

		if (hasAbility(player, Combustion.class)) {
			Combustion c = getAbility(player, Combustion.class);
			if (c.state instanceof ChargeState)
				return;
		}

		setFields();

		start();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Fire.Combustion.Cooldown");

		this.location = null;
		this.state = new ChargeState();

		this.removalPolicy = new CompositeRemovalPolicy(this,
				new CannotBendRemovalPolicy(this.bPlayer, this, true, true),
				new IsOfflineRemovalPolicy(this.player),
				new IsDeadRemovalPolicy(this.player),
				new SwappedSlotsRemovalPolicy<>(bPlayer, Combustion.class)
		);

		this.removalPolicy.load(config, "Abilities.Fire.Combustion");

		this.skipMaterials = loadSkipMaterials();
	}

	@Override
	public void progress() {
		if (this.removalPolicy.shouldRemove()) {
			remove();
			return;
		}

		state.update();
	}

	public static void combust(Player player) {
		if(hasAbility(player, Combustion.class)) {
			Combustion c = getAbility(player, Combustion.class);

			c.selfCombust();
		}
	}

	public void selfCombust() {
		if (this.state instanceof TravelState) {
			this.state = new CombustState(location);
		}
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		// Only enable the collision while traveling.
		if (state instanceof TravelState) {
			return location;
		}

		return null;
	}

	@Override
	public double getCollisionRadius() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getDouble("Abilities.Fire.Combustion.AbilityCollisionRadius");
	}

	@Override
	public String getName() {
		return "Combustion";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Fire.Combustion.Description");
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Fire.Combustion.Enabled");
	}

	private ArrayList<String> loadSkipMaterials() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		ArrayList<String> skipList = new ArrayList<>();

		if (config.contains("Abilities.Fire.Combustion.SkipMaterials")) {
			List<String> configuredSkipList = config.getStringList("Abilities.Fire.Combustion.SkipMaterials");

			for (String entry : configuredSkipList) {
				if (entry.startsWith("#")) {
					String tagName = entry.substring(1).toLowerCase();

					NamespacedKey tagKey = NamespacedKey.minecraft(tagName);
					Tag<Material> materialTag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class);

					if (materialTag != null) {
						skipList.addAll(materialTag.getValues().stream()
								.map(material -> material.name().toLowerCase())
								.collect(Collectors.toList()));
					}
				} else {
					skipList.add(entry.toLowerCase());
				}
			}
		}

		return skipList;
	}

	private interface State {
		void update();
	}

	// This is the initial state of Combustion.
	// It's used to render the particle ring that happens during charging.
	// This state transitions to TravelState if the player stops sneaking after charging is done.
	// This state transitions to CombustState if the player takes damage while charging.
	private class ChargeState implements State {
		private final long startTime;
		private int currPoint = (int) player.getLocation().getYaw() + 90;
		private final long warmup;
		private final double playerStartHealth;
		private final boolean instantExplodeIfHit;

		public ChargeState() {
			this.startTime = System.currentTimeMillis();
			this.playerStartHealth = player.getHealth();

			ConfigurationSection config = JedCoreConfig.getConfig(player);

			this.instantExplodeIfHit = config.getBoolean("Abilities.Fire.Combustion.InstantExplodeIfHit");
			this.warmup = config.getLong("Abilities.Fire.Combustion.Warmup");
		}

		@Override
		public void update() {
			long time = System.currentTimeMillis();

			boolean charged = time >= this.startTime + warmup;

			if (player.isSneaking()) {
				if (!bPlayer.canBendIgnoreBinds(Combustion.this)) {
					remove();
					return;
				}

				playParticleRing(60, 1.75F, 2);

				if (instantExplodeIfHit && player.getHealth() < playerStartHealth) {
					// Remove and combust at player's location
					state = new CombustState(player.getLocation(), true);
					bPlayer.addCooldown(Combustion.this);
					return;
				}

				if (charged) {
					ParticleEffect.SMOKE_LARGE.display(player.getLocation(), 1, Math.random(), Math.random(), Math.random(), 0.1);
				}
			} else {
				if (charged) {
					state = new TravelState();
					bPlayer.addCooldown(Combustion.this);
				} else {
					remove();
				}
			}
		}

		private void playParticleRing(int points, float size, int speed) {
			for (int i = 0; i < speed; ++i) {
				currPoint += 360 / points;

				if (currPoint > 360) {
					currPoint = 0;
				}

				double angle = currPoint * 3.141592653589793D / 180.0D;
				double x = size * Math.cos(angle);
				double z = size * Math.sin(angle);

				Location loc = player.getLocation().add(x, 1.0D, z);
				playFirebendingParticles(loc, 3, 0.0, 0.0, 0.0);
				ParticleEffect.SMOKE_NORMAL.display(loc, 4, 0.0, 0.0, 0.0, 0.01);
				JCMethods.emitLight(loc);
			}
		}
	}

	// This state is used after the player releases a charged Combustion.
    // It's used for moving and rendering the projectile.
    // This state transitions to CombustState when it collides with terrain or an entity.
	private class TravelState implements State {
		private Vector direction;
		private final int range;
		private final double speed;
		private final boolean explodeOnDeath;
		private final double entityCollisionRadius;
		private double distanceTraveled;

		public TravelState() {
			removalPolicy.removePolicyType(SwappedSlotsRemovalPolicy.class);

			Location origin = player.getEyeLocation().clone();
			origin.setY(origin.getY() - 0.8D);
			location = origin.clone();

			ConfigurationSection config = JedCoreConfig.getConfig(player);

			range = config.getInt("Abilities.Fire.Combustion.Range");
			speed = config.getDouble("Abilities.Fire.Combustion.Speed");
			explodeOnDeath = config.getBoolean("Abilities.Fire.Combustion.ExplodeOnDeath");
			entityCollisionRadius = config.getDouble("Abilities.Fire.Combustion.EntityCollisionRadius");

			if (explodeOnDeath) {
				removalPolicy.removePolicyType(CannotBendRemovalPolicy.class);
				removalPolicy.removePolicyType(IsDeadRemovalPolicy.class);
			}

			direction = player.getEyeLocation().getDirection().normalize();
			distanceTraveled = 0;
		}

		@Override
		public void update() {
			if (explodeOnDeath && player.isDead()) {
				state = new CombustState(location);
				return;
			}

			// Manually handle the region protection check because the CannotBendRemovalPolicy no longer checks it
			// when explodeOnDeath is true. This stops players from firing Combustion and then walking into a
			// protected area.
			if (explodeOnDeath) {
				if (RegionProtection.isRegionProtected(Combustion.this, player.getLocation())) {
					remove();
					return;
				}
			}

			if (distanceTraveled >= range) {
				remove();
				return;
			}

			travel();
		}

		private void travel() {
			double stepDistance = speed;

			for (int i = 0; i < (int) (speed * 5); ++i) {
				render();

				Sphere collider = new Sphere(location.toVector(), entityCollisionRadius);

				boolean hit = CollisionDetector.checkEntityCollisions(player, collider, (entity) -> {
					location = entity.getLocation();
					state = new CombustState(location);
					return true;
				});

				if (hit) {
					return;
				}

				if (!MaterialUtil.isTransparent(location.getBlock()) || isWater(location.getBlock())) {
					Material blockMaterial = location.getBlock().getType();
					String blockMaterialName = blockMaterial.name().toLowerCase();

					boolean shouldSkip = skipMaterials.contains(blockMaterialName);

					if (!shouldSkip) {
						state = new CombustState(location);
						return;
					}
				}

				direction = player.getEyeLocation().getDirection().normalize();
				location = location.add(direction.clone().multiply(stepDistance));

				distanceTraveled += stepDistance;

				if (distanceTraveled >= range) {
					remove();
					return;
				}
			}
		}

		private void render() {
			if (bPlayer.canUseSubElement(SubElement.BLUE_FIRE)) {
				ParticleEffect.SOUL_FIRE_FLAME.display(location, 1, 0.0, 0.0, 0.0, 0.03);
			} else {
				ParticleEffect.FLAME.display(location, 1, 0.0, 0.0, 0.0, 0.03);
			}
			ParticleEffect.SMOKE_LARGE.display(location, 1, 0.0, 0.0, 0.0F, 0.06);
			ParticleEffect.FIREWORKS_SPARK.display(location, 1, 0.0, 0.0, 0.0F, 0.06);

			location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 0.01F);

			JCMethods.emitLight(location);
		}
	}

	@Override
	public void handleCollision(final Collision collision) {
		super.handleCollision(collision);

		if (collision.isRemovingFirst()) {
			state = new CombustState(collision.getLocationFirst());
		}
	}

	// This state is used for doing the explosion.
	// ChargeState can transition to this state if the player takes damage while charging.
	// TravelState can transition to this state if the projectile collides with terrain, entity, or collidable ability.
	private class CombustState implements State {
		private final long startTime;
		private final long regenTime;
		private boolean waitForRegen;

		public CombustState(Location location) {
			this(location, false);
		}

		public CombustState(Location location, boolean misfire) {
			removalPolicy.removePolicyType(SwappedSlotsRemovalPolicy.class);
			// This stops players from moving into a protected area to bypass the regen wait time.
			removalPolicy.removePolicyType(CannotBendRemovalPolicy.class);

			ConfigurationSection config = JedCoreConfig.getConfig(player);

			this.startTime = System.currentTimeMillis();
			this.regenTime = config.getLong("Abilities.Fire.Combustion.RegenTime");
			this.waitForRegen = config.getBoolean("Abilities.Fire.Combustion.WaitForRegen");

			double damage = config.getDouble("Abilities.Fire.Combustion.Damage");
			int fireTick = config.getInt("Abilities.Fire.Combustion.FireTick");
			int power = config.getInt("Abilities.Fire.Combustion.Power");
			boolean damageBlocks = config.getBoolean("Abilities.Fire.Combustion.DamageBlocks");
			boolean regenBlocks = config.getBoolean("Abilities.Fire.Combustion.RegenBlocks");

			ExplosionMethod explosionMethod;
			if (regenBlocks) {
				explosionMethod = new RegenExplosionMethod(damageBlocks, regenTime);
			} else {
				explosionMethod = new PermanentExplosionMethod(damageBlocks);
			}

			// Don't wait for regen if the blocks aren't even being destroyed.
			if (!damageBlocks) {
				waitForRegen = false;
			}

			double modifier = 0;
			if (misfire) {
				modifier = config.getInt("Abilities.Fire.Combustion.MisfireModifier");
			}

			int destroyedCount = explosionMethod.explode(location, power + modifier, damage + modifier, fireTick);

			// Don't wait for regen if nothing was actually destroyed.
			if (destroyedCount <= 0) {
				waitForRegen = false;
			}

			AirAbility.removeAirSpouts(location, power, player);
			WaterAbility.removeWaterSpouts(location, power, player);
		}

		@Override
		public void update() {
			if (!waitForRegen || System.currentTimeMillis() >= (this.startTime + this.regenTime)) {
				remove();
			}
		}
	}

	private interface ExplosionMethod {
		// Returns how many blocks were destroyed.
		int explode(Location location, double size, double damage, int fireTick);
	}

	private abstract class AbstractExplosionMethod implements ExplosionMethod {
		protected List<Material> blocks = Arrays.asList(
				Material.AIR, Material.VOID_AIR, Material.CAVE_AIR, Material.BEDROCK, Material.CHEST, Material.TRAPPED_CHEST, Material.OBSIDIAN,
				Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_PORTAL_FRAME, Material.FIRE,
				Material.WATER, Material.LAVA, Material.DROPPER, Material.FURNACE,
				Material.DISPENSER, Material.HOPPER, Material.BEACON, Material.BARRIER, Material.SPAWNER
		);

		private final boolean destroy;
		private final Random rand = new Random();

		public AbstractExplosionMethod(boolean destroy) {
			this.destroy = destroy;
		}

		public int explode(Location location, double size, double damage, int fireTick) {
			render(location);

			if (!destroy) {
				return 0;
			}

			location.getWorld().createExplosion(location, 0.0F);
			int destroyCount = destroyBlocks(location, (int)size);
			damageEntities(location, size, damage, fireTick);

			return destroyCount;
		}

		private void render(Location location) {
			if (bPlayer.canUseSubElement(SubElement.BLUE_FIRE)) {
				ParticleEffect.SOUL_FIRE_FLAME.display(location, 20, Math.random(), Math.random(), Math.random(), 0.5);
			} else {
				ParticleEffect.FLAME.display(location, 20, Math.random(), Math.random(), Math.random(), 0.5);
			}
			ParticleEffect.SMOKE_LARGE.display(location, 20, Math.random(), Math.random(), Math.random(), 0.5);
			ParticleEffect.FIREWORKS_SPARK.display(location, 20, Math.random(), Math.random(), Math.random(), 0.5);
			ParticleEffect.SMOKE_LARGE.display(location, 20, Math.random(), Math.random(), Math.random());
			ParticleEffect.EXPLOSION_HUGE.display(location, 20, Math.random(), Math.random(), Math.random(), 0.5);

			location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
		}

		private int destroyBlocks(Location location, int size) {
			int count = 0;

			for (Location l : GeneralMethods.getCircle(location, size, size, false, true, 0)) {
				if (!RegionProtection.isRegionProtected(Combustion.this, l)) {
					if (destroyBlock(l)) {
						++count;
					}
				}
			}

			return count;
		}

		private void damageEntities(Location location, double size, double damage, int fireTick) {
			for (Entity e : GeneralMethods.getEntitiesAroundPoint(location, size)) {
				if (e instanceof LivingEntity) {
					if (!RegionProtection.isRegionProtected(Combustion.this, e.getLocation())) {
						DamageHandler.damageEntity(e, damage, Combustion.this);
						FireTick.set(e, fireTick);
					}
				}
			}
		}

		protected void placeRandomFire(Location location) {
			int chance = rand.nextInt(3);

			if ((!(location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ()).getType().isSolid())) || (chance != 0))
				return;

			location.getBlock().setType(Material.FIRE);
		}

		protected void placeRandomBlock(Location location) {
			int chance = rand.nextInt(3);

			if (!(location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ()).getType().isSolid()))
				return;

			Material block = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ()).getType();

			if (chance == 0)
				location.getBlock().setType(block);
		}

		// Returns how many blocks were destroyed.
		public abstract boolean destroyBlock(Location location);
	}

	private class RegenExplosionMethod extends AbstractExplosionMethod {
		private final long regenTime;

		public RegenExplosionMethod(boolean destroy, long regenTime) {
			super(destroy);
			this.regenTime = regenTime;
		}

		@Override
		public boolean destroyBlock(Location l) {
			Block block = l.getBlock();

			if (TempBlock.isTempBlock(block))
				TempBlock.revertBlock(block, Material.AIR);
			if (TempBlock.isTempBlock(block))
				TempBlock.removeBlock(block);

			if (!MaterialUtil.isTransparent(block) && !blocks.contains(block.getType()) && !MaterialUtil.isSign(block)) {
				new RegenTempBlock(block, Material.AIR, Material.AIR.createBlockData(), regenTime, false);
				placeRandomBlock(l);
				placeRandomFire(l);

				return true;
			}

			return false;
		}
	}

	private class PermanentExplosionMethod extends AbstractExplosionMethod {
		public PermanentExplosionMethod(boolean destroy) {
			super(destroy);
		}

		@Override
		public boolean destroyBlock(Location l) {
			Block block = l.getBlock();

			if (!MaterialUtil.isTransparent(block) && !blocks.contains(block.getType()) && !MaterialUtil.isSign(block)) {
				Block newBlock = l.getWorld().getBlockAt(l);
				newBlock.setType(Material.AIR);
				placeRandomBlock(l);
				placeRandomFire(l);

				return true;
			}

			return false;
		}
	}
}
