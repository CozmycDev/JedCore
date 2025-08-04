//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.jedk1.jedcore.util;

import com.projectkorra.projectkorra.ability.CoreAbility;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

public class TempFallingBlock {
    public static ConcurrentHashMap<FallingBlock, TempFallingBlock> instances = new ConcurrentHashMap();
    public static ConcurrentHashMap<CoreAbility, Set<TempFallingBlock>> instancesByAbility = new ConcurrentHashMap();
    private FallingBlock fallingblock;
    private CoreAbility ability;
    private long creation;
    private boolean expire;
    private Consumer<TempFallingBlock> onPlace;

    public TempFallingBlock(Location location, BlockData data, Vector velocity, CoreAbility ability) {
        this(location, data, velocity, ability, false);
    }

    public TempFallingBlock(Location location, BlockData data, Vector velocity, CoreAbility ability, boolean expire) {
        this.fallingblock = location.getWorld().spawnFallingBlock(location, data.clone());
        this.fallingblock.setVelocity(velocity);
        this.fallingblock.setDropItem(false);
        this.ability = ability;
        this.creation = System.currentTimeMillis();
        this.expire = expire;
        instances.put(this.fallingblock, this);
        if (!instancesByAbility.containsKey(ability)) {
            instancesByAbility.put(ability, new HashSet());
        }

        ((Set)instancesByAbility.get(ability)).add(this);
    }

    public static void manage() {
        long time = System.currentTimeMillis();

        for(TempFallingBlock tfb : instances.values()) {
            if (tfb.canExpire() && time > tfb.getCreationTime() + 5000L) {
                tfb.remove();
            } else if (time > tfb.getCreationTime() + 120000L) {
                tfb.remove();
            }
        }

    }

    public static TempFallingBlock get(FallingBlock fallingblock) {
        return isTempFallingBlock(fallingblock) ? (TempFallingBlock)instances.get(fallingblock) : null;
    }

    public static boolean isTempFallingBlock(FallingBlock fallingblock) {
        return instances.containsKey(fallingblock);
    }

    public static void removeFallingBlock(FallingBlock fallingblock) {
        if (isTempFallingBlock(fallingblock)) {
            TempFallingBlock tempFallingBlock = (TempFallingBlock)instances.get(fallingblock);
            Objects.requireNonNull(fallingblock);
            ThreadUtil.ensureEntity(fallingblock, fallingblock::remove);
            instances.remove(fallingblock);
            ((Set)instancesByAbility.get(tempFallingBlock.ability)).remove(tempFallingBlock);
            if (((Set)instancesByAbility.get(tempFallingBlock.ability)).isEmpty()) {
                instancesByAbility.remove(tempFallingBlock.ability);
            }
        }

    }

    public static void removeAllFallingBlocks() {
        for(FallingBlock fallingblock : instances.keySet()) {
            Objects.requireNonNull(fallingblock);
            ThreadUtil.ensureEntity(fallingblock, fallingblock::remove);
        }

        instances.clear();
        instancesByAbility.clear();
    }

    public static Set<TempFallingBlock> getFromAbility(CoreAbility ability) {
        return (Set)instancesByAbility.getOrDefault(ability, new HashSet());
    }

    public void remove() {
        FallingBlock var10000 = this.fallingblock;
        FallingBlock var10001 = this.fallingblock;
        Objects.requireNonNull(var10001);
        ThreadUtil.ensureEntity(var10000, var10001::remove);
        instances.remove(this.fallingblock);
    }

    public FallingBlock getFallingBlock() {
        return this.fallingblock;
    }

    public CoreAbility getAbility() {
        return this.ability;
    }

    public Material getMaterial() {
        return this.fallingblock.getBlockData().getMaterial();
    }

    public BlockData getMaterialData() {
        return this.fallingblock.getBlockData();
    }

    public BlockData getData() {
        return this.fallingblock.getBlockData();
    }

    public Location getLocation() {
        return this.fallingblock.getLocation();
    }

    public long getCreationTime() {
        return this.creation;
    }

    public boolean canExpire() {
        return this.expire;
    }

    public void tryPlace() {
        if (this.onPlace != null) {
            this.onPlace.accept(this);
        }

    }

    public Consumer<TempFallingBlock> getOnPlace() {
        return this.onPlace;
    }

    public void setOnPlace(Consumer<TempFallingBlock> onPlace) {
        this.onPlace = onPlace;
    }
}
