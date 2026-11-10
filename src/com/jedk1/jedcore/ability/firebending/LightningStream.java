package com.jedk1.jedcore.ability.firebending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.LightningCollisionSystem;
import com.jedk1.jedcore.util.lightning.LightningArc;
import com.jedk1.jedcore.util.lightning.LightningDisplay;
import com.jedk1.jedcore.util.lightning.LightningElectrocution;
import com.jedk1.jedcore.util.lightning.LightningStyle;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LightningStream extends LightningAbility implements AddonAbility {

	private static final String CONFIG_PATH = "Abilities.Fire.LightningStream.";
	private static final Random RANDOM = new Random();
	private static final double SOUND_RANGE = 16.0;
	private static final String DISPLAY_TAG = "jedcore_lightningstream";

	private Location origin;
	private Vector direction;
	private final List<LightningArc> arcs = new ArrayList<>();
	private final List<LightningArc.Segment> segments = new ArrayList<>();
	private final Set<UUID> damagedEntities = new HashSet<>();
	private final Set<UUID> playersHearingSound = new HashSet<>();

	private int regenerationTicks;
	private int soundTicks;
	private int durationTicks;
	private int maxDurationTicks;
	private LightningDisplay display;
	private LightningStyle lightningStyle;

	private LightningDisplay warmupDisplay;
	private LightningStyle warmupStyle;
	private boolean warmupEnabled;
	private long warmupDuration;
	private int maxWarmupTicks;
	private int warmupTicks;
	private boolean warmingUp;
	private boolean streamStarted;

	private LightningCollisionSystem.ActiveLightning registeredLightning;

	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.KNOCKBACK)
	private double knockback;
	@Attribute("StunChance")
	private double stunChance;
	@Attribute("Stun" + Attribute.DURATION)
	private double stunDuration;
	@Attribute("RegenerateInterval")
	private int regenerateInterval;
	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute("MaxSegments")
	private int maxSegments;
	@Attribute("SegmentLength")
	private double segmentLength;
	@Attribute("JitterAmount")
	private double jitterAmount;
	@Attribute("DisplayThickness")
	private double displayThickness;
	@Attribute("DisplayLengthPadding")
	private double displayLengthPadding;
	@Attribute("DisplayBrightness")
	private int displayBrightness;
	@Attribute("DisplayViewRange")
	private float displayViewRange;
	@Attribute("WaterElectrifyRange")
	private double waterElectrifyRange;
	@Attribute("WaterElectrifyDamage")
	private double waterElectrifyDamage;

	private boolean removeOnDamage;
	private boolean waterElectrifyEnabled;
	private boolean particlesEnabled;

	private String loopSound;
	private double loopSoundVolume;
	private double loopSoundPitch;
	private int loopSoundInterval;
	private String fadeoutSound;
	private double fadeoutSoundVolume;
	private double fadeoutSoundPitch;

	public LightningStream(Player player) {
		super(player);

		if (!isEnabled() || !bPlayer.canBend(this) || !bPlayer.canLightningbend() || hasAbility(player, LightningStream.class)) {
			return;
		}

		setFields();

		start();

		if (warmupEnabled) {
			this.warmingUp = true;
			this.maxWarmupTicks = (int) Math.max(1, warmupDuration / 50);
			this.warmupDisplay = new LightningDisplay(warmupStyle);
			displayWarmup();
		} else {
			beginStream();
		}
	}

	private void beginStream() {
		this.warmingUp = false;
		this.streamStarted = true;

		if (warmupDisplay != null) {
			warmupDisplay.remove();
			warmupDisplay = null;
		}

		this.display = new LightningDisplay(lightningStyle);
		this.maxDurationTicks = (int) Math.max(1, duration / 50);
		this.registeredLightning = LightningCollisionSystem.registerLightning(player, this);

		generateLightning();
		playLoopSoundToNearby();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		range = config.getDouble(CONFIG_PATH + "Range", 8.0);
		damage = config.getDouble(CONFIG_PATH + "Damage", 8.0);
		knockback = config.getDouble(CONFIG_PATH + "Knockback", 0.8);
		stunChance = Math.max(0.0, Math.min(1.0, config.getDouble(CONFIG_PATH + "Stun.Chance", 0.1)));
		stunDuration = Math.max(0.0, config.getDouble(CONFIG_PATH + "Stun.Duration", 1000));
		regenerateInterval = Math.max(1, config.getInt(CONFIG_PATH + "RegenerateInterval", 2));
		duration = Math.max(50, config.getLong(CONFIG_PATH + "Duration", 30000));
		cooldown = config.getLong(CONFIG_PATH + "Cooldown", 4000);
		removeOnDamage = config.getBoolean(CONFIG_PATH + "RemoveOnDamage", true);
		waterElectrifyEnabled = config.getBoolean(CONFIG_PATH + "WaterElectrify.Enabled", true);
		waterElectrifyRange = Math.max(0.5, config.getDouble(CONFIG_PATH + "WaterElectrify.Range", 3.0));
		waterElectrifyDamage = Math.max(0.0, config.getDouble(CONFIG_PATH + "WaterElectrify.Damage", 4.0));
		particlesEnabled = config.getBoolean(CONFIG_PATH + "Particles.Enabled", true);

		maxSegments = Math.max(2, config.getInt(CONFIG_PATH + "MaxSegments", 24));
		segmentLength = Math.max(0.1, config.getDouble(CONFIG_PATH + "SegmentLength", 0.6));
		jitterAmount = Math.max(0.0, config.getDouble(CONFIG_PATH + "JitterAmount", 0.2));
		displayThickness = Math.max(0.01, config.getDouble(CONFIG_PATH + "Display.Thickness", 0.05));
		displayLengthPadding = Math.max(0.0, config.getDouble(CONFIG_PATH + "Display.LengthPadding", 0.02));
		displayBrightness = Math.max(0, Math.min(15, config.getInt(CONFIG_PATH + "Display.Brightness", 15)));
		displayViewRange = (float) Math.max(0.1, config.getDouble(CONFIG_PATH + "Display.ViewRange", 1.0));

		String displayMaterial = config.getString(CONFIG_PATH + "Display.Material", "LIGHT_BLUE_CONCRETE_POWDER");
		Material material = displayMaterial == null ? null : Material.matchMaterial(displayMaterial);

		lightningStyle = LightningStyle.builder()
				.material(material)
				.thickness(displayThickness)
				.lengthPadding(displayLengthPadding)
				.brightness(displayBrightness)
				.viewRange(displayViewRange)
				.emitLight(config.getBoolean(CONFIG_PATH + "Display.EmitLight", true))
				.maxSegments(maxSegments)
				.segmentLength(segmentLength)
				.jitter(jitterAmount)
				.mainArcs(2)
				.branchChance(0.6)
				.branchInterval(4, 6)
				.branchLength(0.4)
				.subBranchChance(0.5)
				.subBranchLength(0.25)
				.branchJitter(0.8)
				.clipToBlocks(true)
				.displayTag(DISPLAY_TAG)
				.build();

		loopSound = config.getString(CONFIG_PATH + "Sound.Loop", "ENTITY_BEE_LOOP");
		loopSoundVolume = config.getDouble(CONFIG_PATH + "Sound.LoopVolume", 0.5);
		loopSoundPitch = config.getDouble(CONFIG_PATH + "Sound.LoopPitch", 0.8);
		loopSoundInterval = Math.max(1, config.getInt(CONFIG_PATH + "Sound.LoopInterval", 20));
		fadeoutSound = config.getString(CONFIG_PATH + "Sound.Fadeout", "ENTITY_LIGHTNING_BOLT_IMPACT");
		fadeoutSoundVolume = config.getDouble(CONFIG_PATH + "Sound.FadeoutVolume", 0.6);
		fadeoutSoundPitch = config.getDouble(CONFIG_PATH + "Sound.FadeoutPitch", 0.8);

		warmupDuration = config.getLong(CONFIG_PATH + "Warmup.Duration", 1500);
		warmupEnabled = config.getBoolean(CONFIG_PATH + "Warmup.Enabled", true) && warmupDuration > 0;

		warmupStyle = LightningStyle.builder()
				.material(Material.LIGHT_BLUE_CONCRETE_POWDER)
				.thickness(0.03)
				.lengthPadding(0.01)
				.brightness(15)
				.viewRange(1.0f)
				.emitLight(true)
				.maxSegments(8)
				.segmentLength(0.25)
				.jitter(0.2)
				.mainArcs(1)
				.branchChance(0.15)
				.branchInterval(2, 4)
				.branchLength(0.3)
				.subBranchChance(0.2)
				.subBranchLength(0.15)
				.branchJitter(0.7)
				.displayTag(DISPLAY_TAG + "_warmup")
				.build();
	}

	private void displayWarmup() {
		if (warmupDisplay == null) {
			return;
		}

		List<LightningArc> warmupArcs = new ArrayList<>();
		warmupArcs.addAll(LightningArc.around(GeneralMethods.getMainHandLocation(player), 0.8, 1, warmupStyle));
		warmupArcs.addAll(LightningArc.around(GeneralMethods.getOffHandLocation(player), 0.8, 1, warmupStyle));
		warmupDisplay.render(warmupArcs);
	}

	private void generateLightning() {
		arcs.clear();
		segments.clear();
		damagedEntities.clear();

		origin = GeneralMethods.getMainHandLocation(player);
		direction = origin.getDirection().normalize();

		Location start = origin.clone().add(direction.clone().multiply(0.5));

		RayTraceResult rayTrace = player.getWorld().rayTrace(
				start,
				direction,
				range,
				FluidCollisionMode.NEVER,
				true,
				0.3,
				entity -> entity instanceof LivingEntity && !entity.equals(player)
		);

		Location target;
		LivingEntity hitEntity = null;

		if (rayTrace != null) {
			if (rayTrace.getHitEntity() instanceof LivingEntity) {
				hitEntity = (LivingEntity) rayTrace.getHitEntity();
				target = hitEntity.getEyeLocation();
			} else if (rayTrace.getHitBlock() != null) {
				target = rayTrace.getHitPosition().toLocation(player.getWorld());
			} else {
				target = start.clone().add(direction.clone().multiply(range));
			}
		} else {
			target = start.clone().add(direction.clone().multiply(range));
		}

		arcs.addAll(LightningArc.beam(start, target, lightningStyle));
		for (LightningArc arc : arcs) {
			segments.addAll(arc.getSegments());
		}

		display.render(arcs);
		spawnBranchParticles();

		if (hitEntity != null && !damagedEntities.contains(hitEntity.getUniqueId())) {
			applyDamage(hitEntity, damage);
			damagedEntities.add(hitEntity.getUniqueId());
		}

		for (LightningArc.Segment segment : segments) {
			checkDamageAlongSegment(segment.getFrom(), segment.getTo());
		}

		electrifyWater();

		Set<String> spawnedBlockImpacts = new HashSet<>();
		for (LightningArc arc : arcs) {
			for (Location impact : arc.getBlockHits()) {
				String key = impact.getWorld().getName() + ":" + Math.round(impact.getX() * 10) + ":" + Math.round(impact.getY() * 10) + ":" + Math.round(impact.getZ() * 10);
				if (!spawnedBlockImpacts.add(key)) {
					continue;
				}
				impact.getWorld().spawnParticle(Particle.POOF, impact, 1, 0.0, 0.0, 0.0, 0.01);
				impact.getWorld().spawnParticle(Particle.SMOKE, impact, 3, 0.0, 0.0, 0.0, 0.01);
			}
		}

		if (registeredLightning != null) {
			List<Location> endpoints = new ArrayList<>();

			for (LightningArc arc : arcs) {
				endpoints.addAll(arc.getBranchEndpoints());
			}

			if (!segments.isEmpty()) {
				endpoints.add(segments.get(segments.size() - 1).getTo());
			}

			registeredLightning.updateBranchEndpoints(endpoints);
			LightningCollisionSystem.checkCollisions(registeredLightning);
		}
	}

	private void spawnBranchParticles() {
		if (!particlesEnabled) {
			return;
		}

		for (LightningArc arc : arcs) {
			for (List<Location> path : arc.getBranches()) {
				for (int i = 0; i < path.size(); i += 2) {
					if (RANDOM.nextInt(99) > 5) {
						playLightningbendingParticle(path.get(i), 0.03, 0.03, 0.03);
					}
				}

				if (!path.isEmpty() && RANDOM.nextInt(99) > 5) {
					playLightningbendingParticle(path.get(path.size() - 1), 0.08, 0.08, 0.08);
				}
			}
		}
	}

	private void checkDamageAlongSegment(Location from, Location to) {
		Location mid = from.clone().add(to).multiply(0.5);
		for (Entity entity : mid.getWorld().getNearbyEntities(mid, 1.0, 1.0, 1.0, e -> e instanceof LivingEntity)) {
			LivingEntity living = (LivingEntity) entity;
			if (!living.equals(player) && !damagedEntities.contains(living.getUniqueId())) {
				applyDamage(living, damage * 0.6);
				damagedEntities.add(living.getUniqueId());
			}
		}
	}

	private void electrifyWater() {
		if (!waterElectrifyEnabled || waterElectrifyDamage <= 0) {
			return;
		}

		double rangeSquared = waterElectrifyRange * waterElectrifyRange;

		for (LightningArc.Segment segment : segments) {
			double searchRadius = waterElectrifyRange + segment.getFrom().distance(segment.getTo());
			Location mid = segment.getFrom().clone().add(segment.getTo()).multiply(0.5);

			for (Entity entity : mid.getWorld().getNearbyEntities(mid, searchRadius, searchRadius, searchRadius, e -> e instanceof LivingEntity)) {
				LivingEntity living = (LivingEntity) entity;
				if (living.equals(player) || damagedEntities.contains(living.getUniqueId()) || !living.isInWater()) {
					continue;
				}

				if (distanceSquaredToSegment(living.getLocation(), segment.getFrom(), segment.getTo()) > rangeSquared) {
					continue;
				}

				applyWaterElectrifyDamage(living);
				damagedEntities.add(living.getUniqueId());
			}
		}
	}

	private double distanceSquaredToSegment(Location point, Location from, Location to) {
		Vector fromVector = from.toVector();
		Vector segment = to.toVector().subtract(fromVector);
		double segmentLengthSquared = segment.lengthSquared();

		if (segmentLengthSquared < 0.0001) {
			return point.toVector().distanceSquared(fromVector);
		}

		Vector pointVector = point.toVector();
		double t = pointVector.clone().subtract(fromVector).dot(segment) / segmentLengthSquared;
		t = Math.max(0.0, Math.min(1.0, t));

		Vector closestPoint = fromVector.clone().add(segment.multiply(t));
		return pointVector.distanceSquared(closestPoint);
	}

	private void applyWaterElectrifyDamage(LivingEntity target) {
		DamageHandler.damageEntity(target, waterElectrifyDamage, this);
		electrocute(target);

		Location effectLocation = target.getLocation().add(0, 0.5, 0);
		target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, effectLocation, 12, 0.4, 0.4, 0.4, 0.1);
		target.getWorld().spawnParticle(Particle.BUBBLE_POP, effectLocation, 6, 0.3, 0.3, 0.3, 0.02);
	}

	private void applyDamage(LivingEntity target, double damageAmount) {
		if (target instanceof Player) {
			Player targetPlayer = (Player) target;
			if (registeredLightning != null && LightningCollisionSystem.isBlocked(registeredLightning, targetPlayer)) {
				return;
			}
		}

		DamageHandler.damageEntity(target, damageAmount, this);
		target.setVelocity(direction.clone().multiply(knockback).setY(0.3));
		electrocute(target);

		target.getWorld().spawnParticle(
				Particle.ELECTRIC_SPARK,
				target.getEyeLocation(),
				10,
				0.35, 0.35, 0.35,
				0.1
		);
	}

	private void electrocute(LivingEntity target) {
		if (stunChance <= 0.0 || stunDuration <= 0.0 || target.equals(player)) {
			return;
		}

		if (ThreadLocalRandom.current().nextDouble() <= stunChance) {
			LightningElectrocution.apply(target, this, stunDuration);
		}
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}

		if (!player.isSneaking()) {
			remove();
			return;
		}

		if (RegionProtection.isRegionProtected(player, player.getLocation(), this)) {
			remove();
			return;
		}

		if (warmingUp) {
			warmupTicks++;
			if (warmupTicks >= maxWarmupTicks) {
				beginStream();
			} else {
				displayWarmup();
				return;
			}
		}

		durationTicks++;
		if (durationTicks >= maxDurationTicks) {
			remove();
			return;
		}

		soundTicks++;
		if (soundTicks >= loopSoundInterval) {
			soundTicks = 0;
			playLoopSoundToNearby();
		}

		regenerationTicks++;
		if (regenerationTicks >= regenerateInterval) {
			regenerationTicks = 0;
			generateLightning();
		} else {
			if (registeredLightning != null && registeredLightning.isInCollision()) {
				renderCollisionVisuals();
			}

			if (registeredLightning != null) {
				LightningCollisionSystem.checkCollisions(registeredLightning);
			}
		}
	}

	@Override
	public void remove() {
		if (isRemoved()) {
			return;
		}

		if (streamStarted) {
			stopLoopSounds();
			playFadeoutSoundToNearby();
		}
		if (warmupDisplay != null) {
			warmupDisplay.remove();
			warmupDisplay = null;
		}
		if (display != null) {
			display.remove();
		}

		arcs.clear();
		segments.clear();

		if (registeredLightning != null) {
			registeredLightning.markRemoved();
			LightningCollisionSystem.unregisterLightning(registeredLightning);
			registeredLightning = null;
		}

		super.remove();

		if (bPlayer != null) {
			bPlayer.addCooldown(this);
		}
	}

	public void onDamageReceived() {
		if (removeOnDamage) {
			remove();
		}
	}

	private void playLoopSoundToNearby() {
		Location soundLocation = player.getEyeLocation();

		for (Entity entity : soundLocation.getWorld().getNearbyEntities(soundLocation, SOUND_RANGE, SOUND_RANGE, SOUND_RANGE, e -> e instanceof Player)) {
			Player nearbyPlayer = (Player) entity;
			playConfiguredSound(nearbyPlayer, soundLocation, loopSound, (float) loopSoundVolume, (float) loopSoundPitch);
			playersHearingSound.add(nearbyPlayer.getUniqueId());
		}
	}

	private void playFadeoutSoundToNearby() {
		Location soundLocation = player.getEyeLocation();

		for (Entity entity : soundLocation.getWorld().getNearbyEntities(soundLocation, SOUND_RANGE, SOUND_RANGE, SOUND_RANGE, e -> e instanceof Player)) {
			playConfiguredSound((Player) entity, soundLocation, fadeoutSound, (float) fadeoutSoundVolume, (float) fadeoutSoundPitch);
		}
	}

	private void stopLoopSounds() {
		for (UUID playerId : playersHearingSound) {
			Player nearbyPlayer = Bukkit.getPlayer(playerId);
			if (nearbyPlayer != null && nearbyPlayer.isOnline()) {
				stopConfiguredSound(nearbyPlayer, loopSound);
			}
		}
		playersHearingSound.clear();
	}

	private void playConfiguredSound(Player target, Location location, String soundName, float volume, float pitch) {
		if (soundName == null || soundName.isEmpty()) {
			return;
		}

		Sound sound = resolveSound(soundName);
		if (sound != null) {
			target.playSound(location, sound, volume, pitch);
		} else {
			target.playSound(location, soundName, volume, pitch);
		}
	}

	private void stopConfiguredSound(Player target, String soundName) {
		if (soundName == null || soundName.isEmpty()) {
			return;
		}

		Sound sound = resolveSound(soundName);
		if (sound != null) {
			target.stopSound(sound);
		} else {
			target.stopSound(soundName);
		}
	}

	private static Sound resolveSound(String soundName) {
		try {
			return Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private void renderCollisionVisuals() {
		if (registeredLightning == null || !registeredLightning.isInCollision()) {
			return;
		}

		Location collisionPoint = registeredLightning.getCollisionPoint();
		if (collisionPoint == null) {
			return;
		}

		Location closestEndpoint = null;
		double closestDistance = Double.MAX_VALUE;

		if (!segments.isEmpty()) {
			Location mainEnd = segments.get(segments.size() - 1).getTo();
			double distance = mainEnd.distance(collisionPoint);
			if (distance < closestDistance) {
				closestDistance = distance;
				closestEndpoint = mainEnd;
			}
		}

		for (LightningArc arc : arcs) {
			for (Location branchEnd : arc.getBranchEndpoints()) {
				double distance = branchEnd.distance(collisionPoint);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestEndpoint = branchEnd;
				}
			}
		}

		if (closestEndpoint != null) {
			LightningCollisionSystem.drawCollisionConnection(closestEndpoint, collisionPoint);

			collisionPoint.getWorld().spawnParticle(
					Particle.ELECTRIC_SPARK,
					collisionPoint,
					2,
					0.1, 0.1, 0.1,
					0.02
			);
		}
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isIgniteAbility() {
		return false;
	}

	@Override
	public boolean isExplosiveAbility() {
		return false;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public String getName() {
		return "LightningStream";
	}

	@Override
	public Location getLocation() {
		return origin != null ? origin : player.getLocation();
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
		return "* JedCore Addon *\n" + config.getString(CONFIG_PATH + "Description");
	}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean(CONFIG_PATH + "Enabled");
	}

	@Override
	public void load() {
	}

	@Override
	public void stop() {
	}

	public double getDamage() {
		return damage;
	}

	public double getRange() {
		return range;
	}
}
