package com.jedk1.jedcore.ability.chiblocking;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.AbilitySelector;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DaggerThrow extends ChiAbility implements AddonAbility {
	private static final List<AbilityInteraction> INTERACTIONS = new ArrayList<>();
	private boolean particles;

	@Attribute(Attribute.DAMAGE)
	private double damage;

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private boolean limitEnabled;
	@Attribute("MaxShots")
	private int maxShots;

	private boolean requireArrows;
	// require arrows (takes arrows from inv)
	private boolean allowPickup;
	// allow arrow pickup (disables pickup and removes the arrow entities)

	private long endTime;
	private int shots = 1;
	private int hits = 0;
	private final List<Arrow> arrows = new ArrayList<>();

	public DaggerThrow(Player player) {
		super(player);

		if (!bPlayer.canBend(this)) {
			return;
		}

		if (bPlayer.isOnCooldown("DaggerThrowShot")) {
			return;
		}

		if (hasAbility(player, DaggerThrow.class)) {
			DaggerThrow dt = getAbility(player, DaggerThrow.class);
			dt.shootArrow();
			return;
		}

		setFields();

		start();
		if (!isRemoved()) {
			shootArrow();
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		cooldown = config.getLong("Abilities.Chi.DaggerThrow.Cooldown");
		limitEnabled = config.getBoolean("Abilities.Chi.DaggerThrow.MaxDaggers.Enabled");
		maxShots = config.getInt("Abilities.Chi.DaggerThrow.MaxDaggers.Amount");
		particles = config.getBoolean("Abilities.Chi.DaggerThrow.ParticleTrail");
		damage = config.getDouble("Abilities.Chi.DaggerThrow.Damage");
		requireArrows = config.getBoolean("Abilities.Chi.DaggerThrow.RequireArrows");
		allowPickup = config.getBoolean("Abilities.Chi.DaggerThrow.AllowPickup");

		loadInteractions();
	}

	private void loadInteractions() {
		INTERACTIONS.clear();

		String path = "Abilities.Chi.DaggerThrow.Interactions";

		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		ConfigurationSection section = config.getConfigurationSection(path);
		for (String abilityName : section.getKeys(false)) {
			INTERACTIONS.add(new AbilityInteraction(abilityName));
		}
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > endTime) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}
		if (shots > maxShots && limitEnabled) {
			bPlayer.addCooldown(this);
			remove();
		}
	}

	private void shootArrow() {
		shots++;
		Location location = player.getEyeLocation();

		Vector vector = location.toVector().
				add(location.getDirection().multiply(2.5)).
				toLocation(location.getWorld()).toVector().
				subtract(player.getEyeLocation().toVector());

		if (requireArrows) JCMethods.removeItemFromInventory(player, Material.ARROW, 1);
		Arrow arrow = player.launchProjectile(Arrow.class);
		arrow.setVelocity(vector);
		arrow.getLocation().setDirection(vector);
		arrow.setKnockbackStrength(0);
		arrow.setBounce(false);
		arrow.setMetadata("daggerthrow", new FixedMetadataValue(JedCore.plugin, "1"));
		if (!allowPickup) arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

		if (particles) {
			arrow.setCritical(true);
		}

		arrows.add(arrow);
		endTime = System.currentTimeMillis() + 500;
		bPlayer.addCooldown("DaggerThrowShot", 100);
	}

	public void damageEntityFromArrow(LivingEntity entity, Arrow arrow) {
		if (!(arrow.getShooter() instanceof Player shooter)) return;

		if (RegionProtection.isRegionProtected(shooter, arrow.getLocation(), "DaggerThrow")) return;

		arrow.setVelocity(new Vector(0, 0, 0));
		entity.setNoDamageTicks(0);

		double prevHealth = entity.getHealth();

		DamageHandler.damageEntity(entity, damage, this);

		if (prevHealth > entity.getHealth()) {
			arrow.remove();
		}

		if (!(entity instanceof Player target)) {
			return;
		}

		DaggerThrow daggerThrow = CoreAbility.getAbility(shooter, DaggerThrow.class);
		if (daggerThrow == null) {
			return;
		}

		daggerThrow.hits++;
		BendingPlayer targetBPlayer = BendingPlayer.getBendingPlayer(target);

		for (AbilityInteraction interaction : INTERACTIONS) {
			if (!interaction.enabled || daggerThrow.hits < interaction.hitRequirement) {
				continue;
			}

			CoreAbility abilityDefinition = AbilitySelector.getAbility(interaction.name);
			if (abilityDefinition == null) {
				continue;
			}

			CoreAbility ability = CoreAbility.getAbility(target, abilityDefinition.getClass());
			if (ability == null) {
				continue;
			}

			ability.remove();
			targetBPlayer.addCooldown(ability, interaction.cooldown);
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
		return arrows.stream().map(Arrow::getLocation).collect(Collectors.toList());
	}

	@Override
	public double getCollisionRadius() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getDouble("Abilities.Chi.DaggerThrow.AbilityCollisionRadius");
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.isRemovingFirst()) {
			Location location = collision.getLocationFirst();

			Optional<Arrow> collidedObject = arrows.stream().filter(arrow -> arrow.getLocation().equals(location)).findAny();

			if (collidedObject.isPresent()) {
				arrows.remove(collidedObject.get());
				collidedObject.get().remove();
			}
		}
	}

	@Override
	public String getName() {
		return "DaggerThrow";
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
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return "* JedCore Addon *\n" + config.getString("Abilities.Chi.DaggerThrow.Description");
	}

	public boolean hasParticleTrail() {
		return particles;
	}

	public double getDamage() {
		return damage;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public int getShots() {
		return shots;
	}

	public void setShots(int shots) {
		this.shots = shots;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public boolean isLimitEnabled() {
		return limitEnabled;
	}

	public void setLimitEnabled(boolean limitEnabled) {
		this.limitEnabled = limitEnabled;
	}

	public int getMaxShots() {
		return maxShots;
	}

	public void setMaxShots(int maxShots) {
		this.maxShots = maxShots;
	}

	public int getHits() {
		return hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
	}

	public List<Arrow> getArrows() {
		return arrows;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Chi.DaggerThrow.Enabled");
	}

	private class AbilityInteraction {
		public boolean enabled;
		public long cooldown;
		public int hitRequirement;
		public String name;

		public AbilityInteraction(String abilityName) {
			this.name = abilityName;
			loadConfig();
		}

		public void loadConfig() {
			ConfigurationSection config = JedCoreConfig.getConfig(player);
			this.enabled = config.getBoolean("Abilities.Chi.DaggerThrow.Interactions." + name + ".Enabled", true);
			this.cooldown = config.getLong("Abilities.Chi.DaggerThrow.Interactions." + name + ".Cooldown", 1000);
			this.hitRequirement = config.getInt("Abilities.Chi.DaggerThrow.Interactions." + name + ".HitsRequired", 1);
		}
	}
}
