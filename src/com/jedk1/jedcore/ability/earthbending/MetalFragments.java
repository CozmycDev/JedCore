package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.ability.earthbending.animation.WobbleAnimation;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class MetalFragments extends MetalAbility implements AddonAbility {

	private static final double SOURCE_DROP_DISTANCE = 10;
	private static final double TARGET_RANGE = 30;

	@Attribute("MaxSources")
	private int maxSources;
	@Attribute(Attribute.SELECT_RANGE)
	private double selectRange;
	@Attribute("MaxShots")
	private int maxFragments;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private double velocity;
	@Attribute("LevitateHeight")
	private double levitateHeight;
	private double levitateSpeed;
	@Attribute("RevertTime")
	private long revertTime;
	private boolean useDisplaySources;

	private final List<Source> sources = new ArrayList<>();
	private final List<Item> thrownFragments = new ArrayList<>();

	public MetalFragments(Player player) {
		super(player);

		if (hasAbility(player, MetalFragments.class)) {
			MetalFragments.selectAnotherSource(player);
			return;
		}

		if (!bPlayer.canBend(this) || !bPlayer.canMetalbend()) {
			return;
		}

		setFields();

		if (addSource()) {
			start();
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		maxSources = config.getInt("Abilities.Earth.MetalFragments.MaxSources");
		selectRange = config.getDouble("Abilities.Earth.MetalFragments.SourceRange");
		maxFragments = config.getInt("Abilities.Earth.MetalFragments.MaxFragments");
		damage = config.getDouble("Abilities.Earth.MetalFragments.Damage");
		cooldown = config.getInt("Abilities.Earth.MetalFragments.Cooldown");
		velocity = config.getDouble("Abilities.Earth.MetalFragments.Velocity");
		levitateHeight = config.getDouble("Abilities.Earth.MetalFragments.LevitateHeight");
		levitateSpeed = config.getDouble("Abilities.Earth.MetalFragments.LevitateSpeed");
		revertTime = config.getLong("Abilities.Earth.MetalFragments.RevertTime");
		useDisplaySources = config.getBoolean("Abilities.Earth.MetalFragments.UseDisplaySources");
	}

	public static void shootFragment(Player player) {
		if (hasAbility(player, MetalFragments.class)) {
			getAbility(player, MetalFragments.class).shootFragment();
		}
	}

	private void shootFragment() {
		List<Source> readySources = new ArrayList<>();
		for (Source source : sources) {
			if (source.state == SourceState.SETTLED) {
				readySources.add(source);
			}
		}

		if (readySources.isEmpty()) {
			return;
		}

		Source source = readySources.get(new Random().nextInt(readySources.size()));
		Location sourceLoc = source.getLocation();
		Vector direction = getFireDirection(sourceLoc);
		Location spawnLoc = getFragmentSpawnLocation(sourceLoc, direction);

		spawnLoc.getWorld().spawnParticle(Particle.POOF, spawnLoc, 10, 0.25, 0.25, 0.25, 0.01);

		Item item = player.getWorld().dropItemNaturally(spawnLoc, getFragmentItem(source.blockData));
		item.setPickupDelay(Integer.MAX_VALUE);
		item.setVelocity(direction.clone().normalize().multiply(velocity));
		playMetalbendingSound(item.getLocation());
		thrownFragments.add(item);

		source.shotsFired++;

		if (useDisplaySources) {
			source.animator.setScale(getSourceScale(source));
		}

		if (source.shotsFired >= maxFragments) {
			dropSource(source);
			sources.remove(source);

			if (sources.isEmpty()) {
				remove();
			}
		}
	}

	private float getSourceScale(Source source) {
		if (maxFragments <= 0) {
			return 0f;
		}
		return Math.max(0f, (maxFragments - source.shotsFired) / (float) maxFragments);
	}

	private ItemStack getFragmentItem(BlockData data) {
		Material type = data.getMaterial();
		if (type == Material.ANCIENT_DEBRIS) {
			return new ItemStack(Material.NETHERITE_SCRAP, 1);
		}
		if (type == Material.NETHERITE_BLOCK) {
			return new ItemStack(Material.NETHERITE_INGOT, 1);
		}
		if (type == Material.GOLD_BLOCK || type.name().contains("GOLD_ORE")) {
			return new ItemStack(Material.GOLD_INGOT, 1);
		}
		if (type == Material.COPPER_BLOCK || type.name().contains("COPPER_ORE")) {
			return new ItemStack(Material.COPPER_INGOT, 1);
		}
		if (type == Material.COAL_BLOCK || type.name().contains("COAL_ORE")) {
			return new ItemStack(Material.COAL, 1);
		}
		return new ItemStack(Material.IRON_INGOT, 1);
	}

	private Vector getFireDirection(Location sourceLoc) {
		Entity target = GeneralMethods.getTargetedEntity(player, TARGET_RANGE, new ArrayList<>());
		if (target != null) {
			return GeneralMethods.getDirection(sourceLoc, target.getLocation());
		}
		return GeneralMethods.getDirection(sourceLoc, GeneralMethods.getTargetedLocation(player, TARGET_RANGE));
	}

	private Location getFragmentSpawnLocation(Location sourceLoc, Vector direction) {
		if (!sourceLoc.getBlock().getType().isSolid()) {
			return sourceLoc;
		}

		Location outside = sourceLoc.clone().add(direction.clone().normalize().multiply(0.75));
		if (!outside.getBlock().getType().isSolid()) {
			return outside;
		}

		return sourceLoc.clone().add(0, 1, 0);
	}

	public static void selectAnotherSource(Player player) {
		if (hasAbility(player, MetalFragments.class)) {
			getAbility(player, MetalFragments.class).addSource();
		}
	}

	private boolean addSource() {
		if (sources.size() >= maxSources) {
			return false;
		}

		Block block = getSourceBlock();
		if (block == null) {
			return false;
		}

		if (RegionProtection.isRegionProtected(player, block.getLocation(), this)) {
			return false;
		}

		BlockData data = block.getBlockData().clone();
		Location displayLoc = block.getLocation().clone();

		TempBlock originTemp = new TempBlock(block, Material.AIR.createBlockData());

		BlockDisplay display = spawnSourceDisplay(displayLoc, data);

		sources.add(new Source(block, data, new WobbleAnimation(this, display), displayLoc, originTemp));
		playMetalbendingSound(block.getLocation());
		return true;
	}

	private Block getSourceBlock() {
		RayTraceResult result = player.getWorld().rayTraceBlocks(
				player.getEyeLocation(),
				player.getEyeLocation().getDirection(),
				selectRange,
				FluidCollisionMode.NEVER,
				true);

		if (result == null || result.getHitBlock() == null) {
			return null;
		}

		Block block = result.getHitBlock();
		if (TempBlock.isTempBlock(block) || EarthAbility.getMovedEarth().containsKey(block)) {
			return null;
		}

		if (!isMetal(block) || isSourceBlock(block)) {
			return null;
		}

		return block;
	}

	public boolean isSourceBlock(Block block) {
		for (Source source : sources) {
			if (source.originBlock.equals(block)) {
				return true;
			}
			if (source.settledTemp != null && source.settledTemp.getBlock().equals(block)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void progress() {
		if (player == null || player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}

		Iterator<Source> iterator = sources.iterator();
		while (iterator.hasNext()) {
			Source source = iterator.next();

			if (source.state == SourceState.RISING) {
				updateLevitation(source);
			}

			source.animator.animate();

			if (player.getLocation().getWorld() != source.getLocation().getWorld()
					|| player.getLocation().distance(source.getLocation()) >= SOURCE_DROP_DISTANCE) {
				dropSource(source);
				iterator.remove();
			}
		}

		if (sources.isEmpty()) {
			remove();
			return;
		}

		for (ListIterator<Item> itr = thrownFragments.listIterator(); itr.hasNext();) {
			Item f = itr.next();

			f.getLocation().getWorld().spawnParticle(Particle.SMOKE, f.getLocation(), 1, 0.02, 0.02, 0.02, 0.01);

			boolean touchedLiving = false;
			for (Entity e : GeneralMethods.getEntitiesAroundPoint(f.getLocation(), 1)) {
				if (e instanceof LivingEntity && e.getEntityId() != player.getEntityId()) {
					touchedLiving = true;
					DamageHandler.damageEntity(e, damage, this);
				}
			}
			if (touchedLiving || f.isOnGround() || f.isDead()) {
				f.getLocation().getWorld().spawnParticle(
						Particle.ITEM,
						f.getLocation(),
						3,
						0.3, 0.3, 0.3,
						0.2,
						f.getItemStack()
				);
				f.remove();
				itr.remove();
			}
		}
	}

	private void updateLevitation(Source source) {
		Location next = source.location.clone().add(0, levitateSpeed, 0);

		Block topFaceBlock = next.clone().add(0, 1, 0).getBlock();
		if (topFaceBlock.getType().isSolid()) {
			settleSource(source);
			return;
		}

		double risen = next.getY() - source.originBlock.getY();
		if (risen >= levitateHeight) {
			next.setY(source.originBlock.getY() + levitateHeight);
			source.location = next;
			source.animator.updateLocation(next);
			settleSource(source);
			return;
		}

		source.location = next;
		source.animator.updateLocation(next);
	}

	private void settleSource(Source source) {
		source.state = SourceState.SETTLED;

		if (useDisplaySources) {
			return;
		}

		Block settleBlock = source.getLocation().getBlock();
		if (settleBlock.equals(source.originBlock)) {
			source.revertOriginTemp();
			source.animator.remove();
			return;
		}

		source.settledTemp = new TempBlock(settleBlock, source.blockData);
		source.animator.remove();
	}

	private void dropSource(Source source) {
		if (source.shotsFired > 0) {
			consumeSource(source);
			return;
		}

		if (source.settledTemp != null) {
			lowerTempSource(source);
		} else {
			lowerSourceWithDisplay(source.animator, source.location, source);
		}
	}

	private void consumeSource(Source source) {
		if (source.settledTemp != null) {
			source.settledTemp.revertBlock();
			source.settledTemp = null;
		}
		source.animator.remove();

		Location loc = source.getLocation();
		loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 10, 5);

		Block origin = source.originBlock;
		source.revertOriginTemp();

		if (revertTime > 0) {
			new TempBlock(origin, Material.AIR.createBlockData(), revertTime);
		}
	}

	private void lowerSourceWithDisplay(WobbleAnimation animator, Location start, Source source) {
		Location location = start;
		double targetY = source.originBlock.getY();
		Block origin = source.originBlock;
		TempBlock originTemp = source.detachOriginTemp();

		new BukkitRunnable() {
			@Override
			public void run() {
				if (animator.getBlockDisplay() == null || animator.getBlockDisplay().isDead()) {
					finish();
					return;
				}

				Location next = location.subtract(0, levitateSpeed, 0);
				if (next.getBlock().getType().isSolid() || next.getY() <= targetY) {
					if (next.getY() < targetY) {
						next.setY(targetY);
					}
					animator.updateLocation(next);
					finish();
					return;
				}

				animator.updateLocation(next);
				animator.animate();
			}

			private void finish() {
				if (originTemp != null) {
					originTemp.revertBlock();
					animator.remove();
					origin.getWorld().playSound(origin.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_METAL_PLACE, 1f, 0.8f);
				} else {
					animator.remove();
				}
				cancel();
			}
		}.runTaskTimer(JedCore.plugin, 1L, 1L);
	}

	private void lowerTempSource(Source source) {
		TempBlock settledTemp = source.settledTemp;
		source.settledTemp = null;
		Block block = settledTemp.getBlock();

		BlockDisplay display = spawnSourceDisplay(block.getLocation(), source.blockData);
		settledTemp.revertBlock();

		lowerSourceWithDisplay(new WobbleAnimation(this, display), block.getLocation(), source);
	}

	private BlockDisplay spawnSourceDisplay(Location location, BlockData blockData) {
		BlockDisplay display = location.getWorld().spawn(location, BlockDisplay.class);
		display.setBlock(blockData);
		display.setPersistent(false);
		display.setBillboard(Display.Billboard.FIXED);
		display.setInterpolationDuration(2);
		display.setTeleportDuration(2);
		return display;
	}

	public void removeFragments() {
		for (Item i : thrownFragments) {
			i.getLocation().getWorld().spawnParticle(
					Particle.ITEM,
					i.getLocation(),
					3,
					0.3, 0.3, 0.3,
					0.2,
					i.getItemStack()
			);
			i.remove();
		}
		thrownFragments.clear();
	}

	public static void remove(Player player, Block block) {
		if (hasAbility(player, MetalFragments.class)) {
			MetalFragments mf = getAbility(player, MetalFragments.class);
			if (mf.isSourceBlock(block)) {
				mf.remove();
			}
		}
	}

	@Override
	public void remove() {
		for (Source source : sources) {
			dropSource(source);
		}
		sources.clear();

		removeFragments();

		if (player.isOnline()) {
			bPlayer.addCooldown(this);
		}
		super.remove();
	}

	private enum SourceState {
		RISING, SETTLED
	}

	private static class Source {
		private final Block originBlock;
		private final BlockData blockData;
		private final WobbleAnimation animator;
		private Location location;
		private TempBlock originTemp;
		private TempBlock settledTemp;
		private SourceState state;
		private int shotsFired;

		Source(Block originBlock, BlockData blockData, WobbleAnimation animator, Location location, TempBlock originTemp) {
			this.originBlock = originBlock;
			this.blockData = blockData;
			this.animator = animator;
			this.location = location;
			this.originTemp = originTemp;
			this.state = SourceState.RISING;
		}

		private Location getLocation() {
			return location.clone().add(0.5, 0.5, 0.5);
		}

		private void revertOriginTemp() {
			if (originTemp != null) {
				originTemp.revertBlock();
				originTemp = null;
			}
		}

		private TempBlock detachOriginTemp() {
			TempBlock temp = originTemp;
			originTemp = null;
			return temp;
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
		return "MetalFragments";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.MetalFragments.Description");
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.MetalFragments.Enabled");
	}
}
