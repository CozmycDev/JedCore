package com.jedk1.jedcore.ability.avatar.elementsphere;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author jedk1
 * @author Finn_Bueno_
 */
public class ESStream extends AvatarAbility implements AddonAbility {

	private boolean cancelAbility;
	private int requiredUses;
	private long regen;
	private Location stream;
	private Location origin;
	private Vector dir;
	private int angle;

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.KNOCKBACK)
	private double knockback;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.RADIUS)
	private double radius;

	public ESStream(Player player) {
		super(player);

		if (!hasAbility(player, ElementSphere.class)) {
			return;
		}

		ElementSphere currES = getAbility(player, ElementSphere.class);

		if (bPlayer.isOnCooldown("ESStream")) {
			return;
		}

		setFields();
		
		if (currES.getAirUses() < requiredUses 
				|| currES.getEarthUses() < requiredUses 
				|| currES.getFireUses() < requiredUses 
				|| currES.getWaterUses() < requiredUses) {
			return;
		}

		if (RegionProtection.isRegionProtected(this, player.getTargetBlock(getTransparentMaterialSet(), (int) range).getLocation())) {
			return;
		}
		
		if (cancelAbility) {
			currES.remove();
		} else {
			currES.setAirUses(currES.getAirUses()-requiredUses);
			currES.setEarthUses(currES.getEarthUses()-requiredUses);
			currES.setFireUses(currES.getFireUses()-requiredUses);
			currES.setWaterUses(currES.getWaterUses()-requiredUses);
		}
		
		stream = player.getEyeLocation();
		origin = player.getEyeLocation();
		dir = player.getEyeLocation().getDirection();
		angle = 0;
		
		start();

		if (!isRemoved()) {
			bPlayer.addCooldown("ESStream", getCooldown());
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		cooldown = config.getLong("Abilities.Avatar.ElementSphere.Stream.Cooldown");
		range = config.getDouble("Abilities.Avatar.ElementSphere.Stream.Range");
		damage = config.getDouble("Abilities.Avatar.ElementSphere.Stream.Damage");
		knockback = config.getDouble("Abilities.Avatar.ElementSphere.Stream.Knockback");
		requiredUses = config.getInt("Abilities.Avatar.ElementSphere.Stream.RequiredUses");
		cancelAbility = config.getBoolean("Abilities.Avatar.ElementSphere.Stream.EndAbility");
		radius = config.getInt("Abilities.Avatar.ElementSphere.Stream.ImpactCraterSize");
		regen = config.getLong("Abilities.Avatar.ElementSphere.Stream.ImpactRevert");
	}

	@Override
	public void progress() {
		if (!checkStreamValidity()) {
			return;
		}

		if (checkStreamRangeAndProtection()) {
			return;
		}

		handleNearbyEntities();

		updateStreamDirection();

		stream.add(dir);

		if (handleBlockCollision()) {
			return;
		}

		playStreamParticles();
	}

	private boolean checkStreamValidity() {
		return player != null && player.isOnline();
	}

	private boolean checkStreamRangeAndProtection() {
		if (origin.distance(stream) >= range || RegionProtection.isRegionProtected(player, stream, this)) {
			remove();
			return true;
		}
		return false;
	}

	private void handleNearbyEntities() {
		for (Entity e : GeneralMethods.getEntitiesAroundPoint(stream, 1.5)) {
			if (e instanceof Player && e == player) {
				continue;
			}
			applyStreamEffects(e);
		}
	}

	private void applyStreamEffects(Entity entity) {
		GeneralMethods.setVelocity(this, entity, dir.normalize().multiply(knockback));
		if (entity instanceof LivingEntity) {
			DamageHandler.damageEntity(entity, damage, this);
		}
	}

	private void updateStreamDirection() {
		if (!player.isDead() && hasAbility(player, ElementSphere.class)) {
			Location loc = stream.clone();
			dir = GeneralMethods.getDirection(loc, player.getTargetBlock(null, (int) range).getLocation()).normalize().multiply(1.2);
		}
	}

	private boolean handleBlockCollision() {
		if (!isTransparent(stream.getBlock())) {
			triggerCollisionEffects();
			remove();
			return true;
		}
		return false;
	}

	private void triggerCollisionEffects() {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		List<BlockState> blocks = getAffectedBlocks();
		damageNearbyEntitiesOnCollision();
		displayCollisionParticles();
		playCollisionSounds(rand);
		spawnFallingBlocks(rand, blocks);
	}

	private List<BlockState> getAffectedBlocks() {
		List<BlockState> blocks = new ArrayList<>();
		for (Location loc : GeneralMethods.getCircle(stream, (int) radius, 0, false, true, 0)) {
			if (JCMethods.isUnbreakable(loc.getBlock()) || RegionProtection.isRegionProtected(this, loc)) continue;
			blocks.add(loc.getBlock().getState());
			new RegenTempBlock(loc.getBlock(), Material.AIR, Material.AIR.createBlockData(), regen, false);
		}
		return blocks;
	}

	private void damageNearbyEntitiesOnCollision() {
        GeneralMethods.getEntitiesAroundPoint(stream, radius).stream().filter(e -> !(e instanceof Player) || e != player).filter(e -> !RegionProtection.isRegionProtected(this, e.getLocation()) && (!(e instanceof Player targetPlayer) || !Commands.invincible.contains((targetPlayer).getName()))).forEach(e -> {
            GeneralMethods.setVelocity(this, e, dir.normalize().multiply(knockback));
            if (e instanceof LivingEntity) DamageHandler.damageEntity(e, damage, this);
        });
	}

	private void displayCollisionParticles() {
		ParticleEffect.FLAME.display(stream, 20, 0.5F, 0.5F, 0.5F, 0.5F);
		ParticleEffect.SMOKE_LARGE.display(stream, 20, 0.5F, 0.5F, 0.5F, 0.5F);
		ParticleEffect.FIREWORKS_SPARK.display(stream, 20, 0.5F, 0.5F, 0.5F, 0.5F);
		ParticleEffect.SMOKE_LARGE.display(stream, 20, 0.5F, 0.5F, 0.5F, 0.5F);
		ParticleEffect.EXPLOSION_HUGE.display(stream, 5, 0.5F, 0.5F, 0.5F, 0.5F);
	}

	private void playCollisionSounds(ThreadLocalRandom rand) {
		stream.getWorld().playSound(stream, rand.nextBoolean() ? Sound.ENTITY_FIREWORK_ROCKET_BLAST : Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 1f, 1f);
		stream.getWorld().playSound(stream, rand.nextBoolean() ? Sound.ENTITY_FIREWORK_ROCKET_TWINKLE : Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 1f, 1f);
	}

	private void spawnFallingBlocks(ThreadLocalRandom rand, List<BlockState> blocks) {
		for (BlockState block : blocks) {
			double x = (rand.nextBoolean() ? -1 : 1) * rand.nextDouble() / 3;
			double z = (rand.nextBoolean() ? -1 : 1) * rand.nextDouble() / 3;
			new TempFallingBlock(block.getLocation().add(0, 1, 0), block.getBlockData(), dir.clone().add(new Vector(x, 0, z)).normalize().multiply(-1), this);
		}
	}

	private void playStreamParticles() {
		angle += 20;
		if (angle > 360) {
			angle = 0;
		}
		for (int i = 0; i < 4; i++) {
			playIndividualStreamParticles(i);
		}
	}

	private void playIndividualStreamParticles(int particleIndex) {
		for (double d = -4; d <= 0; d += 0.1) {
			if (origin.distance(stream) < d) continue;
			Location l = stream.clone().add(dir.clone().normalize().multiply(d));
			double r = Math.min(0.75, d * -1 / 5);
			Vector ov = GeneralMethods.getOrthogonalVector(dir, angle + (90 * particleIndex) + d, r);
			Location pl = l.clone().add(ov.clone());
			displayStreamParticle(pl, particleIndex);
		}
	}

	private void displayStreamParticle(Location location, int index) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		switch (index) {
			case 0:
				ParticleEffect flame = bPlayer.hasSubElement(Element.BLUE_FIRE) ? ParticleEffect.SOUL_FIRE_FLAME : ParticleEffect.FLAME;
				flame.display(location, 1, 0.05F, 0.05F, 0.05F, 0.005F);
				break;
			case 1:
				String color = "#FFFFFF";
				float offset = 0.05F;
				float speed = 0.005F;
				int viewDistance = 50;
				if (rand.nextInt(30) == 0) {
					JCMethods.displayColoredParticles(color, location, 1, 0, 0, 0, speed);
				} else {
					JCMethods.displayColoredParticles(color, location, 1, offset, offset, offset, speed, viewDistance);
				}
				break;
			case 2:
				GeneralMethods.displayColoredParticle("06C1FF", location);
				break;
			case 3:
				GeneralMethods.displayColoredParticle("754719", location);
				break;
		}
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return stream;
	}

	@Override
	public String getName() {
		return "ElementSphereStream";
	}
	
	@Override
	public boolean isHiddenAbility() {
		return true;
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
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	@Override
	public String getDescription() {
		return null;
	}

	public double getKnockback() {
		return knockback;
	}

	public void setKnockback(double knockback) {
		this.knockback = knockback;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public boolean cancelsAbility() {
		return cancelAbility;
	}

	public void setCancelsAbility(boolean cancelAbility) {
		this.cancelAbility = cancelAbility;
	}

	public int getRequiredUses() {
		return requiredUses;
	}

	public void setRequiredUses(int requiredUses) {
		this.requiredUses = requiredUses;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public long getRegenTime() {
		return regen;
	}

	public void setRegenTime(long regen) {
		this.regen = regen;
	}

	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(Location origin) {
		this.origin = origin;
	}

	public Vector getDirection() {
		return dir;
	}

	public void setDirection(Vector dir) {
		this.dir = dir;
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
}
