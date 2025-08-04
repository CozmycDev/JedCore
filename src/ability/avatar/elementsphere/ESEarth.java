package com.jedk1.jedcore.ability.avatar.elementsphere;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class ESEarth extends AvatarAbility implements AddonAbility {

	static Material[] unbreakables = { Material.BEDROCK, Material.BARRIER, Material.NETHER_PORTAL, Material.END_PORTAL,
			Material.END_PORTAL_FRAME, Material.ENDER_CHEST, Material.CHEST, Material.TRAPPED_CHEST };

	private TempFallingBlock tfb;
	private long revertDelay;

	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute("Size")
	private int impactSize;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;

	public ESEarth(Player player) {
		super(player);
		if (!hasAbility(player, ElementSphere.class)) {
			return;
		}
		ElementSphere currES = getAbility(player, ElementSphere.class);
		if (currES.getEarthUses() == 0) {
			return;
		}
		if (bPlayer.isOnCooldown("ESEarth")) {
			return;
		}
		if (RegionProtection.isRegionProtected(this, player.getTargetBlock(getTransparentMaterialSet(), 40).getLocation())) {
			return;
		}
		setFields();
		start();
		if (!isRemoved()) {
			bPlayer.addCooldown("ESEarth", getCooldown());
			currES.setEarthUses(currES.getEarthUses() - 1);
			Location location = player.getEyeLocation().clone().add(player.getEyeLocation().getDirection().multiply(1));
			tfb = new TempFallingBlock(location, Material.DIRT.createBlockData(), location.getDirection().multiply(3), this);
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		revertDelay = config.getLong("Abilities.Avatar.ElementSphere.Earth.ImpactRevert");
		damage = config.getDouble("Abilities.Avatar.ElementSphere.Earth.Damage");
		impactSize = config.getInt("Abilities.Avatar.ElementSphere.Earth.ImpactCraterSize");
		cooldown = config.getLong("Abilities.Avatar.ElementSphere.Earth.Cooldown");
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline()) {
			tfb.remove();
			remove();
			return;
		}
		if (tfb.getFallingBlock().isDead()) {
			remove();
			return;
		}
		if (RegionProtection.isRegionProtected(this, tfb.getLocation())){
			remove();
			return;
		}

		EarthAbility.playEarthbendingSound(tfb.getLocation());

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(tfb.getLocation(), 2.5)) {
			if (entity instanceof LivingEntity && !(entity instanceof ArmorStand) && entity.getEntityId() != player.getEntityId() && !RegionProtection.isRegionProtected(this, entity.getLocation()) && !((entity instanceof Player targetPlayer) && Commands.invincible.contains(targetPlayer.getName()))) {
				DamageHandler.damageEntity(entity, damage, this);
			}
		}
	}

	// Unused
	public static void explodeEarth(TempFallingBlock tempfallingblock) {
		FallingBlock fb = tempfallingblock.getFallingBlock();
		ESEarth es = (ESEarth) tempfallingblock.getAbility();
		Player player = es.getPlayer();

		ParticleEffect.SMOKE_LARGE.display(fb.getLocation(), 0, 0, 0, 0.3F, 25);
		fb.getWorld().playSound(fb.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);

		ThreadLocalRandom rand = ThreadLocalRandom.current();

		for (Location l : GeneralMethods.getCircle(fb.getLocation(), es.impactSize, 1, false, true, 0)) {
			if (isBreakable(l.getBlock()) && !RegionProtection.isRegionProtected(player, l, "ElementSphere") && EarthAbility.isEarthbendable(player, l.getBlock())) {
				ParticleEffect.SMOKE_LARGE.display(l, 0, 0, 0, 0.1F, 2);
				new RegenTempBlock(l.getBlock(), Material.AIR, Material.AIR.createBlockData(), rand.nextInt((int) es.revertDelay - (int) (es.revertDelay - 1000)) + (es.revertDelay - 1000), false);
			}

			if (GeneralMethods.isSolid(l.getBlock().getRelative(BlockFace.DOWN)) && isBreakable(l.getBlock()) && ElementalAbility.isAir(l.getBlock().getType()) && rand.nextInt(20) == 0 && EarthAbility.isEarthbendable(player, l.getBlock().getRelative(BlockFace.DOWN))) {
				Material type = l.getBlock().getRelative(BlockFace.DOWN).getType();
				new RegenTempBlock(l.getBlock(), type, type.createBlockData(), rand.nextInt((int) es.revertDelay - (int) (es.revertDelay - 1000)) + (es.revertDelay - 1000));
			}
		}

		tempfallingblock.remove();
	}

	public static boolean isBreakable(Block block) {
		return !Arrays.asList(unbreakables).contains(block.getType());
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return tfb != null ? tfb.getLocation() : null;
	}

	@Override
	public String getName() {
		return "ElementSphereEarth";
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

	public long getRevertDelay() {
		return revertDelay;
	}

	public void setRevertDelay(long revertDelay) {
		this.revertDelay = revertDelay;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public int getImpactSize() {
		return impactSize;
	}

	public void setImpactSize(int impactSize) {
		this.impactSize = impactSize;
	}

	public TempFallingBlock getTempFallingBlock() {
		return tfb;
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
