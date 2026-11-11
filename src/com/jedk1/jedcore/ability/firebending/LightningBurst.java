package com.jedk1.jedcore.ability.firebending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.lightning.LightningArc;
import com.jedk1.jedcore.util.lightning.LightningDisplay;
import com.jedk1.jedcore.util.lightning.LightningEffects;
import com.jedk1.jedcore.util.lightning.LightningElectrocution;
import com.jedk1.jedcore.util.lightning.LightningStyle;
import com.jedk1.jedcore.util.lightning.LightningWaterElectrify;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class LightningBurst extends LightningAbility implements AddonAbility {

	private static final String DISPLAY_TAG = "jedcore_lightningburst";
	private static final String CONFIG_PATH = "Abilities.Fire.LightningBurst.";
	private static final double SOUND_RANGE = 16.0;
	private static final ConcurrentHashMap<Integer, Bolt> BOLTS = new ConcurrentHashMap<>();
	private final Set<UUID> playersHearingWarmup = new HashSet<>();
	private final Set<UUID> waterElectrified = new HashSet<>();

	Random rand = new Random();
	private LightningDisplay warmupDisplay;
	private LightningStyle warmupStyle;
	private LightningStyle burstStyle;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown, avatarCooldown;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeUp, avatarChargeup;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.RADIUS)
	private double radius;
	@Attribute(Attribute.SPEED)
	private double speed;
	@Attribute("StunChance")
	private double stunChance;
	@Attribute("Stun" + Attribute.DURATION)
	private double stunDuration;
	@Attribute("WaterElectrifyRange")
	private double waterElectrifyRange;
	@Attribute("WaterElectrifyDamage")
	private double waterElectrifyDamage;

	private boolean waterElectrifyEnabled;
	private boolean charged;
	private static int ID = Integer.MIN_VALUE;

	private float soundVolume;
	private int soundInterval;

	private String warmupSound;
	private double warmupSoundVolume;
	private double warmupSoundPitch;
	private int warmupSoundInterval;
	private int warmupSoundTicks;

	public LightningBurst(Player player) {
		super(player);
		if (!bPlayer.canBend(this) || hasAbility(player, LightningBurst.class)) {
			return;
		}

		setFields();
		warmupDisplay = new LightningDisplay(warmupStyle);
		if (bPlayer.isAvatarState() || JCMethods.isSozinsComet(player.getWorld())) {
			chargeUp = avatarChargeup;
			cooldown = avatarCooldown;
		}

		start();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Fire.LightningBurst.Cooldown", 25000L);
		chargeUp = config.getLong("Abilities.Fire.LightningBurst.ChargeUp", 4000L);
		avatarCooldown = config.getLong("Abilities.Fire.LightningBurst.AvatarCooldown", 1000L);
		avatarChargeup = config.getLong("Abilities.Fire.LightningBurst.AvatarChargeUp", 1000L);
		damage = config.getDouble("Abilities.Fire.LightningBurst.Damage", 9.0);
		radius = config.getDouble("Abilities.Fire.LightningBurst.Radius", 12.0);
		speed = Math.max(0.05, config.getDouble("Abilities.Fire.LightningBurst.Speed", 0.8));
		stunChance = config.getDouble("Abilities.Fire.LightningBurst.Stun.Chance", 0.1);
		stunDuration = config.getDouble("Abilities.Fire.LightningBurst.Stun.Duration", 1000.0);
		waterElectrifyEnabled = config.getBoolean("Abilities.Fire.LightningBurst.WaterElectrify.Enabled", true);
		waterElectrifyRange = Math.max(0.5, config.getDouble("Abilities.Fire.LightningBurst.WaterElectrify.Range", 3.0));
		waterElectrifyDamage = Math.max(0.0, config.getDouble("Abilities.Fire.LightningBurst.WaterElectrify.Damage", 5.0));

		soundVolume = (float) config.getDouble("Abilities.Fire.LightningBurst.Sound.Volume", 0.6);
		soundInterval = config.getInt("Abilities.Fire.LightningBurst.Sound.Interval", 6);

		warmupSound = config.getString(CONFIG_PATH + "Sound.Warmup.Sound", "ENTITY_BEE_LOOP");
		warmupSoundVolume = config.getDouble(CONFIG_PATH + "Sound.Warmup.Volume", 0.5);
		warmupSoundPitch = config.getDouble(CONFIG_PATH + "Sound.Warmup.Pitch", 0.6);
		warmupSoundInterval = Math.max(1, config.getInt(CONFIG_PATH + "Sound.Warmup.Interval", 20));

		String displayMaterial = config.getString(CONFIG_PATH + "Display.Material", "LIGHT_BLUE_CONCRETE_POWDER");
		Material material = displayMaterial == null ? null : Material.matchMaterial(displayMaterial);
		double displayThickness = Math.max(0.01, config.getDouble(CONFIG_PATH + "Display.Thickness", 0.05));
		double displayLengthPadding = Math.max(0.0, config.getDouble(CONFIG_PATH + "Display.LengthPadding", 0.02));
		int displayBrightness = Math.max(0, Math.min(15, config.getInt(CONFIG_PATH + "Display.Brightness", 15)));
		float displayViewRange = (float) Math.max(0.1, config.getDouble(CONFIG_PATH + "Display.ViewRange", 1.0));
		boolean displayEmitLight = config.getBoolean(CONFIG_PATH + "Display.EmitLight", true);

		warmupStyle = LightningStyle.builder()
				.material(material)
				.thickness(displayThickness)
				.lengthPadding(displayLengthPadding)
				.brightness(displayBrightness)
				.viewRange(displayViewRange)
				.emitLight(displayEmitLight)
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

		burstStyle = LightningStyle.builder()
				.material(material)
				.thickness(displayThickness)
				.lengthPadding(displayLengthPadding)
				.brightness(displayBrightness)
				.viewRange(displayViewRange)
				.emitLight(displayEmitLight)
				.maxSegments(8)
				.segmentLength(1.5)
				.jitter(0.4)
				.mainArcs(5)
				.branchChance(0.15)
				.branchInterval(4, 7)
				.branchLength(0.22)
				.subBranchChance(0.3)
				.subBranchLength(0.12)
				.branchJitter(0.7)
				.clipToBlocks(true)
				.displayTag(DISPLAY_TAG + "_burst")
				.build();
	}

	public static boolean isNearBolt(Player player, double range) {
		double rangeSquared = range * range;
		for (Bolt bolt : BOLTS.values()) {
			if (bolt.ability.player.equals(player)) {
				continue;
			}

			Location location = bolt.location;
			if (location == null || location.getWorld() == null || !location.getWorld().equals(player.getWorld())) {
				continue;
			}

			if (location.distanceSquared(player.getLocation()) <= rangeSquared) {
				return true;
			}
		}

		return false;
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
		if (RegionProtection.isRegionProtected(player, player.getLocation(), this)) {
			remove();
			return;
		}
		if (!player.isSneaking()) {
			if (!isCharging()) {
				releaseHemispheres();
				bPlayer.addCooldown(this);
			}
			remove();
		} else {
			if (System.currentTimeMillis() > getStartTime() + chargeUp){
				if (isCharging()) {
					setCharging(false);
					warmupSoundTicks = 0;
					playWarmupSoundToNearby();
				}
				displayCharging();
			}

			if (!isCharging()) {
				warmupSoundTicks++;
				if (warmupSoundTicks >= warmupSoundInterval) {
					warmupSoundTicks = 0;
					playWarmupSoundToNearby();
				}
			}
		}
	}

	private void releaseHemispheres() {
		spawnHemisphere(true);
		spawnHemisphere(false);
		spawnBurstParticles();
	}

	private void spawnHemisphere(boolean mainHand) {
		Location hand = mainHand ? GeneralMethods.getMainHandLocation(player) : GeneralMethods.getOffHandLocation(player);
		Vector axis = getOutwardAxis(hand, mainHand);
		Location start = hand.clone().add(axis.clone().multiply(0.2));

		Vector perpendicular1 = axis.clone().crossProduct(new Vector(0, 1, 0));
		if (perpendicular1.lengthSquared() < 0.01) {
			perpendicular1 = axis.clone().crossProduct(new Vector(1, 0, 0));
		}
		perpendicular1.normalize();
		Vector perpendicular2 = axis.clone().crossProduct(perpendicular1).normalize();

		double[] polarAngles = {0.0, 45.0, 80.0};
		int[] counts = {1, 6, 10};

		for (int ring = 0; ring < polarAngles.length; ring++) {
			double theta = Math.toRadians(polarAngles[ring]);
			double cosTheta = Math.cos(theta);
			double sinTheta = Math.sin(theta);

			for (int i = 0; i < counts[ring]; i++) {
				double phi = (Math.PI * 2.0 * i) / counts[ring];
				Vector direction = axis.clone().multiply(cosTheta)
						.add(perpendicular1.clone().multiply(Math.cos(phi) * sinTheta))
						.add(perpendicular2.clone().multiply(Math.sin(phi) * sinTheta))
						.normalize();

				Location boltStart = start.clone();
				boltStart.setDirection(direction);
				spawnBolt(boltStart, radius, speed, 20, true, mainHand);
			}
		}
	}

	private void spawnBurstParticles() {
		for (boolean mainHand : new boolean[] { true, false }) {
			Location hand = mainHand ? GeneralMethods.getMainHandLocation(player) : GeneralMethods.getOffHandLocation(player);
			Vector axis = getOutwardAxis(hand, mainHand);
			Vector perpendicular1 = axis.clone().crossProduct(new Vector(0, 1, 0));
			if (perpendicular1.lengthSquared() < 0.01) {
				perpendicular1 = axis.clone().crossProduct(new Vector(1, 0, 0));
			}
			perpendicular1.normalize();
			Vector perpendicular2 = axis.clone().crossProduct(perpendicular1).normalize();

			for (int i = 0; i < 12; i++) {
				double theta = rand.nextDouble() * (Math.PI / 2.0);
				double phi = rand.nextDouble() * Math.PI * 2.0;
				double distance = 0.5 + rand.nextDouble() * Math.max(1.0, radius * 0.6);
				Vector direction = axis.clone().multiply(Math.cos(theta))
						.add(perpendicular1.clone().multiply(Math.cos(phi) * Math.sin(theta)))
						.add(perpendicular2.clone().multiply(Math.sin(phi) * Math.sin(theta)))
						.normalize();

				Location particleLocation = hand.clone().add(direction.multiply(distance));
				playLightningbendingParticle(particleLocation, 0.04, 0.04, 0.04);
				if (rand.nextInt(3) == 0) {
					particleLocation.getWorld().spawnParticle(Particle.WAX_OFF, particleLocation, 1, 0.04, 0.04, 0.04, 0.01);
				}
			}
		}
	}

	void spawnAirParticle() {
		boolean mainHand = rand.nextBoolean();
		Location hand = mainHand ? GeneralMethods.getMainHandLocation(player) : GeneralMethods.getOffHandLocation(player);
		Vector axis = getOutwardAxis(hand, mainHand);
		Vector perpendicular1 = axis.clone().crossProduct(new Vector(0, 1, 0));
		if (perpendicular1.lengthSquared() < 0.01) {
			perpendicular1 = axis.clone().crossProduct(new Vector(1, 0, 0));
		}
		perpendicular1.normalize();
		Vector perpendicular2 = axis.clone().crossProduct(perpendicular1).normalize();

		double theta = rand.nextDouble() * (Math.PI / 2.0);
		double phi = rand.nextDouble() * Math.PI * 2.0;
		double distance = 0.5 + rand.nextDouble() * Math.max(1.0, radius * 0.6);
		Vector direction = axis.clone().multiply(Math.cos(theta))
				.add(perpendicular1.clone().multiply(Math.cos(phi) * Math.sin(theta)))
				.add(perpendicular2.clone().multiply(Math.sin(phi) * Math.sin(theta)))
				.normalize();

		Location particleLocation = hand.clone().add(direction.multiply(distance));
		playLightningbendingParticle(particleLocation, 0.04, 0.04, 0.04);
		if (rand.nextInt(4) == 0) {
			particleLocation.getWorld().spawnParticle(Particle.WAX_OFF, particleLocation, 1, 0.04, 0.04, 0.04, 0.01);
		}
	}

	private Vector getOutwardAxis(Location hand, boolean mainHand) {
		Vector forward = player.getEyeLocation().getDirection().clone().setY(0.0);
		if (forward.lengthSquared() < 0.01) {
			forward = player.getLocation().getDirection().clone().setY(0.0);
		}
		if (forward.lengthSquared() < 0.01) {
			forward = new Vector(0, 0, 1);
		}
		forward.normalize();

		Vector right = forward.crossProduct(new Vector(0, 1, 0)).normalize();
		Location center = player.getLocation().add(0, 1, 0);
		Vector offset = hand.toVector().subtract(center.toVector());
		offset.setY(0.0);

		double side = offset.dot(right);
		if (Math.abs(side) < 0.01) {
			return mainHand ? right : right.clone().multiply(-1);
		}

		return side > 0 ? right : right.clone().multiply(-1);
	}

	private void spawnBolt(Location location, double max, double gap, int arc, boolean doDamage, boolean mainHand) {
		int id = ID;
		BOLTS.put(id, new Bolt(this, location, id, max, gap, arc, doDamage, mainHand));
		if (ID == Integer.MAX_VALUE)
			ID = Integer.MIN_VALUE;
		ID++;
	}

	private void displayCharging() {
		if (warmupDisplay == null) {
			return;
		}

		List<LightningArc> arcs = new ArrayList<>();
		arcs.addAll(LightningArc.around(GeneralMethods.getMainHandLocation(player), 0.8, 1, warmupStyle));
		arcs.addAll(LightningArc.around(GeneralMethods.getOffHandLocation(player), 0.8, 1, warmupStyle));
		warmupDisplay.render(arcs);
	}

	private void playWarmupSoundToNearby() {
		Location soundLocation = player.getEyeLocation();

		for (Entity entity : soundLocation.getWorld().getNearbyEntities(soundLocation, SOUND_RANGE, SOUND_RANGE, SOUND_RANGE, e -> e instanceof Player)) {
			Player nearbyPlayer = (Player) entity;
			playConfiguredSound(nearbyPlayer, soundLocation, warmupSound, (float) warmupSoundVolume, (float) warmupSoundPitch);
			playersHearingWarmup.add(nearbyPlayer.getUniqueId());
		}
	}

	private void stopWarmupSounds() {
		for (UUID playerId : playersHearingWarmup) {
			Player nearbyPlayer = Bukkit.getPlayer(playerId);
			if (nearbyPlayer != null && nearbyPlayer.isOnline()) {
				stopConfiguredSound(nearbyPlayer, warmupSound);
			}
		}
		playersHearingWarmup.clear();
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

	public static void progressAll() {
		BOLTS.values().forEach(Bolt::progress);
	}

	public boolean isCharging() {
		return !charged;
	}

	public void setCharging(boolean charging) {
		this.charged = !charging;
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
		return "LightningBurst";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Fire.LightningBurst.Description");
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getAvatarCooldown() {
		return avatarCooldown;
	}

	public void setAvatarCooldown(long avatarCooldown) {
		this.avatarCooldown = avatarCooldown;
	}

	public long getChargeUp() {
		return chargeUp;
	}

	public void setChargeUp(long chargeUp) {
		this.chargeUp = chargeUp;
	}

	public long getAvatarChargeup() {
		return avatarChargeup;
	}

	public void setAvatarChargeup(long avatarChargeup) {
		this.avatarChargeup = avatarChargeup;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public double getStunChance() {
		return stunChance;
	}

	public void setStunChance(double stunChance) {
		this.stunChance = stunChance;
	}

	public double getStunDuration() {
		return stunDuration;
	}

	public void setStunDuration(double stunDuration) {
		this.stunDuration = stunDuration;
	}

	public boolean isCharged() {
		return charged;
	}

	public void setCharged(boolean charged) {
		this.charged = charged;
	}

	@Override
	public void remove() {
		stopWarmupSounds();
		if (warmupDisplay != null) {
			warmupDisplay.remove();
			warmupDisplay = null;
		}
		super.remove();
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Fire.LightningBurst.Enabled");
	}

	public class Bolt {

		private final LightningBurst ability;
		private Location start;
		private Location location;
		private Location lastOrigin;
		private final float initYaw;
		private final float initPitch;
		private double step;
		private final double max;
		private final double gap;
		private final int id;
		private final int arc;
		private final boolean doDamage;
		private final boolean mainHand;
		private final LightningDisplay display;

		public Bolt(LightningBurst ability, Location location, int id, double max, double gap, int arc, boolean doDamage, boolean mainHand) {
			this.ability = ability;
			this.location = location;
			this.start = location.clone();
			this.mainHand = mainHand;
			this.lastOrigin = getHandLocation().clone();
			this.id = id;
			this.max = max;
			this.arc = arc;
			this.gap = gap;
			this.doDamage = doDamage;
			this.display = doDamage ? new LightningDisplay(ability.burstStyle) : null;
			initYaw = location.getYaw();
			initPitch = location.getPitch();
		}

		private void progress() {
			if (this.step >= max) {
				removeBolt();
				return;
			}
			if (RegionProtection.isRegionProtected(player, location, LightningBurst.this) || !isTransparent(location.getBlock())) {
				removeBolt();
				return;
			}
			if (!followPlayer()) {
				removeBolt();
				return;
			}
			double distance = Math.min(gap, max - this.step);
			int steps = Math.max(1, (int) Math.ceil(distance / 0.5));
			double stepDistance = distance / steps;
			for (int i = 0; i < steps; i++) {
				this.step += stepDistance;
				location = location.add(location.getDirection().clone().multiply(stepDistance));
			}
			switch (rand.nextInt(3)) {
				case 0:
					location.setYaw(initYaw - arc);
					break;
				case 1:
					location.setYaw(initYaw + arc);
					break;
				default:
					location.setYaw(initYaw);
					break;
			}
			switch (rand.nextInt(3)) {
				case 0:
					location.setPitch(initPitch - arc);
					break;
				case 1:
					location.setPitch(initPitch + arc);
					break;
				default:
					location.setPitch(initPitch);
					break;
			}

			List<LightningArc> arcs = new ArrayList<>();
			arcs.add(LightningArc.create(start, location, ability.burstStyle));

			List<LightningWaterElectrify.Hit> waterHits = new ArrayList<>();
			if (doDamage && ability.waterElectrifyEnabled && ability.waterElectrifyDamage > 0) {
				waterHits = LightningWaterElectrify.collectHits(player, arcs, ability.burstStyle, ability.waterElectrifyRange);
				for (LightningWaterElectrify.Hit hit : waterHits) {
					arcs.add(hit.getArc());
				}
			}

			if (display != null) {
				display.render(arcs);
			}
			LightningEffects.spawnBlockImpactParticles(arcs);

			if (rand.nextInt(6) == 0) {
				ability.spawnAirParticle();
			}

			if (rand.nextInt(soundInterval) == 0) {
				location.getWorld().playSound(location, Sound.ENTITY_BEE_HURT, soundVolume, 0.2f);
			}

			if (!doDamage) {
				return;
			}

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 2)) {
				if (!(entity instanceof LivingEntity) || entity.equals(player)) {
					continue;
				}

				LivingEntity living = (LivingEntity) entity;
				DamageHandler.damageEntity(living, damage, ability);

				if (ThreadLocalRandom.current().nextDouble() <= ability.stunChance) {
					LightningElectrocution.apply(living, ability, ability.stunDuration);
				}
			}

			for (LightningWaterElectrify.Hit hit : waterHits) {
				LivingEntity living = hit.getTarget();
				if (ability.waterElectrified.add(living.getUniqueId())) {
					LightningWaterElectrify.applyDamage(ability, living, ability.waterElectrifyDamage, ability.stunChance, ability.stunDuration);
				}
			}
		}

		private Location getHandLocation() {
			return mainHand ? GeneralMethods.getMainHandLocation(player) : GeneralMethods.getOffHandLocation(player);
		}

		private boolean followPlayer() {
			Location currentOrigin = getHandLocation();

			if (lastOrigin == null) {
				lastOrigin = currentOrigin.clone();
				return true;
			}

			if (!lastOrigin.getWorld().equals(currentOrigin.getWorld())) {
				return false;
			}

			Vector shift = currentOrigin.toVector().subtract(lastOrigin.toVector());
			if (shift.lengthSquared() > 0.0) {
				start.add(shift);
				location.add(shift);
			}

			lastOrigin = currentOrigin.clone();
			return true;
		}

		private void removeBolt() {
			BOLTS.remove(id);
			if (display != null) {
				display.remove();
			}
		}

		public LightningBurst getAbility() {
			return ability;
		}

		public Location getLocation() {
			return location;
		}

		public float getInitYaw() {
			return initYaw;
		}

		public float getInitPitch() {
			return initPitch;
		}

		public double getStep() {
			return step;
		}

		public double getMax() {
			return max;
		}

		public double getGap() {
			return gap;
		}

		public int getId() {
			return id;
		}

		public int getArc() {
			return arc;
		}

		public boolean isDoDamage() {
			return doDamage;
		}
	}
}