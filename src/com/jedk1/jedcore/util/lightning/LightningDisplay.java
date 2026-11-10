package com.jedk1.jedcore.util.lightning;

import com.projectkorra.projectkorra.util.LightManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class LightningDisplay {

	private final LightningStyle style;
	private final List<BlockDisplay> displays = new ArrayList<>();

	public LightningDisplay(LightningStyle style) {
		this.style = style;
	}

	public void render(Collection<LightningArc> arcs) {
		List<LightningArc.Segment> segments = new ArrayList<>();
		for (LightningArc arc : arcs) {
			segments.addAll(arc.getSegments());
		}

		int index = 0;
		for (LightningArc.Segment segment : segments) {
			BlockDisplay display = getOrCreateDisplay(index, segment);
			renderSegment(display, segment);
			index++;
		}

		while (index < displays.size()) {
			clearDisplay(displays.get(index));
			index++;
		}
	}

	public void render(LightningArc arc) {
		List<LightningArc> arcs = new ArrayList<>(1);
		arcs.add(arc);
		render(arcs);
	}

	public void clear() {
		for (BlockDisplay display : displays) {
			clearDisplay(display);
		}
	}

	public void remove() {
		for (BlockDisplay display : displays) {
			if (display != null && !display.isDead()) {
				display.remove();
			}
		}
		displays.clear();
	}

	private BlockDisplay getOrCreateDisplay(int index, LightningArc.Segment segment) {
		if (index < displays.size()) {
			BlockDisplay existing = displays.get(index);
			if (existing != null && !existing.isDead()) {
				return existing;
			}

			BlockDisplay replacement = createDisplay(segment);
			displays.set(index, replacement);
			return replacement;
		}

		BlockDisplay display = createDisplay(segment);
		displays.add(display);
		return display;
	}

	private BlockDisplay createDisplay(LightningArc.Segment segment) {
		Location spawnLocation = segment.getDisplayLocation();

		return spawnLocation.getWorld().spawn(spawnLocation, BlockDisplay.class, display -> {
			display.setPersistent(false);
			display.setBillboard(Display.Billboard.FIXED);
			display.setBrightness(new Display.Brightness(style.getBrightness(), style.getBrightness()));
			display.setViewRange(style.getViewRange());
			display.setInterpolationDuration(0);
			display.setTeleportDuration(0);
			display.setGlowing(false);
			display.setInvulnerable(true);
			display.setSilent(true);
			display.setBlock(style.getBlockData());
			display.addScoreboardTag(style.getDisplayTag());
			applyTransformation(display, segment, spawnLocation.toVector());
		});
	}

	private void renderSegment(BlockDisplay display, LightningArc.Segment segment) {
		if (!style.getBlockData().equals(display.getBlock())) {
			display.setBlock(style.getBlockData());
		}

		applyTransformation(display, segment, display.getLocation().toVector());

		if (style.isEmitLight()) {
			LightManager.createLight(segment.getDisplayLocation())
					.brightness(style.getLightBrightness())
					.timeUntilFadeout(style.getLightFadeTicks())
					.emit();
		}
	}

	private void applyTransformation(BlockDisplay display, LightningArc.Segment segment, Vector anchor) {
		Vector delta = segment.getTo().toVector().subtract(segment.getFrom().toVector());
		double length = delta.length();

		if (length < 0.001) {
			display.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf(),
					new Vector3f(),
					new Quaternionf()
			));
			return;
		}

		Vector segmentDirection = delta.clone().normalize();
		Vector midpointOffset = segment.getFrom().toVector()
				.add(delta.clone().multiply(0.5))
				.subtract(anchor);

		Quaternionf rotation = new Quaternionf().rotationTo(
				new Vector3f(0.0f, 0.0f, 1.0f),
				new Vector3f((float) segmentDirection.getX(), (float) segmentDirection.getY(), (float) segmentDirection.getZ())
		);

		double jointOverlap = Math.max(0.03, style.getThickness());
		Vector3f scale = new Vector3f(
				(float) style.getThickness(),
				(float) style.getThickness(),
				(float) Math.max(0.01, length + style.getLengthPadding() + jointOverlap)
		);

		Vector3f translation = new Vector3f(
				(float) midpointOffset.getX(),
				(float) midpointOffset.getY(),
				(float) midpointOffset.getZ()
		).sub(new Vector3f(scale).mul(0.5f).rotate(rotation));

		display.setTransformation(new Transformation(
				translation,
				rotation,
				scale,
				new Quaternionf()
		));
	}

	private void clearDisplay(BlockDisplay display) {
		if (display != null && !display.isDead() && display.getBlock().getMaterial() != Material.AIR) {
			display.setBlock(Material.AIR.createBlockData());
		}
	}
}
