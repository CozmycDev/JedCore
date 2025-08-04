package com.jedk1.jedcore.ability.passive;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ParticleEffect;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class WallRun extends ChiAbility implements AddonAbility {

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long duration;

	private boolean enabled;
	
	private boolean particles;
	private boolean air;
	private boolean earth;
	private boolean water;
	private boolean fire;
	private boolean chi;

	private List<String> invalid;

	public WallRun(Player player) {
		super(player);

		setFields();
		if (!enabled) return;
		
		if (bPlayer.isOnCooldown("WallRun")) return;
		
		if (hasAbility(player, WallRun.class)) {
			getAbility(player, WallRun.class).remove();
			return;
		}

		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}

		if (isEligible() && !JCMethods.isDisabledWorld(player.getWorld())) {
			start();
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		enabled = config.getBoolean("Abilities.Passives.WallRun.Enabled");
		cooldown = config.getLong("Abilities.Passives.WallRun.Cooldown");
		duration = config.getLong("Abilities.Passives.WallRun.Duration");
		particles = config.getBoolean("Abilities.Passives.WallRun.Particles");
		air = config.getBoolean("Abilities.Passives.WallRun.Air");
		earth = config.getBoolean("Abilities.Passives.WallRun.Earth");
		water = config.getBoolean("Abilities.Passives.WallRun.Water");
		fire = config.getBoolean("Abilities.Passives.WallRun.Fire");
		chi = config.getBoolean("Abilities.Passives.WallRun.Chi");
		invalid = config.getStringList("Abilities.Passives.WallRun.InvalidBlocks");
	}

	private boolean isEligible() {
		if (!player.isSprinting())
			return false;

		if (!bPlayer.isToggled()) {
			return false;
		}

		if (bPlayer.getElements().contains(Element.AIR) && air) {
			return true;
		} else if (bPlayer.getElements().contains(Element.EARTH) && earth) {
			return true;
		} else if (bPlayer.getElements().contains(Element.WATER) && water) {
			return true;
		} else if (bPlayer.getElements().contains(Element.FIRE) && fire) {
			return true;
		} else return bPlayer.getElements().contains(Element.CHI) && chi;
	}

	private boolean isAgainstWall() {
		Location location = player.getLocation();
		if (location.getBlock().getRelative(BlockFace.NORTH).getType().isSolid() && !invalid.contains(location.getBlock().getRelative(BlockFace.NORTH).getType().name())) {
			return true;
		} else if (location.getBlock().getRelative(BlockFace.SOUTH).getType().isSolid() && !invalid.contains(location.getBlock().getRelative(BlockFace.SOUTH).getType().name())) {
			return true;
		} else if (location.getBlock().getRelative(BlockFace.WEST).getType().isSolid() && !invalid.contains(location.getBlock().getRelative(BlockFace.WEST).getType().name())) {
			return true;
		} else return location.getBlock().getRelative(BlockFace.EAST).getType().isSolid() && !invalid.contains(location.getBlock().getRelative(BlockFace.EAST).getType().name());
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline() || player.isOnGround()) {
			remove();
			return;
		}
		if (!isAgainstWall()) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + duration) {
			remove();
			return;
		}

		if (System.currentTimeMillis() - getStartTime() > 50L) {
			bPlayer.addCooldown("WallRun", getCooldown());
		}

		if (particles) {
			ParticleEffect.CRIT.display(player.getLocation(), 4, Math.random(), Math.random(), Math.random(), 0);
			ParticleEffect.BLOCK_CRACK.display(player.getLocation(), 3, Math.random(), Math.random(), Math.random(), 0.1, Material.STONE.createBlockData());
			AirAbility.playAirbendingParticles(player.getLocation(), 5);
		}

		Vector dir = player.getLocation().getDirection();
		dir.multiply(1.15);
		GeneralMethods.setVelocity(this, player, dir);
	}
	
	public long getCooldown() {
		return cooldown;
	}
	
	public Location getLocation() {
		return null;
	}
	
	@Override
	public String getName() {
		return "WallRun";
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
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	@Override
	public String getDescription() {
	   return "To use WallRun, sprint towards a wall, jump, then rapidly click to activate. You don't have to bind this ability to use it. It is a passive.";
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean hasParticles() {
		return particles;
	}

	public void setHasParticles(boolean particles) {
		this.particles = particles;
	}

	public boolean allowsAir() {
		return air;
	}

	public void setAllowsAir(boolean air) {
		this.air = air;
	}

	public boolean allowsEarth() {
		return earth;
	}

	public void setAllowsEarth(boolean earth) {
		this.earth = earth;
	}

	public boolean allowsWater() {
		return water;
	}

	public void setAllowsWater(boolean water) {
		this.water = water;
	}

	public boolean allowsFire() {
		return fire;
	}

	public void setAllowsFire(boolean fire) {
		this.fire = fire;
	}

	public boolean allowsChi() {
		return chi;
	}

	public void setAllowsChi(boolean chi) {
		this.chi = chi;
	}

	public List<String> getInvalid() {
		return invalid;
	}

	public void setInvalid(List<String> invalid) {
		this.invalid = invalid;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
	
	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Passives.WallRun.Enabled");
	}
}
