package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class MagnetShield extends MetalAbility implements AddonAbility {

	private static List<Material> METAL_LIST;

	private boolean isClickState = false;
	private long startTime;

	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.RANGE)
	private double range;

	private double velocity;
	private boolean repelArrows;
	private boolean repelLivingEntities;
	private long shiftCooldown;
	private long clickCooldown;

	public MagnetShield(Player player) {
		super(player);

		if (!bPlayer.canBendIgnoreCooldowns(this) || !bPlayer.canMetalbend()) {
			return;
		}

		if (!bPlayer.canBend(this)) {
			return;
		}

		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.5f);

		setFields();
		loadMaterialsFromConfig();
		start();
	}

	public MagnetShield(Player player, boolean isClickState) {
		super(player);
		
		if (!bPlayer.canBendIgnoreCooldowns(this) || !bPlayer.canMetalbend()) {
			return;
		}

		if (!bPlayer.canBend(this)) {
			return;
		}
		
		if (hasAbility(player, MagnetShield.class)) {
			getAbility(player, MagnetShield.class).remove();
			return;
		}

		setFields();

		if (isClickState) {
			this.isClickState = true;
			startTime = System.currentTimeMillis();
			bPlayer.addCooldown(this, duration + 1);
		}

		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.5f);

		loadMaterialsFromConfig();
		start();
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

		if (isClickState) {
			if (System.currentTimeMillis() - startTime >= duration) {
				bPlayer.addCooldown(this, clickCooldown);
				remove();
				return;
			}
		} else {
			if (!player.isSneaking()) {
				bPlayer.addCooldown(this, shiftCooldown);
				remove();
				return;
			}
		}

		renderMagneticFieldLines();
		repelMetal();
	}
	
	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public String getName() {
		return "MagnetShield";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.MagnetShield.Description");
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.MagnetShield.Enabled");
	}

	private void renderMagneticFieldLines() {
		Location playerLocation = player.getLocation();
		int numLoops = 5;
		int pointsPerLoop = 30;
		double verticalSpacing = 0.06;

		for (int loop = 0; loop < numLoops; loop++) {
			double currentRadius = range * (1 - (double) loop / numLoops);
			double currentHeight = ((loop - numLoops / 2.0) * verticalSpacing) + 0.3;

			for (int i = 0; i < pointsPerLoop; i++) {
				double angle = 2 * Math.PI * i / pointsPerLoop;
				double x = currentRadius * Math.cos(angle);
				double z = currentRadius * Math.sin(angle);
				Location particleLocation = playerLocation.clone().add(x, currentHeight, z);
				JedCore.plugin.getParticleAdapter().displayMagneticParticles(particleLocation);
			}
		}
	}

	private void loadMaterialsFromConfig() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		List<String> materialNames = config.getStringList("Abilities.Earth.MagnetShield.Materials");
		METAL_LIST = new ArrayList<>();

		for (String materialName : materialNames) {
			Material material = Material.getMaterial(materialName.toUpperCase());
			if (material != null) {
				METAL_LIST.add(material);
			} else {
				System.out.println("Invalid material in config: " + materialName);
			}
		}
	}

	private void repelMetal() {
		for (Entity e : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), range)) {
			if (e instanceof Item) {
				Item i = (Item) e;
				if (isMetalMaterial(i.getItemStack().getType())) {
					Vector direction = GeneralMethods.getDirection(player.getLocation(), i.getLocation()).multiply(velocity);
					i.setVelocity(direction);
				}
			} else if (e instanceof FallingBlock) {
				FallingBlock fb = (FallingBlock) e;

				if (isMetalMaterial(fb.getBlockData().getMaterial())) {
					Vector direction = GeneralMethods.getDirection(player.getLocation(), fb.getLocation()).multiply(velocity);
					fb.setVelocity(direction);
					fb.setDropItem(false);
				}
			} else if (e instanceof Arrow && repelArrows) {
				Arrow arrow = (Arrow) e;
				Vector currentVelocity = arrow.getVelocity();
				Vector reversedVelocity = currentVelocity.multiply(-1);
				arrow.setVelocity(reversedVelocity);
			} else if (e instanceof LivingEntity && repelLivingEntities) {
				LivingEntity livingEntity = (LivingEntity) e;

				if (livingEntity.getUniqueId().equals(player.getUniqueId())) continue;

				EntityEquipment equipment = livingEntity.getEquipment();
				if (equipment == null) continue;

				ItemStack mainHand = equipment.getItemInMainHand();
				ItemStack offHand = equipment.getItemInOffHand();
				ItemStack helmet = equipment.getHelmet();
				ItemStack chestplate = equipment.getChestplate();
				ItemStack leggings = equipment.getLeggings();
				ItemStack boots = equipment.getBoots();

				boolean isMetal = isMetalItem(mainHand) || isMetalItem(offHand) ||
						isMetalItem(helmet) || isMetalItem(chestplate) ||
						isMetalItem(leggings) || isMetalItem(boots);

				if (isMetal) {
					Vector direction = GeneralMethods.getDirection(player.getLocation(), e.getLocation()).multiply(velocity);
					livingEntity.setVelocity(direction);
				}
			}
		}
	}

	private boolean isMetalItem(ItemStack itemStack) {
		return itemStack != null && METAL_LIST.contains(itemStack.getType());
	}

	private boolean isMetalMaterial(Material material) {
		return METAL_LIST.contains(material);
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		duration = config.getLong("Abilities.Earth.MagnetShield.Duration");
        shiftCooldown = config.getLong("Abilities.Earth.MagnetShield.Cooldowns.Shift");
		clickCooldown = config.getLong("Abilities.Earth.MagnetShield.Cooldowns.Click");
		range = config.getDouble("Abilities.Earth.MagnetShield.Range");
		repelArrows = config.getBoolean("Abilities.Earth.MagnetShield.RepelArrows");
		repelLivingEntities = config.getBoolean("Abilities.Earth.MagnetShield.RepelLivingEntities");
		velocity = config.getDouble("Abilities.Earth.MagnetShield.Velocity");
	}
}