package com.jedk1.jedcore.util.lightning;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public final class LightningStyle {

	// EXPERIMENTAL: still figuring out what looks decent!

	private final BlockData blockData;
	private final double thickness;
	private final double lengthPadding;
	private final int brightness;
	private final float viewRange;
	private final boolean emitLight;
	private final int lightBrightness;
	private final int lightFadeTicks;
	private final int maxSegments;
	private final double segmentLength;
	private final double jitter;
	private final int mainArcs;
	private final double branchChance;
	private final int branchIntervalMin;
	private final int branchIntervalMax;
	private final double branchLength;
	private final double subBranchChance;
	private final double subBranchLength;
	private final double branchJitter;
	private final boolean clipToBlocks;
	private final double taper;
	private final double branchThickness;
	private final double slant;
	private final double roll;
	private final double flicker;
	private final String displayTag;

	private LightningStyle(Builder builder) {
		this.blockData = builder.blockData;
		this.thickness = builder.thickness;
		this.lengthPadding = builder.lengthPadding;
		this.brightness = builder.brightness;
		this.viewRange = builder.viewRange;
		this.emitLight = builder.emitLight;
		this.lightBrightness = builder.lightBrightness;
		this.lightFadeTicks = builder.lightFadeTicks;
		this.maxSegments = builder.maxSegments;
		this.segmentLength = builder.segmentLength;
		this.jitter = builder.jitter;
		this.mainArcs = builder.mainArcs;
		this.branchChance = builder.branchChance;
		this.branchIntervalMin = builder.branchIntervalMin;
		this.branchIntervalMax = Math.max(builder.branchIntervalMin, builder.branchIntervalMax);
		this.branchLength = builder.branchLength;
		this.subBranchChance = builder.subBranchChance;
		this.subBranchLength = builder.subBranchLength;
		this.branchJitter = builder.branchJitter;
		this.clipToBlocks = builder.clipToBlocks;
		this.taper = builder.taper;
		this.branchThickness = builder.branchThickness;
		this.slant = builder.slant;
		this.roll = builder.roll;
		this.flicker = builder.flicker;
		this.displayTag = builder.displayTag;
	}

	public static Builder builder() {
		return new Builder();
	}

	public BlockData getBlockData() {
		return blockData;
	}

	public double getThickness() {
		return thickness;
	}

	public double getLengthPadding() {
		return lengthPadding;
	}

	public int getBrightness() {
		return brightness;
	}

	public float getViewRange() {
		return viewRange;
	}

	public boolean isEmitLight() {
		return emitLight;
	}

	public int getLightBrightness() {
		return lightBrightness;
	}

	public int getLightFadeTicks() {
		return lightFadeTicks;
	}

	public int getMaxSegments() {
		return maxSegments;
	}

	public double getSegmentLength() {
		return segmentLength;
	}

	public double getJitter() {
		return jitter;
	}

	public int getMainArcs() {
		return mainArcs;
	}

	public double getBranchChance() {
		return branchChance;
	}

	public int getBranchIntervalMin() {
		return branchIntervalMin;
	}

	public int getBranchIntervalMax() {
		return branchIntervalMax;
	}

	public double getBranchLength() {
		return branchLength;
	}

	public double getSubBranchChance() {
		return subBranchChance;
	}

	public double getSubBranchLength() {
		return subBranchLength;
	}

	public double getBranchJitter() {
		return branchJitter;
	}

	public double getTaper() {
		return taper;
	}

	public double getBranchThickness() {
		return branchThickness;
	}

	public double getSlant() {
		return slant;
	}

	public double getRoll() {
		return roll;
	}

	public double getFlicker() {
		return flicker;
	}

	public boolean isClipToBlocks() {
		return clipToBlocks;
	}

	public String getDisplayTag() {
		return displayTag;
	}

	public static final class Builder {

		private BlockData blockData = Material.LIGHT_BLUE_CONCRETE_POWDER.createBlockData();
		private double thickness = 0.05;
		private double lengthPadding = 0.02;
		private int brightness = 15;
		private float viewRange = 1.0f;
		private boolean emitLight = true;
		private int lightBrightness = 8;
		private int lightFadeTicks = 50;
		private int maxSegments = 24;
		private double segmentLength = 0.6;
		private double jitter = 0.2;
		private int mainArcs = 2;
		private double branchChance = 0.6;
		private int branchIntervalMin = 4;
		private int branchIntervalMax = 6;
		private double branchLength = 0.4;
		private double subBranchChance = 0.5;
		private double subBranchLength = 0.25;
		private double branchJitter = 0.8;
		private boolean clipToBlocks = false;
		private double taper = 0.55;
		private double branchThickness = 0.62;
		private double slant = 0.12;
		private double roll = 0.4;
		private double flicker = 0.14;
		private String displayTag = "jedcore_lightning";

		private Builder() {
		}

		public Builder material(Material material) {
			this.blockData = material != null && material.isBlock()
					? material.createBlockData()
					: Material.LIGHT_BLUE_CONCRETE_POWDER.createBlockData();
			return this;
		}

		public Builder blockData(BlockData blockData) {
			this.blockData = blockData != null ? blockData : Material.LIGHT_BLUE_CONCRETE_POWDER.createBlockData();
			return this;
		}

		public Builder thickness(double thickness) {
			this.thickness = Math.max(0.01, thickness);
			return this;
		}

		public Builder lengthPadding(double lengthPadding) {
			this.lengthPadding = Math.max(0.0, lengthPadding);
			return this;
		}

		public Builder brightness(int brightness) {
			this.brightness = Math.max(0, Math.min(15, brightness));
			return this;
		}

		public Builder viewRange(float viewRange) {
			this.viewRange = (float) Math.max(0.1, viewRange);
			return this;
		}

		public Builder emitLight(boolean emitLight) {
			this.emitLight = emitLight;
			return this;
		}

		public Builder lightBrightness(int lightBrightness) {
			this.lightBrightness = Math.max(0, Math.min(15, lightBrightness));
			return this;
		}

		public Builder lightFadeTicks(int lightFadeTicks) {
			this.lightFadeTicks = Math.max(1, lightFadeTicks);
			return this;
		}

		public Builder maxSegments(int maxSegments) {
			this.maxSegments = Math.max(2, maxSegments);
			return this;
		}

		public Builder segmentLength(double segmentLength) {
			this.segmentLength = Math.max(0.1, segmentLength);
			return this;
		}

		public Builder jitter(double jitter) {
			this.jitter = Math.max(0.0, jitter);
			return this;
		}

		public Builder mainArcs(int mainArcs) {
			this.mainArcs = Math.max(1, mainArcs);
			return this;
		}

		public Builder branchChance(double branchChance) {
			this.branchChance = Math.max(0.0, Math.min(1.0, branchChance));
			return this;
		}

		public Builder branchInterval(int min, int max) {
			this.branchIntervalMin = Math.max(1, min);
			this.branchIntervalMax = Math.max(this.branchIntervalMin, max);
			return this;
		}

		public Builder branchLength(double branchLength) {
			this.branchLength = Math.max(0.0, branchLength);
			return this;
		}

		public Builder subBranchChance(double subBranchChance) {
			this.subBranchChance = Math.max(0.0, Math.min(1.0, subBranchChance));
			return this;
		}

		public Builder subBranchLength(double subBranchLength) {
			this.subBranchLength = Math.max(0.0, subBranchLength);
			return this;
		}

		public Builder branchJitter(double branchJitter) {
			this.branchJitter = Math.max(0.0, branchJitter);
			return this;
		}

		public Builder taper(double taper) {
			this.taper = Math.max(0.0, Math.min(0.9, taper));
			return this;
		}

		public Builder branchThickness(double branchThickness) {
			this.branchThickness = Math.max(0.15, Math.min(1.0, branchThickness));
			return this;
		}

		public Builder slant(double slant) {
			this.slant = Math.max(0.0, Math.min(0.5, slant));
			return this;
		}

		public Builder roll(double roll) {
			this.roll = Math.max(0.0, Math.min(1.0, roll));
			return this;
		}

		public Builder flicker(double flicker) {
			this.flicker = Math.max(0.0, Math.min(0.5, flicker));
			return this;
		}

		public Builder clipToBlocks(boolean clipToBlocks) {
			this.clipToBlocks = clipToBlocks;
			return this;
		}

		public Builder displayTag(String displayTag) {
			this.displayTag = displayTag == null || displayTag.isEmpty() ? "jedcore_lightning" : displayTag;
			return this;
		}

		public LightningStyle build() {
			return new LightningStyle(this);
		}
	}
}
