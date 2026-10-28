package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.ability.earthbending.animation.Animation;
import com.jedk1.jedcore.ability.earthbending.animation.TumbleAnimation;
import com.jedk1.jedcore.ability.earthbending.animation.WobbleAnimation;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EarthBlastAbility extends EarthAbility implements AddonAbility {

    private static final String CONFIG_PATH = "Abilities.Earth.EarthBlast.";
    private static final double BLOCK_CENTER_OFFSET = 0.5;
    private static final float STONE_BREAK_VOLUME = 1.5f;
    private static final float STONE_BREAK_PITCH = 0.8f;

    private Location location;
    private Location origin;
    private Vector direction;
    private Block sourceBlock;
    private BlockDisplay blockDisplay;
    private BlockDisplay highlightDisplay;
    private Animation animator;
    private BlockData sourceBlockData;
    private Location sourceBlockLocation;
    protected AbilityState state;
    private int launchTicks;
    private boolean hasBlockAbove;
    private boolean blockConsumed;
    private Block indicatorBlock;
    private Vector3f selfScale;
    private Vector targetDirection;

    private Config config;

    public enum AbilityState {
        READY, SELECTED, LAUNCHING, FRAGMENTING
    }

    private static class Config {
        final double selectRange;
        final double launchRange;
        final double speed;
        final double directDamage;
        final double fragmentDamage;
        final double knockback;
        final boolean useModernHighlight;
        final String highlightColor;
        final double collisionRadius;
        final long cooldown;
        final long revertTime;
        final boolean allowFragments;
        final int fragmentCount;
        final double fragmentSpeed;
        final int maxHurls;
        final int duration;
        final boolean collisionParticles;
        final boolean fragmentParticles;
        final double redirectEasingFactor;
        final boolean useRedirectEasing;
        final int raiseTicks;
        final String animationStyle;

        Config(Player player) {
            ConfigurationSection cfg = JedCoreConfig.getConfig(player);
            this.selectRange = cfg.getDouble(CONFIG_PATH + "SelectRange", 6.0);
            this.launchRange = cfg.getDouble(CONFIG_PATH + "LaunchRange", 32.0);
            this.raiseTicks = cfg.getInt(CONFIG_PATH + "RaiseTicks", 5);
            this.speed = cfg.getDouble(CONFIG_PATH + "Speed", 1.45);
            this.directDamage = cfg.getDouble(CONFIG_PATH + "DirectDamage", 3.0);
            this.fragmentDamage = cfg.getDouble(CONFIG_PATH + "FragmentDamage", 2.0);
            this.knockback = cfg.getDouble(CONFIG_PATH + "Knockback", 1.3);
            this.useModernHighlight = cfg.getBoolean(CONFIG_PATH + "UseModernHighlight", false);
            this.highlightColor = cfg.getString(CONFIG_PATH + "HighlightColor", "#FFFFFF");
            this.collisionRadius = cfg.getDouble(CONFIG_PATH + "CollisionRadius", 1.0);
            this.cooldown = cfg.getLong(CONFIG_PATH + "Cooldown", 500);
            this.revertTime = cfg.getLong(CONFIG_PATH + "RevertTime", 15000);
            this.allowFragments = cfg.getBoolean(CONFIG_PATH + "AllowFragments", true);
            this.fragmentCount = cfg.getInt(CONFIG_PATH + "FragmentCount", 6);
            this.fragmentSpeed = cfg.getDouble(CONFIG_PATH + "FragmentSpeed", 0.3);
            this.maxHurls = cfg.getInt(CONFIG_PATH + "MaxHurls", 6);
            this.duration = cfg.getInt(CONFIG_PATH + "TickDuration", 300);
            this.collisionParticles = cfg.getBoolean(CONFIG_PATH + "CollisionParticles", false);
            this.fragmentParticles = cfg.getBoolean(CONFIG_PATH + "FragmentParticles", true);
            this.redirectEasingFactor = cfg.getDouble(CONFIG_PATH + "RedirectEasingFactor", 0.15);
            this.useRedirectEasing = cfg.getBoolean(CONFIG_PATH + "UseRedirectEasing", true);
            this.animationStyle = cfg.getString(CONFIG_PATH + "AnimationStyle", "wobble").toLowerCase();
        }
    }

    public EarthBlastAbility(Player player) {
        super(player);
        if (!isEnabled() || !bPlayer.canBend(this)) {
            return;
        }
        this.config = new Config(player);
        this.state = AbilityState.READY;
        this.launchTicks = 0;
        this.blockConsumed = false;
        start();
    }

    @Override
    public void progress() {
        if (!isEnabled() || !bPlayer.canBendIgnoreBindsCooldowns(this)) {
            remove();
            return;
        }

        if (animator != null) {
            animator.animate();
        }

        switch (state) {
            case READY -> {
                if (player.isSneaking()) {
                    selectBlock();
                }
            }
            case SELECTED -> handleSelected();
            case LAUNCHING -> handleLaunching();
            case FRAGMENTING -> remove();
        }
    }

    public boolean isReadyOrSelected() {
        return state == AbilityState.READY || state == AbilityState.SELECTED;
    }

    public void handleLeftClick(boolean isSneaking) {
        switch (state) {
            case SELECTED -> {
                if (isSneaking) {
                    cancelSelection();
                } else {
                    launch();
                }
            }
            case LAUNCHING -> {
                if (isSneaking) {
                    shatterAllProjectiles();
                } else {
                    redirectActiveProjectiles();
                }
            }
        }
    }

    private void cancelSelection() {
        restoreSourceBlock();
        removeHighlight();
        state = AbilityState.READY;
        sourceBlock = null;
    }

    private void shatterAllProjectiles() {
        getAbilities(player, EarthBlastAbility.class).stream()
                .filter(hurl -> hurl.state == AbilityState.LAUNCHING && hurl.location != null)
                .forEach(hurl -> {
                    playSound(hurl.location, Sound.BLOCK_STONE_BREAK, STONE_BREAK_VOLUME, STONE_BREAK_PITCH);
                    hurl.spawnFragments(hurl.location, hurl.direction);
                    hurl.state = AbilityState.FRAGMENTING;
                });
    }

    private void redirectActiveProjectiles() {
        Location targetLocation = getTargetLocation();
        if (targetLocation == null || isWithinSelectRange(targetLocation)) {
            return;
        }

        getAbilities(player, EarthBlastAbility.class).stream()
                .filter(hurl -> hurl.state == AbilityState.LAUNCHING && hurl.location != null)
                .forEach(hurl -> {
                    Vector newDirection = calculateDirection(hurl.location, targetLocation);
                    double distanceFromOrigin = hurl.location.distance(hurl.origin);

                    if (distanceFromOrigin < config.launchRange * 0.9) {
                        hurl.targetDirection = newDirection;
                    } else {
                        Vector toOrigin = calculateDirection(hurl.location, hurl.origin);
                        if (newDirection.dot(toOrigin) <= 0) {
                            hurl.targetDirection = newDirection;
                        }
                    }
                });
    }

    private boolean isWithinSelectRange(Location target) {
        return player.getEyeLocation().distance(target) <= config.selectRange;
    }

    private Vector calculateDirection(Location from, Location to) {
        return to.toVector().subtract(from.toVector()).normalize();
    }

    private Location getTargetLocation() {
        Entity targetEntity = GeneralMethods.getTargetedEntity(player, config.launchRange, new ArrayList<>());
        if (targetEntity instanceof LivingEntity) {
            return ((LivingEntity) targetEntity).getEyeLocation();
        }

        RayTraceResult blockResult = rayTraceBlocks(player.getEyeLocation(), config.launchRange);
        if (blockResult != null && blockResult.getHitBlock() != null) {
            return blockResult.getHitBlock().getLocation().add(BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET);
        }

        return player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(config.launchRange * 2));
    }

    private void handleSelected() {
        if (isTooFarFromSource() || !isHurlBound()) {
            remove();
            return;
        }

        if (player.isSneaking()) {
            updateSourceBlockData();
            selectBlock();
        }

        updateHighlightIfNeeded();
    }

    private boolean isTooFarFromSource() {
        return sourceBlock != null && player.getLocation().distance(sourceBlock.getLocation()) > config.selectRange + 2;
    }

    private boolean isHurlBound() {
        return bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthBlast");
    }

    private void updateSourceBlockData() {
        if (sourceBlock != null && sourceBlock.getType() != Material.AIR && !TempBlock.isTempBlock(sourceBlock)) {
            sourceBlockData = sourceBlock.getBlockData().clone();
        }
    }

    private void updateHighlightIfNeeded() {
        if (config.useModernHighlight && (highlightDisplay == null || highlightDisplay.isDead())) {
            updateHighlight();
        }
    }

    private void handleLaunching() {
        if (location == null) {
            remove();
            return;
        }

        launchTicks++;

        if (launchTicks >= config.duration) {
            fragmentAndEnd();
            return;
        }

        if (launchTicks <= config.raiseTicks) {
            performInitialRise();
        } else {
            performFlightAndCollisionDetection();
        }

        updateBlockDisplayPosition();
    }

    private void fragmentAndEnd() {
        // Pass the current travel direction so fragments carry forward momentum
        spawnFragments(location, direction);
        state = AbilityState.FRAGMENTING;
    }

    private void performInitialRise() {
        if (hasBlockAbove) {
            Vector horizontalToPlayer = player.getEyeLocation().toVector()
                    .subtract(location.toVector())
                    .normalize();
            horizontalToPlayer.setY(0);
            location.add(horizontalToPlayer.multiply(config.speed * 0.1));
        } else {
            location.add(0, config.speed * 0.25, 0);
        }
    }

    private void performFlightAndCollisionDetection() {
        if (config.useRedirectEasing && targetDirection != null) {
            direction = direction.clone()
                    .multiply(1 - config.redirectEasingFactor)
                    .add(targetDirection.clone().multiply(config.redirectEasingFactor))
                    .normalize();
        }

        direction = direction.normalize();
        Location previousLocation = location.clone();
        Vector movement = direction.clone().multiply(config.speed);
        Location nextLocation = location.clone().add(movement);

        if (hasExceededRange(nextLocation)) {
            fragmentAndEnd();
            return;
        }

        if (checkBlockCollision(previousLocation, movement, nextLocation)) {
            return;
        }

        location = nextLocation;

        if (checkEntityCollision()) {
            return;
        }
    }

    private boolean hasExceededRange(Location nextLocation) {
        return nextLocation.distance(origin) > config.launchRange;
    }

    private boolean checkBlockCollision(Location previousLocation, Vector movement, Location nextLocation) {
        RayTraceResult blockHit = location.getWorld().rayTrace(
                previousLocation,
                direction,
                movement.length() + 0.2,
                FluidCollisionMode.NEVER,
                true,
                0.9,
                entity -> false
        );

        if (blockHit != null && blockHit.getHitBlock() != null) {
            Block hitBlock = blockHit.getHitBlock();
            if (isSolidBlockCollision(hitBlock, location.getBlock(), nextLocation.getBlock())) {
                Location hitLocation = blockHit.getHitPosition().toLocation(location.getWorld());
                // Block/wall impact: random scatter (no momentum direction)
                spawnFragments(hitLocation);
                state = AbilityState.FRAGMENTING;
                return true;
            }
        }

        // todo: this is a bandaid for a rare phasing bug, find the root cause later
        if (location.getBlock().getType().isSolid()
                && location.getBlock().getType() != Material.WATER) {
            fragmentAndEnd();
        }
        return false;
    }

    private boolean isSolidBlockCollision(Block hitBlock, Block currentBlock, Block nextBlock) {
        return hitBlock.getType().isSolid()
                && hitBlock.getType() != Material.WATER
                && !isWaterlogged(hitBlock)
                && hitBlock.getType() != Material.LAVA
                && !hitBlock.equals(currentBlock)
                && !hitBlock.getLocation().equals(sourceBlockLocation)
                && (hitBlock.equals(nextBlock) || currentBlock.getLocation().distance(hitBlock.getLocation()) > 0.3);
    }

    private boolean checkEntityCollision() {
        for (Entity entity : location.getWorld().getNearbyEntities(location, config.collisionRadius, config.collisionRadius, config.collisionRadius)) {
            if (shouldIgnoreEntity(entity)) {
                continue;
            }

            if (entity instanceof ItemDisplay) {
                handleDisplayCollision();
                return true;
            } else if (entity instanceof BlockDisplay otherDisplay) {
                Vector3f otherScale = otherDisplay.getTransformation().getScale();

                if (isStrictlySmaller(otherScale, selfScale)) {
                    continue;
                }

                handleDisplayCollision();
                return true;
            } else if (entity instanceof LivingEntity target) {
                handleLivingEntityCollision(target);
                return true;
            }

        }
        return false;
    }

    private static boolean isStrictlySmaller(Vector3f other, Vector3f self) {
        return other.x < self.x && other.y < self.y && other.z < self.z;
    }

    private boolean shouldIgnoreEntity(Entity entity) {
        return entity.equals(player) || !entity.isValid() || entity instanceof ArmorStand || entity.equals(blockDisplay);
    }

    private void handleDisplayCollision() {
        playSound(location, Sound.BLOCK_STONE_BREAK, STONE_BREAK_VOLUME, STONE_BREAK_PITCH);
        cleanupProjectile();
        // Display entity impact: carry direction forward
        spawnFragments(location, direction);
        state = AbilityState.FRAGMENTING;
    }

    private void handleLivingEntityCollision(LivingEntity target) {
        DamageHandler.damageEntity(target, applyMetalPowerFactor(config.directDamage, sourceBlock), this);
        target.setVelocity(direction.clone().multiply(config.knockback));
        playSound(location, Sound.BLOCK_STONE_BREAK, STONE_BREAK_VOLUME, STONE_BREAK_PITCH);
        cleanupProjectile();
        // Entity impact: fragments carry direction forward past the target
        spawnFragments(location, direction);
        state = AbilityState.FRAGMENTING;
    }

    private void updateBlockDisplayPosition() {
        animator.updateLocation(location);
    }

    public void selectBlock() {
        if (state != AbilityState.READY && state != AbilityState.SELECTED) {
            return;
        }

        RayTraceResult rayTrace = rayTraceBlocks(player.getEyeLocation(), config.selectRange);
        if (rayTrace == null || rayTrace.getHitBlock() == null) {
            return;
        }

        Block target = rayTrace.getHitBlock();
        if (!canSelectBlock(target)) {
            return;
        }

        if (sourceBlock != null) {
            restoreSourceBlock();
            removeHighlight();
        }

        initializeSelection(target);
        playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5f, 1.2f);
    }

    private boolean canSelectBlock(Block target) {
        return !target.equals(sourceBlock)
                && isEarthbendable(target)
                && target.getType() != Material.AIR
                && !TempBlock.isTempBlock(target)
                && !isBlockSelectedByOtherHurl(target);
    }

    private boolean isBlockSelectedByOtherHurl(Block target) {
        return getAbilities(player, EarthBlastAbility.class).stream()
                .anyMatch(hurl -> hurl != this
                        && hurl.state == AbilityState.SELECTED
                        && hurl.sourceBlock != null
                        && hurl.sourceBlock.equals(target));
    }

    private void initializeSelection(Block target) {
        sourceBlock = target;
        sourceBlockData = sourceBlock.getBlockData().clone();
        hasBlockAbove = isBlockAbove(sourceBlock);
        state = AbilityState.SELECTED;

        if (config.useModernHighlight) {
            updateHighlight();
        } else {
            focusBlock();
        }
    }

    private boolean isBlockAbove(Block block) {
        Block above = block.getRelative(0, 1, 0);
        return above.getType().isSolid() && !above.getType().isAir();
    }

    private void breakPlantAbove(Block block) {
        Block above = block.getRelative(0, 1, 0);
        Material type = above.getType();

        if (type.isAir() || !above.isEmpty()) {
            if (Tag.FLOWERS.isTagged(type) || Tag.SAPLINGS.isTagged(type) ||
                    type.name().contains("GRASS") || type.name().contains("FERN") ||
                    type == Material.DEAD_BUSH) {
                above.breakNaturally();
            }
        }
    }

    private void focusBlock() {
        if (sourceBlock == null) return;

        indicatorBlock = sourceBlock;
        if (sourceBlockData == null) {
            sourceBlockData = sourceBlock.getBlockData().clone();
        }

        BlockData indicatorData = getIndicatorBlockData(sourceBlockData.getMaterial()).createBlockData();
        player.sendBlockChange(indicatorBlock.getLocation(), indicatorData);
    }

    private Material getIndicatorBlockData(Material sourceMaterial) {
        return switch (sourceMaterial) {
            case SAND -> Material.SANDSTONE;
            case RED_SAND -> Material.RED_SANDSTONE;
            case STONE -> Material.COBBLESTONE;
            default -> Material.STONE;
        };
    }

    private void restoreSourceBlock() {
        if (indicatorBlock != null && sourceBlockData != null) {
            player.sendBlockChange(indicatorBlock.getLocation(), sourceBlockData);
            indicatorBlock = null;
        }
    }

    public void launch() {
        if (state != AbilityState.SELECTED || sourceBlock == null || !bPlayer.canBend(this)) {
            return;
        }

        sourceBlockLocation = sourceBlock.getLocation().clone();
        if (sourceBlockData == null) {
            sourceBlockData = sourceBlock.getBlockData().clone();
        }

        removeHighlight();
        initializeLaunchParameters();
        createBlockDisplay();
        playEarthbendingSound(origin);

        state = AbilityState.LAUNCHING;
        launchTicks = 0;
        blockConsumed = true;

        bPlayer.addCooldown(this);
        breakPlantAbove(sourceBlock);
        new TempBlock(sourceBlock, Material.AIR.createBlockData(), config.revertTime);
        new EarthBlastAbility(player);
    }

    private void initializeLaunchParameters() {
        origin = sourceBlockLocation.clone();
        location = origin.clone().add(0, BLOCK_CENTER_OFFSET, 0);
        direction = player.getEyeLocation().getDirection().normalize();
        targetDirection = direction.clone();
    }

    private void createBlockDisplay() {
        blockDisplay = location.getWorld().spawn(location, BlockDisplay.class);
        blockDisplay.setBlock(sourceBlockData);
        blockDisplay.setPersistent(false);
        blockDisplay.setBillboard(Display.Billboard.FIXED);
        blockDisplay.setInterpolationDuration(2);
        blockDisplay.setTeleportDuration(2);
        this.selfScale = blockDisplay.getTransformation().getScale();

        animator = switch (config.animationStyle) {
            case "tumble" -> new TumbleAnimation(this, blockDisplay);
            default -> new WobbleAnimation(this, blockDisplay);
        };
    }

    /**
     * Spawns fragments with fully random scatter — used for block/wall impacts
     * where the projectile stops dead and energy dissipates in all directions.
     */
    private void spawnFragments(Location impactLoc) {
        spawnFragments(impactLoc, null);
    }

    /**
     * Spawns fragments optionally biased toward {@code impactDirection}.
     * When a direction is supplied (max-range expiry, entity hits, manual shatter),
     * fragments fan out in a forward cone to simulate conservation of momentum.
     * When {@code impactDirection} is null, fragments scatter randomly (wall impacts).
     */
    private void spawnFragments(Location impactLoc, Vector impactDirection) {
        playSound(impactLoc, Sound.BLOCK_STONE_BREAK, 2.0f, 0.6f);
        if (config.collisionParticles) {
            impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc.clone().add(0, BLOCK_CENTER_OFFSET, 0), 1);
        }

        cleanupProjectile();

        if (!config.allowFragments) return;

        List<FragmentData> fragments = createFragments(impactLoc, impactDirection);
        animateFragments(fragments, impactLoc);
    }

    private List<FragmentData> createFragments(Location impactLoc, Vector impactDirection) {
        List<FragmentData> fragments = new ArrayList<>();

        for (int i = 0; i < config.fragmentCount; i++) {
            BlockDisplay fragment = createFragmentDisplay(impactLoc);
            Vector velocity = (impactDirection != null)
                    ? createDirectionalVelocity(impactDirection)
                    : createRandomVelocity();
            TumbleAnimation fragAnimator = new TumbleAnimation(this, fragment);

            fragments.add(new FragmentData(fragment, velocity, fragAnimator));
        }

        return fragments;
    }

    private BlockDisplay createFragmentDisplay(Location impactLoc) {
        BlockDisplay fragment = impactLoc.getWorld().spawn(
                impactLoc.clone().add(0, 0.2, 0),
                BlockDisplay.class
        );

        fragment.setBlock(sourceBlockData);
        fragment.setPersistent(false);
        fragment.setBillboard(Display.Billboard.FIXED);
        fragment.setInterpolationDuration(2);
        fragment.setTeleportDuration(2);

        org.bukkit.util.Transformation transform = fragment.getTransformation();
        fragment.setTransformation(new org.bukkit.util.Transformation(
                transform.getTranslation(),
                transform.getLeftRotation(),
                new org.joml.Vector3f(0.25f, 0.25f, 0.25f),
                transform.getRightRotation()
        ));

        return fragment;
    }

    /**
     * Creates a velocity fanned out in a cone around {@code baseDirection}.
     *
     * The cone half-angle is ~40 degrees, giving a believable spread that still
     * reads as clearly directional — like shards breaking off the front face of
     * a rock that has been pushed to its limit.  A small upward bias is added so
     * fragments arc slightly rather than travelling dead-straight.
     */
    private Vector createDirectionalVelocity(Vector baseDirection) {
        Vector forward = baseDirection.clone().normalize();

        // Build two axes perpendicular to the travel direction
        Vector up = new Vector(0, 1, 0);
        Vector perp1 = forward.clone().crossProduct(up);
        if (perp1.lengthSquared() < 1e-4) {
            // Forward is nearly vertical — use a different reference axis
            perp1 = forward.clone().crossProduct(new Vector(1, 0, 0));
        }
        perp1.normalize();
        Vector perp2 = forward.clone().crossProduct(perp1).normalize();

        // Cone spread: ~40-degree half-angle  (tan(40°) ≈ 0.84)
        double spreadRadius = 0.84;
        double randomAngle = Math.random() * Math.PI * 2;
        // Square-root distribution so fragments fill the cone area evenly
        double randomRadius = Math.sqrt(Math.random()) * spreadRadius;

        double speed = config.fragmentSpeed + Math.random() * 0.25;

        Vector result = forward.clone().multiply(speed)
                .add(perp1.clone().multiply(Math.cos(randomAngle) * randomRadius * speed))
                .add(perp2.clone().multiply(Math.sin(randomAngle) * randomRadius * speed));

        // Small upward arc — fragments should tumble forward and slightly up,
        // not fly straight like bullets
        result.setY(result.getY() + 0.15 + Math.random() * 0.2);

        return result;
    }

    /** Original fully-random scatter, used for block/wall impacts. */
    private Vector createRandomVelocity() {
        double angle = Math.random() * Math.PI * 2;
        double speedXZ = config.fragmentSpeed + Math.random() * 0.2;
        double speedY = 0.2 + Math.random() * 0.3;

        return new Vector(
                Math.cos(angle) * speedXZ,
                speedY,
                Math.sin(angle) * speedXZ
        );
    }

    private void animateFragments(List<FragmentData> fragments, Location impactLoc) {
        new BukkitRunnable() {
            // todo: damage source needs finishing, wait until we add source stealing
            final Player damageSource = player;
            final double fragmentDmg = applyMetalPowerFactor(config.fragmentDamage, sourceBlock);

            private static final double GRAVITY = 0.05;
            private static final int MAX_TICKS = 80;
            private int tick = 0;

            @Override
            public void run() {
                tick++;

                fragments.removeIf(frag -> updateFragment(frag, impactLoc));

                if (tick >= MAX_TICKS || fragments.isEmpty()) {
                    cleanup(fragments);
                    cancel();
                }
            }

            private boolean updateFragment(FragmentData frag, Location impactLoc) {
                if (frag.display.isDead()) {
                    return true;
                }

                frag.velocity.setY(frag.velocity.getY() - GRAVITY);
                Location nextLoc = frag.display.getLocation().add(frag.velocity);

                if (shouldRemoveFragment(nextLoc, impactLoc)) {
                    frag.cleanup();
                    return true;
                }

                if (checkFragmentEntityCollision(nextLoc, frag)) {
                    return true;
                }


                spawnFragmentParticles(nextLoc);

                if (frag.animator != null) {
                    frag.animator.animate();
                    frag.animator.updateLocation(nextLoc);
                }

                return false;
            }

            private boolean shouldRemoveFragment(Location nextLoc, Location impactLoc) {
                Block fragBlock = nextLoc.getBlock();
                return (fragBlock.getType().isSolid()
                        && fragBlock.getType() != Material.WATER
                        && fragBlock.getType() != Material.LAVA)
                        || nextLoc.getY() < impactLoc.getY() - 8;
            }

            private boolean checkFragmentEntityCollision(Location nextLoc, FragmentData frag) {
                if (tick <= 4) return false;

                for (Entity e : nextLoc.getWorld().getNearbyEntities(nextLoc, 0.8, 0.6, 0.6)) {
                    if (e instanceof LivingEntity && !e.equals(damageSource) && e.isValid()) {
                        LivingEntity target = (LivingEntity) e;
                        //DamageHandler.damageEntity(target, null, fragmentDmg, null, false, true);
                        target.damage(fragmentDmg);
                        e.setVelocity(frag.velocity.clone().multiply(0.5));
                        frag.cleanup();
                        return true;
                    }
                }
                return false;
            }

            private void spawnFragmentParticles(Location loc) {
                if (config.fragmentParticles && tick % 3 == 0) {
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc, 1, 0.02, 0.02, 0.02, 0.01);
                }
            }

            private void cleanup(List<FragmentData> fragments) {
                fragments.forEach(FragmentData::cleanup);
            }
        }.runTaskTimer(JedCore.plugin, 0L, 1L);
    }

    private static class FragmentData {
        final BlockDisplay display;
        final Vector velocity;
        final Animation animator;

        FragmentData(BlockDisplay display, Vector velocity, TumbleAnimation animator) {
            this.display = display;
            this.velocity = velocity;
            this.animator = animator;
        }

        void cleanup() {
            animator.remove();
        }
    }

    private void updateHighlight() {
        removeHighlight();
        if (sourceBlock == null) return;

        Location blockLoc = sourceBlock.getLocation().clone();
        blockLoc.setY(blockLoc.getBlockY() + 0.01);

        highlightDisplay = blockLoc.getWorld().spawn(blockLoc, BlockDisplay.class);
        configureHighlightDisplay(blockLoc);
    }

    private void configureHighlightDisplay(Location blockLoc) {
        highlightDisplay.setBlock(sourceBlock.getBlockData());
        highlightDisplay.setPersistent(false);
        highlightDisplay.setBillboard(Display.Billboard.FIXED);
        highlightDisplay.setGlowing(true);
        highlightDisplay.setBrightness(calculateBrightness(blockLoc));
        highlightDisplay.setGlowColorOverride(hexToColor(config.highlightColor));
        highlightDisplay.setInterpolationDuration(2);
        highlightDisplay.setTeleportDuration(2);
        highlightDisplay.setVisibleByDefault(false);

        player.showEntity(JedCore.plugin, highlightDisplay);

        org.bukkit.util.Transformation transform = highlightDisplay.getTransformation();
        highlightDisplay.setTransformation(new org.bukkit.util.Transformation(
                new org.joml.Vector3f(-0.005f, -0.005f, -0.005f),
                transform.getLeftRotation(),
                new org.joml.Vector3f(1.01f, 1.01f, 1.01f),
                transform.getRightRotation()
        ));
    }

    private Display.Brightness calculateBrightness(Location blockLoc) {
        Location above = blockLoc.clone().add(0, 1, 0);
        Block aboveBlock = above.getBlock();
        return new Display.Brightness(
                Math.max(aboveBlock.getLightLevel(), aboveBlock.getLightFromSky()),
                aboveBlock.getLightFromSky()
        );
    }

    private void removeHighlight() {
        if (highlightDisplay != null && !highlightDisplay.isDead()) {
            highlightDisplay.remove();
            highlightDisplay = null;
        }
    }

    private void cleanupProjectile() {
        if (animator != null) {
            animator.remove();
        }
    }

    private void playSound(Location loc, Sound sound, float volume, float pitch) {
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }

    private RayTraceResult rayTraceBlocks(Location eyeLoc, double range) {
        return player.getWorld().rayTraceBlocks(
                eyeLoc,
                eyeLoc.getDirection(),
                range,
                FluidCollisionMode.NEVER,
                true
        );
    }

    private boolean isWaterlogged(Block block) {
        return block.getBlockData() instanceof Waterlogged waterLogged && waterLogged.isWaterlogged();
    }

    public static Color hexToColor(String hex) {
        if (hex == null) return Color.WHITE;

        hex = hex.replace("#", "");
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return Color.fromRGB(r, g, b);
    }

    public int getMaxHurls() {
        return this.config.maxHurls;
    }

    @Override
    public void remove() {
        super.remove();
        removeHighlight();
        if (!blockConsumed) {
            restoreSourceBlock();
        }
        cleanupProjectile();
    }

    public void forceFragment() {
        if (state == AbilityState.LAUNCHING && location != null) {
            spawnFragments(location, direction);
            state = AbilityState.FRAGMENTING;
        }
    }

    @Override
    public void handleCollision(Collision collision) {
        if (collision.isRemovingFirst()) {
            forceFragment();
        }
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isEnabled() {
        return JedCoreConfig.getConfig(player).getBoolean(CONFIG_PATH + "Enabled");
    }

    @Override
    public String getDescription() {
        return "* JedCore Addon *\n" + JedCoreConfig.getConfig(player).getString(CONFIG_PATH + "Description");
    }

    @Override
    public String getAuthor() {
        return "Cozmyc";
    }

    @Override
    public String getVersion() {
        return "1.1.0";
    }

    @Override
    public String getName() {
        return "EarthBlast";
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isIgniteAbility() {
        return false;
    }

    @Override
    public boolean isExplosiveAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return config != null ? config.cooldown : JedCoreConfig.getConfig(player).getLong(CONFIG_PATH + "Cooldown");
    }

    @Override
    public Location getLocation() {
        return (state == AbilityState.LAUNCHING && location != null)
                ? location
                : player.getLocation();
    }

    @Override
    public boolean isCollidable() {
        return state == AbilityState.LAUNCHING;
    }

    @Override
    public double getCollisionRadius() {
        return config != null ? config.collisionRadius : JedCoreConfig.getConfig(player).getDouble(CONFIG_PATH + "CollisionRadius");
    }

    @Override
    public List<Location> getLocations() {
        return (state == AbilityState.LAUNCHING && location != null)
                ? Collections.singletonList(location)
                : new ArrayList<>();
    }
}