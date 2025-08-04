package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.util.TempFallingBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class MetalFragments extends MetalAbility implements AddonAbility {

	@Attribute("MaxSources")
	private int maxSources;
	@Attribute(Attribute.SELECT_RANGE)
	private int selectRange;
	@Attribute("MaxShots")
	private int maxFragments;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private double velocity;

	public List<Block> sources = new ArrayList<>();
	private final List<Item> thrownFragments = new ArrayList<>();
	private final List<TempBlock> tblockTracker = new ArrayList<>();
	//private List<FallingBlock> fblockTracker = new ArrayList<>();
	private final HashMap<Block, Integer> counters = new HashMap<>();

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

		if (tblockTracker.size() >= maxSources) {
			return;
		}

		if (prepare()) {
			Block b = selectSource();
			if (RegionProtection.isRegionProtected(player, b.getLocation(), this)) {
				return;
			}

			start();
			if (!isRemoved()) {
				translateUpward(b);
			}
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		maxSources = config.getInt("Abilities.Earth.MetalFragments.MaxSources");
		selectRange = config.getInt("Abilities.Earth.MetalFragments.SourceRange");
		maxFragments = config.getInt("Abilities.Earth.MetalFragments.MaxFragments");
		damage = config.getDouble("Abilities.Earth.MetalFragments.Damage");
		cooldown = config.getInt("Abilities.Earth.MetalFragments.Cooldown");
		velocity = config.getDouble("Abilities.Earth.MetalFragments.Velocity");
	}

	public static void shootFragment(Player player) {
		if (hasAbility(player, MetalFragments.class)) {
			getAbility(player, MetalFragments.class).shootFragment();
		}
	}

	private void shootFragment() {
		if (sources.size() <= 0)
			return;

		Random randy = new Random();
		int i = randy.nextInt(sources.size());
		Block source = sources.get(i);
		ItemStack is;

		switch (source.getType()) {
		case GOLD_BLOCK:
		case GOLD_ORE:
			is = new ItemStack(Material.GOLD_INGOT, 1);
			break;
		case COAL_BLOCK:
			is = new ItemStack(Material.COAL, 1);
			break;
		case COAL_ORE:
			is = new ItemStack(Material.COAL_ORE, 1);
			break;
		default:
			is = new ItemStack(Material.IRON_INGOT, 1);
			break;
		}

		Vector direction;
		if (GeneralMethods.getTargetedEntity(player, 30, new ArrayList<>()) != null) {
			direction = GeneralMethods.getDirection(source.getLocation(), GeneralMethods.getTargetedEntity(player, 30, new ArrayList<>()).getLocation());
		} else {
			direction = GeneralMethods.getDirection(source.getLocation(), GeneralMethods.getTargetedLocation(player, 30));
		}

		Item ii = player.getWorld().dropItemNaturally(source.getLocation().getBlock().getRelative(GeneralMethods.getCardinalDirection(direction)).getLocation(), is);
		ii.setPickupDelay(Integer.MAX_VALUE);
		ii.setVelocity(direction.normalize().multiply(velocity));
		playMetalbendingSound(ii.getLocation());
		thrownFragments.add(ii);

		if (counters.containsKey(source)) {
			int count = counters.get(source);
			count++;

			if (count >= maxFragments) {
				counters.remove(source);
				source.getWorld().spawnFallingBlock(source.getLocation().add(0.5, 0, 0.5), source.getBlockData());
				TempBlock tempBlock = TempBlock.get(source);
				if (tempBlock != null) {
					tempBlock.revertBlock();
				}
				sources.remove(source);
				source.getWorld().playSound(source.getLocation(), Sound.ENTITY_ITEM_BREAK, 10, 5);
			} else {
				counters.put(source, count);
			}

			if (sources.size() == 0) {
				remove();
			}
		}
	}

	public static void selectAnotherSource(Player player) {
		if (hasAbility(player, MetalFragments.class)) {
			getAbility(player, MetalFragments.class).selectAnotherSource();
		}
	}

	private void selectAnotherSource() {
		if (tblockTracker.size() >= maxSources)
			return;

		if (prepare()) {
			Block b = selectSource();
			translateUpward(b);
		}
	}

	public boolean prepare() {
		Block block = BlockSource.getEarthSourceBlock(player, selectRange, ClickType.SHIFT_DOWN);

		if (block == null)
			return false;

		return isMetal(block);
	}

	public Block selectSource() {
		Block block = BlockSource.getEarthSourceBlock(player, selectRange, ClickType.SHIFT_DOWN);
		if (isMetal(block))
			return block;
		return null;
	}

	public void translateUpward(Block block) {
		if (block == null)
			return;

		if (sources.contains(block))
			return;

		if (block.getRelative(BlockFace.UP).getType().isSolid())
			return;

		if (isEarthbendable(player, block)) {
			new TempFallingBlock(block.getLocation().add(0.5, 0, 0.5), block.getBlockData(), new Vector(0, 0.8, 0), this);
			block.setType(Material.AIR);

			playMetalbendingSound(block.getLocation());
		}
	}

	public void progress() {
		if (player == null || player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!hasAbility(player, MetalFragments.class)) {
			return;
		}
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}

		Iterator<TempBlock> itr = tblockTracker.iterator();
		while (itr.hasNext()) {
			TempBlock tb = itr.next();
			if (player.getLocation().distance(tb.getLocation()) >= 10) {
				player.getWorld().spawnFallingBlock(tb.getLocation().add(0.5,0.0,0.5), tb.getBlockData());
				sources.remove(tb.getBlock());
				tb.revertBlock();
				itr.remove();
			}
		}

		for (TempFallingBlock tfb : TempFallingBlock.getFromAbility(this)) {
			FallingBlock fb = tfb.getFallingBlock();
			if (fb.getLocation().getY() >= player.getEyeLocation().getY() + 1) {
				Block block = fb.getLocation().getBlock();
				TempBlock tb = new TempBlock(block, fb.getBlockData());

				tblockTracker.add(tb);
				sources.add(tb.getBlock());
				counters.put(tb.getBlock(), 0);
				tfb.remove();
			}

			if (fb.isOnGround()) {
				fb.getLocation().getBlock().setBlockData(fb.getBlockData());
			}
		}
		for (ListIterator<Item> iterator = thrownFragments.listIterator(); iterator.hasNext();) {
			Item f = iterator.next();

			boolean touchedLiving = false;
			for (Entity e : GeneralMethods.getEntitiesAroundPoint(f.getLocation(), 1)) {
				if (e instanceof LivingEntity && e.getEntityId() != player.getEntityId()) {
					touchedLiving = true;
					DamageHandler.damageEntity(e, damage, this);
				}
			}
			if (touchedLiving || f.isOnGround() || f.isDead()) {
				ParticleEffect.ITEM_CRACK.display(f.getLocation(), 3, 0.3, 0.3, 0.3, 0.2, f.getItemStack());
				f.remove();
				iterator.remove();
			}
		}

		//removeDeadFBlocks();
	}

	/*
	public void removeDeadFBlocks() {
		for (int i = 0; i < fblockTracker.size(); i++)
			if (fblockTracker.get(i).isDead())
				fblockTracker.remove(i);
	}
	*/

	public void removeDeadItems() {
		thrownFragments.removeIf(Item::isDead);
	}


	public void dropSources() {
		for (TempBlock tb : tblockTracker) {
			tb.getBlock().getWorld().spawnFallingBlock(tb.getLocation().add(0.5,0.0,0.5), tb.getBlock().getBlockData());
			tb.revertBlock();
		}

		tblockTracker.clear();
	}

	public void removeFragments() {
		for (Item i : thrownFragments) {
			ParticleEffect.ITEM_CRACK.display(i.getLocation(), 3, 0.3, 0.3, 0.3, 0.2, i.getItemStack());
			i.remove();
		}
		thrownFragments.clear();
	}

	public static void remove(Player player, Block block) {
		if (hasAbility(player, MetalFragments.class)) {
			MetalFragments mf = getAbility(player, MetalFragments.class);
			if (mf.sources.contains(block)) {
				mf.remove();
			}
		}
	}

	@Override
	public void remove() {
		dropSources();
		removeFragments();
		removeDeadItems();
		if (player.isOnline()) {
			bPlayer.addCooldown(this);
		}
		super.remove();
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

	public int getMaxSources() {
		return maxSources;
	}

	public void setMaxSources(int maxSources) {
		this.maxSources = maxSources;
	}

	public int getSelectRange() {
		return selectRange;
	}

	public void setSelectRange(int selectRange) {
		this.selectRange = selectRange;
	}

	public int getMaxFragments() {
		return maxFragments;
	}

	public void setMaxFragments(int maxFragments) {
		this.maxFragments = maxFragments;
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

	public List<Block> getSources() {
		return sources;
	}

	public void setSources(List<Block> sources) {
		this.sources = sources;
	}

	public List<Item> getThrownFragments() {
		return thrownFragments;
	}

	public List<TempBlock> getTblockTracker() {
		return tblockTracker;
	}

	public HashMap<Block, Integer> getCounters() {
		return counters;
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
