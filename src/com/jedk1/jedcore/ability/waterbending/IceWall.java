package com.jedk1.jedcore.ability.waterbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.SchedulerUtil;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.earthbending.EarthSmash;
import com.projectkorra.projectkorra.firebending.FireBlast;
import com.projectkorra.projectkorra.firebending.FireBlastCharged;
import com.projectkorra.projectkorra.firebending.lightning.Lightning;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.Torrent;
import com.projectkorra.projectkorra.waterbending.ice.IceBlast;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class IceWall extends IceAbility implements AddonAbility {
	public static List<IceWall> instances = new ArrayList<>();

	@Attribute(Attribute.HEIGHT)
	private int maxHeight;
	private int minHeight;

	@Attribute(Attribute.WIDTH)
	private int width;

	@Attribute(Attribute.RANGE)
	private int range;

	@Attribute("Health")
	private int maxHealth;
	private int minHealth;

	@Attribute(Attribute.DAMAGE)
	private double damage;

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;

	public static boolean stackable;

	public static boolean lifetimeEnabled;
	public static long lifetimeTime;

	public int torrentDamage;
	public int torrentFreezeDamage;
	public int iceBlastDamage;
	public int fireBlastDamage;
	public int fireBlastChargedDamage;
	public int lightningDamage;
	public int combustionDamage;
	public int earthSmashDamage;
	public int airBlastDamage;

	public boolean isWallDoneFor = false;
	public List<Block> affectedBlocks = new ArrayList<>();

	private boolean rising = false;
	private long lastDamageTime = 0;
	private long lifetime = 0;
	private int wallHealth;
	private int tankedDamage;

	private final List<Block> lastBlocks = new ArrayList<>();
	private final List<TempBlock> tempBlocks = new ArrayList<>();

	Random rand = new Random();

	public IceWall(Player player) {
		super(player);
		if (!bPlayer.canBendIgnoreCooldowns(this) || !bPlayer.canIcebend()) {
			return;
		}

		setFields();
		Block b = getSourceBlock(player, (int) (range * getNightFactor(player.getWorld())));
		if (b == null) return;

		for (IceWall iw : instances) {
			if (iw.affectedBlocks.contains(b)) {
				iw.collapse(player, false);
				return;
			}
		}

		if (!bPlayer.canBend(this) || !isWaterbendable(b)) return;

		wallHealth = (int) (((rand.nextInt((maxHealth - minHealth) + 1)) + minHealth) * getNightFactor(player.getWorld()));
		loadAffectedBlocks(player, b);
		lifetime = System.currentTimeMillis() + lifetimeTime;
		start();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		maxHeight = (config.getInt("Abilities.Water.IceWall.MaxHeight"));
		minHeight = (config.getInt("Abilities.Water.IceWall.MinHeight"));
		width = (config.getInt("Abilities.Water.IceWall.Width"));
		range = config.getInt("Abilities.Water.IceWall.Range");
		maxHealth = config.getInt("Abilities.Water.IceWall.MaxWallHealth");
		minHealth = config.getInt("Abilities.Water.IceWall.MinWallHealth");
		damage = config.getDouble("Abilities.Water.IceWall.Damage");
		cooldown = config.getLong("Abilities.Water.IceWall.Cooldown");
		stackable = config.getBoolean("Abilities.Water.IceWall.Stackable");
		lifetimeEnabled = config.getBoolean("Abilities.Water.IceWall.LifeTime.Enabled");
		lifetimeTime = config.getLong("Abilities.Water.IceWall.LifeTime.Duration");
		torrentDamage = config.getInt("Abilities.Water.IceWall.WallDamage.Torrent");
		torrentFreezeDamage = config.getInt("Abilities.Water.IceWall.WallDamage.TorrentFreeze");
		iceBlastDamage = config.getInt("Abilities.Water.IceWall.WallDamage.IceBlast");
		fireBlastDamage = config.getInt("Abilities.Water.IceWall.WallDamage.Fireblast");
		fireBlastChargedDamage = config.getInt("Abilities.Water.IceWall.WallDamage.FireblastCharged");
		lightningDamage = config.getInt("Abilities.Water.IceWall.WallDamage.Lightning");
		combustionDamage = config.getInt("Abilities.Water.IceWall.WallDamage.Combustion");
		earthSmashDamage = config.getInt("Abilities.Water.IceWall.WallDamage.EarthSmash");
		airBlastDamage = config.getInt("Abilities.Water.IceWall.WallDamage.AirBlast");
	}

	public Block getSourceBlock(Player player, int range) {
		Vector direction = player.getEyeLocation().getDirection().normalize();

		for (int i = 0; i <= range; i++) {
			Block b = player.getEyeLocation().add(direction.clone().multiply((double) i)).getBlock();

			if (isBendable(b)) return b;
		}

		return null;
	}

	public boolean isBendable(Block b) {
		return isWater(b) || isIce(b.getType()) || isSnow(b.getType());
	}

	public void loadAffectedBlocks(Player player, Block block) {
		Vector direction = player.getEyeLocation().getDirection().normalize();

		double ox, oy, oz;
		ox = -direction.getZ();
		oy = 0;
		oz = direction.getX();

		Vector orth = new Vector(ox, oy, oz);
		orth = orth.normalize();

		Location origin = block.getLocation();

		World world = origin.getWorld();

		int width = (int) (getWidth() * getNightFactor(world));
		int minHeight = (int) (getMinHeight() * getNightFactor(world));
		int maxHeight = (int) (getMaxHeight() * getNightFactor(world));

		int height = minHeight;
		boolean increasingHeight = true;
		for (int i = -(width / 2); i < width / 2; i++) {
			Block b = world.getBlockAt(origin.clone().add(orth.clone().multiply((double) i)));

			if (ElementalAbility.isAir(b.getType())) {
				while (ElementalAbility.isAir(b.getType())) {
					if (b.getY() < world.getMinHeight())
						return;

					b = b.getRelative(BlockFace.DOWN);
				}
			}

			if (!ElementalAbility.isAir(b.getRelative(BlockFace.UP).getType())) {
				while (!ElementalAbility.isAir(b.getRelative(BlockFace.UP).getType())) {
					if (b.getY() > b.getWorld().getMaxHeight())
						return;

					b = b.getRelative(BlockFace.UP);
				}
			}

			if (!stackable && isIceWallBlock(b)) {
				continue;
			}

			if (isBendable(b)) {
				affectedBlocks.add(b);
				for (int h = 1; h <= height; h++) {
					Block up = b.getRelative(BlockFace.UP, h);
					if (ElementalAbility.isAir(up.getType())) {
						affectedBlocks.add(up);
					}
				}

				if (height < maxHeight && increasingHeight)
					height++;

				if (i == 0)
					increasingHeight = false;

				if (!increasingHeight && height > minHeight)
					height--;

				lastBlocks.add(b);
			}

		}

		bPlayer.addCooldown(this);
		rising = true;
		instances.add(this);
	}

	@Override
	public void progress() {
		if (rising) {
			if (lastBlocks.isEmpty()) {
				rising = false;
			} else {
				List<Block> theseBlocks = new ArrayList<>(lastBlocks);
				lastBlocks.clear();

				for (Block b : theseBlocks) {
					TempBlock tb = new TempBlock(b, Material.ICE.createBlockData());
					tempBlocks.add(tb);

					playIcebendingSound(b.getLocation());

					Block up = b.getRelative(BlockFace.UP);

					if (affectedBlocks.contains(up))
						lastBlocks.add(up);
				}
			}
		}

		if (System.currentTimeMillis() > lifetime && lifetimeEnabled) {
			collapse(player, false);
		}
	}

	public void damageWall(Player player, int damage) {
		long noDamageTicks = 1000;
		if (System.currentTimeMillis() < lastDamageTime + noDamageTicks)
			return;

		lastDamageTime = System.currentTimeMillis();
		tankedDamage += damage;

		if (tankedDamage >= wallHealth) {
			collapse(player, true);
		}
	}

	public void collapse(Player player, boolean forceful) {
		if (rising) return;

		for (TempBlock tb : tempBlocks) {
			tb.revertBlock();
			tb.getLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, tb.getLocation(), 5, 0, 0, 0, 0, Material.PACKED_ICE.createBlockData());
			tb.getLocation().getWorld().playSound(tb.getLocation(), Sound.BLOCK_GLASS_BREAK, 5f, 5f);

			for (Entity e : GeneralMethods.getEntitiesAroundPoint(tb.getLocation(), 2.5)) {
				if (e.getEntityId() != player.getEntityId() && e instanceof LivingEntity) {
					DamageHandler.damageEntity(e, damage * getNightFactor(player.getWorld()), this);
					if (forceful) {
						((LivingEntity) e).setNoDamageTicks(0);
					}
				}
			}
		}

		tempBlocks.clear();
		isWallDoneFor = true;
	}

	@Override
	public void remove() {
		super.remove();
	}

	public static void collisionDamage(Entity entity, double travelledDistance, Vector difference, Player instigator) {
		for (IceWall iw : IceWall.instances) {
			for (Block b : iw.affectedBlocks) {
				if (entity.getLocation().getWorld() == b.getLocation().getWorld() && entity.getLocation().distance(b.getLocation()) < 2) {
					double damage = ((travelledDistance - 5.0) < 0 ? 0 : travelledDistance - 5.0) / (difference.length());
					iw.damageWall(instigator, (int) damage);
				}
			}
		}
	}

	public static boolean checkExplosions(Location location, Entity entity) {
		for (IceWall iw : IceWall.instances) {
			for (Block b : iw.affectedBlocks) {
				if (location.getWorld() == b.getLocation().getWorld() && location.distance(b.getLocation()) < 2) {

					for (Entity e : GeneralMethods.getEntitiesAroundPoint(location, 3)) {
						if (e instanceof LivingEntity) {
							LivingEntity livingEntity = (LivingEntity) e;
							SchedulerUtil.ensureEntity(livingEntity, () -> livingEntity.damage(7, entity));
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isIceWallBlock(Block block) {
		for (IceWall iw : IceWall.instances) {
			if (iw.affectedBlocks.contains(block)) {
				return true;
			}
		}
		return false;
	}

	public static void progressAll() {
        for (IceWall iw : new ArrayList<>(instances)) {
			if (iw.isWallDoneFor) continue; // Skip already collapsed walls
            for (Torrent t : getAbilities(Torrent.class)) {
                if (t.getLocation() == null) continue;
                for (int i = 0; i < t.getLaunchedBlocks().size(); i++) {
                    TempBlock tb = t.getLaunchedBlocks().get(i);

                    for (Block ice : iw.affectedBlocks) {
                        if (ice.getLocation().getWorld() == tb.getLocation().getWorld() && ice.getLocation().distance(tb.getLocation()) <= 2) {
                            if (t.isFreeze())
                                iw.damageWall(t.getPlayer(), (int) (iw.torrentFreezeDamage * getNightFactor(ice.getWorld())));
                            else
                                iw.damageWall(t.getPlayer(), (int) (iw.torrentDamage * getNightFactor(ice.getWorld())));

                            if (!iw.isWallDoneFor)
                                t.setFreeze(false);
                        }
                    }
                }
            }

            for (IceBlast ib : getAbilities(IceBlast.class)) {
                if (ib.getLocation() == null) continue;
                for (Block ice : iw.affectedBlocks) {
                    if (ib.source == null)
                        break;

                    if (ice.getLocation().getWorld() == ib.source.getLocation().getWorld() && ice.getLocation().distance(ib.source.getLocation()) <= 2) {
                        iw.damageWall(ib.getPlayer(), (int) (iw.iceBlastDamage * getNightFactor(ice.getWorld())));

                        if (!iw.isWallDoneFor)
                            ib.remove();
                    }
                }
            }

            for (FireBlastCharged fb : getAbilities(FireBlastCharged.class)) {
                if (fb.getLocation() == null) continue;
                for (Block ice : iw.affectedBlocks) {
                    if (ice.getLocation().getWorld() == fb.getLocation().getWorld() && fb.getLocation().distance(ice.getLocation()) <= 1.5) {
                        iw.damageWall(fb.getPlayer(), iw.fireBlastChargedDamage);

                        if (!iw.isWallDoneFor)
                            fb.remove();
                    }
                }
            }

            for (FireBlast fb : getAbilities(FireBlast.class)) {
                if (fb.getLocation() == null) continue;
                for (Block ice : iw.affectedBlocks) {
                    if (ice.getLocation().getWorld() == fb.getLocation().getWorld() && fb.getLocation().distance(ice.getLocation()) <= 1.5) {
                        iw.damageWall(fb.getPlayer(), iw.fireBlastDamage);

                        if (!iw.isWallDoneFor)
                            fb.remove();
                    }
                }
            }

            for (EarthSmash es : getAbilities(EarthSmash.class)) {
                if (es.getLocation() == null) continue;
                for (Block ice : iw.affectedBlocks) {
                    if (es.getState() == EarthSmash.State.SHOT) {
                        for (int j = 0; j < es.getBlocks().size(); j++) {
                            Block b = es.getBlocks().get(j);
                            if (ice.getLocation().getWorld() == b.getLocation().getWorld() && b.getLocation().distance(ice.getLocation()) <= 2) {
                                iw.damageWall(es.getPlayer(), iw.earthSmashDamage);

                                if (!iw.isWallDoneFor) {
                                    for (Block block : es.getBlocksIncludingInner()) {
                                        if (block != null && !ElementalAbility.isAir(block.getType())) {
                                            ParticleEffect.BLOCK_CRACK.display(block.getLocation(), 5, 0, 0, 0, 0, block.getBlockData().clone());
                                        }
                                    }
                                    es.remove();
                                }
                            }
                        }
                    }
                }
            }

            for (Lightning l : getAbilities(Lightning.class)) {
                for (Lightning.Arc arc : l.getArcs()) {
                    for (Block ice : iw.affectedBlocks) {
                        for (Location loc : arc.getPoints()) {
                            if (ice.getLocation().getWorld() == loc.getWorld() && loc.distance(ice.getLocation()) <= 1.5) {
                                iw.damageWall(l.getPlayer(), (int) (FireAbility.getDayFactor(iw.lightningDamage, ice.getWorld())));

                                if (!iw.isWallDoneFor)
                                    l.remove();
                            }
                        }
                    }
                }
            }

            for (CoreAbility ca : getAbilities(getAbility("Combustion").getClass())) {
                if (ca.getLocation() == null) continue;
                for (Block ice : iw.affectedBlocks) {
                    if (ice.getLocation().getWorld() == ca.getLocation().getWorld() && ca.getLocation().distance(ice.getLocation()) <= 1.5) {
                        iw.damageWall(ca.getPlayer(), iw.combustionDamage);
                        if (!iw.isWallDoneFor) ca.remove();
                    }
                }
            }
        }

		Iterator<IceWall> it = instances.iterator();
		while (it.hasNext()) {
			IceWall iw = it.next();
			if (iw.isWallDoneFor) {
				iw.affectedBlocks.clear();
				it.remove();
			}
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
	public String getName() {
		return "IceWall";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Water.IceWall.Description");
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
	}

	public int getMinHealth() {
		return minHealth;
	}

	public void setMinHealth(int minHealth) {
		this.minHealth = minHealth;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public boolean isRising() {
		return rising;
	}

	public void setRising(boolean rising) {
		this.rising = rising;
	}

	public long getLastDamageTime() {
		return lastDamageTime;
	}

	public void setLastDamageTime(long lastDamageTime) {
		this.lastDamageTime = lastDamageTime;
	}

	public long getLifetime() {
		return lifetime;
	}

	public void setLifetime(long lifetime) {
		this.lifetime = lifetime;
	}

	public int getWallHealth() {
		return wallHealth;
	}

	public void setWallHealth(int wallHealth) {
		this.wallHealth = wallHealth;
	}

	public int getTankedDamage() {
		return tankedDamage;
	}

	public void setTankedDamage(int tankedDamage) {
		this.tankedDamage = tankedDamage;
	}

	public List<Block> getLastBlocks() {
		return lastBlocks;
	}

	public List<TempBlock> getTempBlocks() {
		return tempBlocks;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
	}

	public int getMinHeight() {
		return minHeight;
	}

	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.IceWall.Enabled");
	}
}
