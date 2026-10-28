package com.jedk1.jedcore.ability.earthbending.animation;

import com.projectkorra.projectkorra.ability.Ability;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class WobbleAnimation implements Animation {

    private final Ability ability;
    private final BlockDisplay blockDisplay;

    private float time = 0f;

    private final float frequency;
    private final float maxTilt;
    private final float twistAmount;

    public WobbleAnimation(Ability ability, BlockDisplay blockDisplay) {
        this.ability = ability;
        this.blockDisplay = blockDisplay;

        Random random = ThreadLocalRandom.current();

        this.frequency = 0.05f + random.nextFloat() * 0.05f;
        this.maxTilt = 0.15f + random.nextFloat() * 0.1f;
        this.twistAmount = 0.05f * random.nextFloat();
    }

    @Override
    public void animate() {
        if (blockDisplay == null || blockDisplay.isDead()) {
            return;
        }

        if (ability == null || !ability.isEnabled()) {
            remove();
            return;
        }

        time += 1f;

        float tiltX = (float) Math.sin(time * frequency) * maxTilt;
        float tiltZ = (float) Math.cos(time * frequency * 0.9f) * maxTilt;
        float twistY = (float) Math.sin(time * frequency * 0.5f) * twistAmount;

        Quaternionf rotation = new Quaternionf()
                .rotateY(twistY)   // torsional component
                .rotateX(tiltX)    // wobble
                .rotateZ(tiltZ);

        Vector3f scale = blockDisplay.getTransformation().getScale();
        Vector3f pivot = new Vector3f(scale).mul(0.5f);

        Vector3f rotatedPivot = new Vector3f(pivot).rotate(rotation);
        Vector3f translation = new Vector3f(pivot).sub(rotatedPivot);

        Transformation transform = new Transformation(
                translation,
                rotation,
                scale,
                new Quaternionf()
        );

        blockDisplay.setInterpolationDuration(2);
        blockDisplay.setTransformation(transform);
    }

    @Override
    public void updateLocation(Location newLocation) {
        if (blockDisplay != null && !blockDisplay.isDead()) {
            blockDisplay.teleport(newLocation);
        }
    }

    @Override
    public void remove() {
        if (blockDisplay != null && !blockDisplay.isDead()) {
            blockDisplay.remove();
        }
    }

    public Ability getAbility() {
        return ability;
    }

    public BlockDisplay getBlockDisplay() {
        return blockDisplay;
    }
}
