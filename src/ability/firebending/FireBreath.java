package com.jedk1.jedcore.ability.firebending;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.listener.CommandListener;
import com.jedk1.jedcore.util.FireTick;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ChatUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.jedk1.jedcore.JedCore;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BlueFireAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.firebending.BlazeArc;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.ice.PhaseChange;

public class FireBreath extends FireAbility implements AddonAbility {

	public static List<UUID> rainbowPlayer = new ArrayList<>();

	private int ticks;
	Random rand = new Random();
	private final List<Location> locations = new ArrayList<>();

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	private int particles;
	@Attribute(Attribute.DAMAGE)
	private double playerDamage;
	@Attribute(Attribute.DAMAGE)
	private double mobDamage;
	@Attribute(Attribute.DURATION)
	private int fireDuration;
	@Attribute(Attribute.RANGE)
	private int range;
	private boolean spawnFire;
	private boolean meltEnabled;
	private int meltChance;


	public FireBreath(Player player) {
		super(player);
		if (!bPlayer.canBend(this) || hasAbility(player, FireBreath.class)) {
			return;
		}

		setFields();
		
		if (bPlayer.isAvatarState()) {
			range = range * 2;
			playerDamage = playerDamage * 1.5;
			mobDamage = mobDamage * 2;
			duration = duration * 3;
		} else if (JCMethods.isSozinsComet(player.getWorld())) {
			range = range * 2;
			playerDamage = playerDamage * 1.5;
			mobDamage = mobDamage * 2;
		}
		start();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Fire.FireBreath.Cooldown");
		duration = config.getLong("Abilities.Fire.FireBreath.Duration");
		particles = config.getInt("Abilities.Fire.FireBreath.Particles");
		playerDamage = config.getDouble("Abilities.Fire.FireBreath.Damage.Player");
		mobDamage = config.getDouble("Abilities.Fire.FireBreath.Damage.Mob");
		fireDuration = config.getInt("Abilities.Fire.FireBreath.FireDuration");
		range = config.getInt("Abilities.Fire.FireBreath.Range");
		spawnFire = config.getBoolean("Abilities.Fire.FireBreath.Avatar.FireEnabled");
		meltEnabled = config.getBoolean("Abilities.Fire.FireBreath.Melt.Enabled");
		meltChance = config.getInt("Abilities.Fire.FireBreath.Melt.Chance");
		
		applyModifiers();
	}
	
	private void applyModifiers() {
		if (bPlayer.canUseSubElement(SubElement.BLUE_FIRE)) {
			cooldown *= BlueFireAbility.getCooldownFactor();
			range *= BlueFireAbility.getRangeFactor();
			playerDamage *= BlueFireAbility.getDamageFactor();
			mobDamage *= BlueFireAbility.getDamageFactor();
		}
		
		if (isDay(player.getWorld())) {
			cooldown -= ((long) getDayFactor(cooldown) - cooldown);
			range = (int) getDayFactor(range);
			playerDamage = getDayFactor(playerDamage);
			mobDamage = getDayFactor(mobDamage);
		}
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}
		if (!player.isSneaking()) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}
		if (System.currentTimeMillis() < getStartTime() + duration) {
			createBeam();
		} else {
			bPlayer.addCooldown(this);
			remove();
		}
	}

	private boolean isLocationSafe(Location loc) {
		Block block = loc.getBlock();
		if (RegionProtection.isRegionProtected(player, loc, this)) {
			return false;
		}
		if (!isTransparent(block)) {
			return false;
		}
		return !isWater(block);
	}

	private void createBeam() {
		Location loc = player.getEyeLocation();
		Vector dir = player.getLocation().getDirection();
		double step = 1;
		double size = 0;
		double offset = 0;
		double damageRegion = 1.5;

		locations.clear();

		for (double k = 0; k < range; k += step) {
			loc = loc.add(dir.clone().multiply(step));
			size += 0.005;
			offset += 0.3;
			damageRegion += 0.01;
			if (meltEnabled) {
				for (Block b : GeneralMethods.getBlocksAroundPoint(loc, damageRegion)) {
					if (isIce(b) && rand.nextInt(meltChance) == 0) {
						if (TempBlock.isTempBlock(b)) {
							TempBlock temp = TempBlock.get(b);
							if (PhaseChange.getFrozenBlocksMap().containsKey(temp)) {
								temp.revertBlock();
								PhaseChange.getFrozenBlocksMap().remove(temp);
							}
						}
					}
				}
			}
			if (!isLocationSafe(loc))
				return;

			locations.add(loc);

			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, damageRegion)) {
				if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId()) {
					if (entity instanceof Player) {
						FireTick.set(entity, fireDuration / 50);
						DamageHandler.damageEntity(entity, playerDamage, this);
					} else {
						FireTick.set(entity, fireDuration / 50);
						DamageHandler.damageEntity(entity, mobDamage, this);
					}
				}
			}

			playFirebendingSound(loc);
			if (bPlayer.isAvatarState() && spawnFire) {
				new BlazeArc(player, loc, dir, 2);
			}

			if (rainbowPlayer.contains(player.getUniqueId())) {
				ticks++;
				if (ticks >= 301)
					ticks = 0;
				if (isInRange(ticks, 0, 50)) {
					for (int i = 0; i < 6; i++)
						displayParticle(getOffsetLocation(loc, offset), 1, 140, 32, 32);
				} else if (isInRange(ticks, 51, 100)) {
					for (int i = 0; i < 6; i++)
						displayParticle(getOffsetLocation(loc, offset), 1, 196, 93, 0);
				} else if (isInRange(ticks, 101, 150)) {
					for (int i = 0; i < 6; i++)
						displayParticle(getOffsetLocation(loc, offset), 1, 186, 166, 37);
				} else if (isInRange(ticks, 151, 200)) {
					for (int i = 0; i < 6; i++)
						displayParticle(getOffsetLocation(loc, offset), 1, 36, 171, 47);
				} else if (isInRange(ticks, 201, 250)) {
					for (int i = 0; i < 6; i++)
						displayParticle(getOffsetLocation(loc, offset), 1, 36, 142, 171);
				} else if (isInRange(ticks, 251, 300)) {
					for (int i = 0; i < 6; i++)
						displayParticle(getOffsetLocation(loc, offset), 1, 128, 36, 171);
				}
			} else {
				playFirebendingParticles(loc, particles, Math.random(), Math.random(), Math.random());
				ParticleEffect.SMOKE_NORMAL.display(loc, particles, Math.random(), Math.random(), Math.random(), size);
				JCMethods.emitLight(loc);
			}
		}
	}

	private void displayParticle(Location location, int amount, int r, int g, int b) {
		ParticleEffect.REDSTONE.display(location, amount, 0, 0, 0, 0.005, new Particle.DustOptions(Color.fromRGB(r, g, b), 1));
		JCMethods.emitLight(location);
	}

	private boolean isInRange(int x, int min, int max) {
		return min <= x && x <= max;
	}

	/**
	 * Generates an offset location around a given location with variable offset
	 * amount.
	 */
	private Location getOffsetLocation(Location loc, double offset) {
		return loc.clone().add((float) ((Math.random() - 0.5) * offset), (float) ((Math.random() - 0.5) * offset), (float) ((Math.random() - 0.5) * offset));
	}

	public static void toggleRainbowBreath(Player player, boolean activate) {
		ConfigurationSection config = JedCoreConfig.getConfig(player);

		boolean easterEgg = config.getBoolean("Abilities.Fire.FireBreath.RainbowBreath.Enabled");
		String bindMsg = ChatUtil.color(config.getString("Abilities.Fire.FireBreath.RainbowBreath.EnabledMessage", ""));
		String unbindMsg = ChatUtil.color(config.getString("Abilities.Fire.FireBreath.RainbowBreath.DisabledMessage", ""));
		String deniedMsg = ChatUtil.color(config.getString("Abilities.Fire.FireBreath.RainbowBreath.NoAccess", ""));

		if (easterEgg && (player.hasPermission("bending.ability.FireBreath.RainbowBreath") 
				|| Arrays.asList(CommandListener.DEVELOPERS).contains(player.getUniqueId().toString()))) {
			if (activate) {
				if (!rainbowPlayer.contains(player.getUniqueId())) {
					rainbowPlayer.add(player.getUniqueId());
					if (!bindMsg.equals("")) player.sendMessage(Element.FIRE.getColor() + bindMsg);
				}
			} else {
				if (rainbowPlayer.contains(player.getUniqueId())) {
					rainbowPlayer.remove(player.getUniqueId());
					if (!unbindMsg.equals("")) player.sendMessage(Element.FIRE.getColor() + unbindMsg);
				}
			}
		} else if (!deniedMsg.equals("")) {
			player.sendMessage(Element.FIRE.getColor() + deniedMsg);
		}
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public List<Location> getLocations() {
		return locations;
	}

	@Override
	public String getName() {
		return "FireBreath";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Fire.FireBreath.Description");
	}

	public static List<UUID> getRainbowPlayer() {
		return rainbowPlayer;
	}

	public static void setRainbowPlayer(List<UUID> rainbowPlayer) {
		FireBreath.rainbowPlayer = rainbowPlayer;
	}

	public int getTicks() {
		return ticks;
	}

	public void setTicks(int ticks) {
		this.ticks = ticks;
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

	public int getParticles() {
		return particles;
	}

	public void setParticles(int particles) {
		this.particles = particles;
	}

	public double getPlayerDamage() {
		return playerDamage;
	}

	public void setPlayerDamage(double playerDamage) {
		this.playerDamage = playerDamage;
	}

	public double getMobDamage() {
		return mobDamage;
	}

	public void setMobDamage(double mobDamage) {
		this.mobDamage = mobDamage;
	}

	public int getFireDuration() {
		return fireDuration;
	}

	public void setFireDuration(int fireDuration) {
		this.fireDuration = fireDuration;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public boolean isSpawnFire() {
		return spawnFire;
	}

	public void setSpawnFire(boolean spawnFire) {
		this.spawnFire = spawnFire;
	}

	public boolean isMeltEnabled() {
		return meltEnabled;
	}

	public void setMeltEnabled(boolean meltEnabled) {
		this.meltEnabled = meltEnabled;
	}

	public int getMeltChance() {
		return meltChance;
	}

	public void setMeltChance(int meltChance) {
		this.meltChance = meltChance;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Fire.FireBreath.Enabled");
	}
}
