package com.jedk1.jedcore.ability.earthbending.animation;

import com.projectkorra.projectkorra.ability.Ability;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class TumbleAnimation implements Animation {

    private final Ability ability;
    private final BlockDisplay blockDisplay;

    private final Vector3f rotationAxis;
    private final float angularVelocity;
    private float currentAngle = 0f;

    public TumbleAnimation(Ability ability, BlockDisplay blockDisplay) {
        this.ability = ability;
        this.blockDisplay = blockDisplay;

        Random random = ThreadLocalRandom.current();

        this.rotationAxis = new Vector3f(
                random.nextFloat() * 2f - 1f,
                random.nextFloat() * 2f - 1f,
                random.nextFloat() * 2f - 1f
        ).normalize();

        this.angularVelocity = 0.11f + random.nextFloat() * 0.15f;
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

        currentAngle += angularVelocity;

        Quaternionf rotation = new Quaternionf()
                .rotateAxis(currentAngle, rotationAxis.x, rotationAxis.y, rotationAxis.z);

        Vector3f scale = blockDisplay.getTransformation().getScale();
        Vector3f pivot = new Vector3f(scale.x / 2f, scale.y / 2f, scale.z / 2f);

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
