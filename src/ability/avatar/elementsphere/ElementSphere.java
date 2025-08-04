package com.jedk1.jedcore.ability.avatar.elementsphere;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager.MultiAbilityInfoSub;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

public class ElementSphere extends AvatarAbility implements AddonAbility, MultiAbility {

	protected static final ConcurrentMap<Player, HashMap<Integer, String>> abilities = new ConcurrentHashMap<>();
	private static final ArrayList<MultiAbilityInfoSub> multiAbilityInfo = new ArrayList<>();

	static {
		multiAbilityInfo.add(new MultiAbilityInfoSub("", Element.AIR));
		multiAbilityInfo.add(new MultiAbilityInfoSub("", Element.EARTH));
		multiAbilityInfo.add(new MultiAbilityInfoSub("", Element.FIRE));
		multiAbilityInfo.add(new MultiAbilityInfoSub("", Element.WATER));
		multiAbilityInfo.add(new MultiAbilityInfoSub("", Element.AVATAR));
	}

	private World world;
	private int airUses;
	private int fireUses;
	private int waterUses;
	private int earthUses;
	private boolean setup;
	private Location location;
	private double yaw;
	private int point;
	private long endTime;
	private long lastClickTime;

	@Attribute(Attribute.COOLDOWN)
	public long cooldown;
	@Attribute(Attribute.DURATION)
	public long duration;
	@Attribute(Attribute.HEIGHT)
	private double height;
	@Attribute(Attribute.SPEED)
	private double speed;

	public ElementSphere(Player player) {
		super(player);

		ElementSphere oldES = getAbility(player, ElementSphere.class);

		if (handleExistingSphere(player, oldES)) {
			return;
		}

		if (canStartNewSphere()) {
			initializeNewSphere(player);
		}
	}

	private boolean handleExistingSphere(Player player, ElementSphere oldES) {
		if (oldES != null) {
			if (player.isSneaking()) {
				oldES.prepareCancel();
			} else {
				if (oldES.setup) {
					handleElementSwitch(player);
				}
			}
			return true;
		}
		return false;
	}

	private void handleElementSwitch(Player player) {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			return;
		}

		switch (player.getInventory().getHeldItemSlot()) {
			case 0:
				if (checkPermission(player, "Air")) new ESAir(player);
				break;
			case 1:
				if (checkPermission(player, "Earth")) new ESEarth(player);
				break;
			case 2:
				if (checkPermission(player, "Fire")) new ESFire(player);
				break;
			case 3:
				if (checkPermission(player, "Water")) new ESWater(player);
				break;
			case 4:
				if (checkPermission(player, "Stream")) new ESStream(player);
				break;
		}
	}

	private boolean checkPermission(Player player, String element) {
		return player.hasPermission("bending.ability.ElementSphere." + element);
	}

	private boolean canStartNewSphere() {
		return bPlayer.canBend(this);
	}

	private void initializeNewSphere(Player player) {
		setFields();
		location = player.getLocation().clone().subtract(0, 1, 0);
		world = player.getWorld();
		endTime = System.currentTimeMillis() + duration;
		start();

		if (!isRemoved()) {
			bindAndCooldown(player);
			enableFlight(player);
			checkBoundAbilityName();
		}
	}

	private void bindAndCooldown(Player player) {
		MultiAbilityManager.bindMultiAbility(player, "ElementSphere");
		bPlayer.addCooldown(this);
	}

	private void enableFlight(Player player) {
		flightHandler.createInstance(player, this.getName());
	}

	private void checkBoundAbilityName() {
		if (ChatColor.stripColor(bPlayer.getBoundAbilityName()) == null) {
			remove();
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		airUses = config.getInt("Abilities.Avatar.ElementSphere.Air.Uses");
		fireUses = config.getInt("Abilities.Avatar.ElementSphere.Fire.Uses");
		waterUses = config.getInt("Abilities.Avatar.ElementSphere.Water.Uses");
		earthUses = config.getInt("Abilities.Avatar.ElementSphere.Earth.Uses");
		cooldown = config.getLong("Abilities.Avatar.ElementSphere.Cooldown");
		duration = config.getLong("Abilities.Avatar.ElementSphere.Duration");
		height = config.getDouble("Abilities.Avatar.ElementSphere.MaxControlledHeight");
		speed = config.getDouble("Abilities.Avatar.ElementSphere.FlySpeed");
	}

	@Override
	public void progress() {
		if (!checkPlayerValidity()) {
			return;
		}

		if (!checkAbilityPrerequisites()) {
			return;
		}

		if (isDurationOver()) {
			remove();
			return;
		}

		if (!hasUsableElements()) {
			remove();
			return;
		}

		handlePlayerMovement();
		handleFlight();
		handleEntityPush();
		updateLocationAndPlayParticles();

		setup = true;
	}

	private boolean checkPlayerValidity() {
		return !player.isDead() && player.isOnline() && world == player.getWorld() && !player.getGameMode().equals(GameMode.SPECTATOR);
	}

	private boolean checkAbilityPrerequisites() {
		return bPlayer.isToggled() && MultiAbilityManager.hasMultiAbilityBound(player, "ElementSphere");
	}

	private boolean isDurationOver() {
		return duration > 0 && System.currentTimeMillis() > endTime;
	}

	private boolean hasUsableElements() {
		return airUses > 0 || fireUses > 0 || waterUses > 0 || earthUses > 0;
	}

	private void handlePlayerMovement() {
		player.setFallDistance(0);
		if (player.isSneaking()) {
			player.setVelocity(player.getLocation().getDirection().multiply(speed));
		}
	}

	private void handleFlight() {
		Block block = getGround();
		if (block != null) {
			double dy = player.getLocation().getY() - block.getY();
			if (dy > height) {
				removeFlight();
			} else {
				allowFlight();
			}
		}
	}

	private void handleEntityPush() {
		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 2.5)) {
			if (isPushableEntity(entity)) {
				entity.setVelocity(entity.getLocation().toVector().subtract(player.getLocation().toVector()).multiply(1));
			}
		}
	}

	private boolean isPushableEntity(Entity entity) {
		return entity instanceof LivingEntity &&
				entity.getEntityId() != player.getEntityId() &&
				!(entity instanceof ArmorStand) &&
				!RegionProtection.isRegionProtected(player, entity.getLocation(), this);
	}

	private void updateLocationAndPlayParticles() {
		location = player.getLocation().clone().subtract(0, 1, 0);
		playParticles();
	}

	private void allowFlight() {
		if (!player.getAllowFlight()) {
			player.setAllowFlight(true);
		}
		if (!player.isFlying()) {
			player.setFlying(true);
		}
	}

	private void removeFlight() {
		if (player.getAllowFlight()) {
			player.setAllowFlight(false);
		}
		if (player.isFlying()) {
			player.setFlying(false);
		}
	}

	private Block getGround() {
		Block standingblock = player.getLocation().getBlock();

		for (int i = 0; i <= height + 5; i++) {
			Block block = standingblock.getRelative(BlockFace.DOWN, i);
			if (GeneralMethods.isSolid(block) || block.isLiquid()) {
				return block;
			}
		}

		return null;
	}

	private void playParticles() {
		playAirParticles();
		playFireParticles();
		playWaterEarthParticles();
		updatePointCounter();
	}

	private void playAirParticles() {
		if (airUses != 0) {
			double currentYaw = yaw + 40;
			yaw = currentYaw;
			Location fakeLoc = createRotatedLocation(location.clone(), 0, currentYaw);
			Vector direction = fakeLoc.getDirection();
			for (double j = -180; j <= 180; j += 45) {
				Location tempLoc = calculateAirParticleLocation(fakeLoc, direction, j);
				displayAirParticle(tempLoc);
			}
		}
	}

	private Location createRotatedLocation(Location baseLoc, double pitchOffset, double yawOffset) {
		Location newLoc = baseLoc.clone();
		newLoc.setPitch((float) pitchOffset);
		newLoc.setYaw((float) yawOffset);
		return newLoc;
	}

	private Location calculateAirParticleLocation(Location center, Vector direction, double angleDegrees) {
		Location tempLoc = center.clone();
		double angleRadians = Math.toRadians(angleDegrees);
		Vector newDir = direction.clone().multiply(2 * Math.cos(angleRadians));
		tempLoc.add(newDir);
		tempLoc.setY(tempLoc.getY() + 2 + (2 * Math.sin(angleRadians)));
		return tempLoc;
	}

	private void displayAirParticle(Location loc) {
		String color = "#FFFFFF";
		int count = 1;
		float offsetX = 0;
		float offsetY = 0;
		float offsetZ = 0;
		float particleSpeed = 0.003f;
		int viewDistance = 50;

		if (ThreadLocalRandom.current().nextInt(30) == 0) {
			JCMethods.displayColoredParticles(color, loc, count, offsetX, offsetY, offsetZ, particleSpeed);
		} else {
			JCMethods.displayColoredParticles(color, loc, count, offsetX, offsetY, offsetZ, particleSpeed, viewDistance);
		}
	}

	private void playFireParticles() {
		if (fireUses != 0) {
			ParticleEffect flame = bPlayer.hasSubElement(Element.BLUE_FIRE) ? ParticleEffect.SOUL_FIRE_FLAME : ParticleEffect.FLAME;
			for (int i = -180; i < 180; i += 40) {
				Location particleLoc = calculateCircularLocation(location, 2, i, 2);
				flame.display(particleLoc, 0, 0, 0, 0, 1);
				JCMethods.emitLight(particleLoc);
			}
		}
	}

	private Location calculateCircularLocation(Location center, double radius, double angleOffsetDegrees, double yOffset) {
		double angleRadians = Math.toRadians(angleOffsetDegrees);
		double x = radius * Math.cos(angleRadians + point);
		double z = radius * Math.sin(angleRadians + point);
		return center.clone().add(x, yOffset, z);
	}

	private void playWaterEarthParticles() {
		Location centerLoc = location.clone().add(0, 2, 0);
		double xRotation = Math.PI * 2.1 / 3;
		double yawRadians = -(centerLoc.getYaw() * Math.PI / 180 - 1.575);

		for (int i = -180; i < 180; i += 30) {
			double angle = Math.toRadians(i);
			Vector v = new Vector(Math.cos(angle + point), Math.sin(angle + point), 0.0D).multiply(2);
			Vector v1 = v.clone();

			rotateAroundAxisX(v, xRotation);
			rotateAroundAxisY(v, yawRadians);
			rotateAroundAxisX(v1, -xRotation);
			rotateAroundAxisY(v1, yawRadians);

			if (waterUses != 0) {
				GeneralMethods.displayColoredParticle("06C1FF", centerLoc.clone().add(v));
			}
			if (earthUses != 0) {
				GeneralMethods.displayColoredParticle("754719", centerLoc.clone().add(v1));
			}
		}
	}

	private void updatePointCounter() {
		point = (point + 1) % 360;
	}

	private void rotateAroundAxisX(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double y = v.getY() * cos - v.getZ() * sin;
		double z = v.getY() * sin + v.getZ() * cos;
		v.setY(y).setZ(z);
	}

	private void rotateAroundAxisY(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double x = v.getX() * cos + v.getZ() * sin;
		double z = v.getX() * -sin + v.getZ() * cos;
		v.setX(x).setZ(z);
	}

	@Override
	public void remove() {
		super.remove();
		MultiAbilityManager.unbindMultiAbility(player);
		flightHandler.removeInstance(player, this.getName());
	}

	public void prepareCancel() {
		if (System.currentTimeMillis() < lastClickTime + 500L) {
			remove();
		} else {
			lastClickTime = System.currentTimeMillis();
		}
	}

	public int getAirUses() {
		return airUses;
	}

	public void setAirUses(int airuses) {
		this.airUses = airuses;
	}

	public int getEarthUses() {
		return earthUses;
	}

	public void setEarthUses(int earthuses) {
		this.earthUses = earthuses;
	}

	public int getFireUses() {
		return fireUses;
	}

	public void setFireUses(int fireuses) {
		this.fireUses = fireuses;
	}

	public int getWaterUses() {
		return waterUses;
	}

	public void setWaterUses(int wateruses) {
		this.waterUses = wateruses;
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
	public boolean requireAvatar() {
		return false;
	}
	
	@Override
	public String getName() {
		return "ElementSphere";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Avatar.ElementSphere.Description");
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Avatar.ElementSphere.Enabled");
	}

	@Override
	public ArrayList<MultiAbilityInfoSub> getMultiAbilities() {
		FileConfiguration lang = getLanguageConfig();

		String airName = lang.getString("Abilities.Avatar.ElementSphereAir.Name");
		String fireName = lang.getString("Abilities.Avatar.ElementSphereFire.Name");
		String waterName = lang.getString("Abilities.Avatar.ElementSphereWater.Name");
		String earthName = lang.getString("Abilities.Avatar.ElementSphereEarth.Name");
		String streamName = lang.getString("Abilities.Avatar.ElementSphereStream.Name");

		multiAbilityInfo.get(0).setName(airName);
		multiAbilityInfo.get(1).setName(earthName);
		multiAbilityInfo.get(2).setName(fireName);
		multiAbilityInfo.get(3).setName(waterName);
		multiAbilityInfo.get(4).setName(streamName);

		return multiAbilityInfo;
	}
}
