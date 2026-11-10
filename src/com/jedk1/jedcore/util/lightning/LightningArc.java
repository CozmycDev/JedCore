package com.jedk1.jedcore.util.lightning;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class LightningArc {

	private static final Random RANDOM = new Random();

	private final List<Location> path;
	private final List<List<Location>> branches;
	private final List<Segment> segments;
	private final List<Location> branchEndpoints;
	private final List<Location> blockHits;

	private LightningArc(List<Location> path, List<List<Location>> branches, List<Location> blockHits) {
		this.path = Collections.unmodifiableList(copyLocations(path));

		List<List<Location>> branchCopy = new ArrayList<>();
		List<Location> endpoints = new ArrayList<>();
		List<Segment> allSegments = new ArrayList<>();

		addSegments(allSegments, this.path, false);
		for (List<Location> branch : branches) {
			List<Location> branchPath = copyLocations(branch);
			branchCopy.add(Collections.unmodifiableList(branchPath));
			addSegments(allSegments, branchPath, true);
			if (!branchPath.isEmpty()) {
				endpoints.add(branchPath.get(branchPath.size() - 1).clone());
			}
		}

		this.branches = Collections.unmodifiableList(branchCopy);
		this.segments = Collections.unmodifiableList(allSegments);
		this.branchEndpoints = Collections.unmodifiableList(endpoints);
		this.blockHits = Collections.unmodifiableList(copyLocations(blockHits));
	}

	public static LightningArc create(Location start, Location end, LightningStyle style) {
		List<Location> blockHits = new ArrayList<>();
		List<Location> path = generatePath(start, end, 1.0, style, blockHits);
		List<List<Location>> branches = generateBranches(path, style, blockHits);
		return new LightningArc(path, branches, blockHits);
	}

	public static List<LightningArc> beam(Location start, Location end, LightningStyle style) {
		List<LightningArc> arcs = new ArrayList<>();
		for (int i = 0; i < style.getMainArcs(); i++) {
			arcs.add(create(start, end, style));
		}
		return arcs;
	}

	public static List<LightningArc> around(Location center, double radius, int count, LightningStyle style) {
		List<LightningArc> arcs = new ArrayList<>();
		if (count <= 0) {
			return arcs;
		}

		double horizontalRadius = Math.max(0.1, radius);
		double verticalSpread = horizontalRadius * 0.9;
		double baseAngle = RANDOM.nextDouble() * Math.PI * 2.0;

		for (int i = 0; i < count; i++) {
			double angle = baseAngle + (Math.PI * 2.0 * i / count) + (RANDOM.nextDouble() - 0.5) * 0.4;
			double span = 0.8 + RANDOM.nextDouble() * 0.6;
			double startY = (RANDOM.nextDouble() - 0.5) * verticalSpread;
			double endY = (RANDOM.nextDouble() - 0.5) * verticalSpread;
			double endAngle = angle + span;

			Location start = center.clone().add(
					Math.cos(angle) * horizontalRadius,
					startY,
					Math.sin(angle) * horizontalRadius
			);
			Location end = center.clone().add(
					Math.cos(endAngle) * horizontalRadius,
					endY,
					Math.sin(endAngle) * horizontalRadius
			);

			arcs.add(create(start, end, style));
		}

		return arcs;
	}

	public List<Location> getPath() {
		return path;
	}

	public List<List<Location>> getBranches() {
		return branches;
	}

	public List<Segment> getSegments() {
		return segments;
	}

	public List<Location> getBranchEndpoints() {
		return branchEndpoints;
	}

	public List<Location> getBlockHits() {
		return blockHits;
	}

	public Location getEnd() {
		return path.isEmpty() ? null : path.get(path.size() - 1);
	}

	private static void addSegments(List<Segment> segments, List<Location> path, boolean branch) {
		double totalLength = pathLength(path);
		double coveredLength = 0.0;

		for (int i = 0; i < path.size() - 1; i++) {
			double segmentLength = path.get(i).distance(path.get(i + 1));
			double progress = totalLength > 0.001 ? coveredLength / totalLength : (path.size() > 1 ? (double) i / (path.size() - 1) : 0.0);
			segments.add(new Segment(path.get(i), path.get(i + 1), progress, branch));
			coveredLength += segmentLength;
		}
	}

	private static List<List<Location>> generateBranches(List<Location> path, LightningStyle style, List<Location> blockHits) {
		List<List<Location>> branches = new ArrayList<>();
		if (path.size() < 3 || style.getBranchChance() <= 0.0) {
			return branches;
		}

		double mainLength = pathLength(path);
		if (mainLength < 0.001) {
			return branches;
		}

		int interval = style.getBranchIntervalMin()
				+ RANDOM.nextInt(style.getBranchIntervalMax() - style.getBranchIntervalMin() + 1);

		for (int i = interval; i < path.size() - 1; i += interval) {
			if (RANDOM.nextDouble() >= style.getBranchChance()) {
				continue;
			}

			Location branchStart = path.get(i);
			Vector segmentDirection = direction(path.get(i - 1), branchStart);
			if (segmentDirection == null) {
				continue;
			}

			List<Location> branchPath = generateBranchPath(
					branchStart,
					segmentDirection,
					mainLength * style.getBranchLength(),
					style,
					blockHits
			);
			if (branchPath.size() <= 1) {
				continue;
			}

			branches.add(branchPath);

			if (branchPath.size() > 3 && RANDOM.nextDouble() < style.getSubBranchChance()) {
				int subBranchIndex = branchPath.size() / 2;
				Location subBranchStart = branchPath.get(subBranchIndex);
				Vector subBranchDirection = direction(branchPath.get(subBranchIndex - 1), subBranchStart);
				if (subBranchDirection == null) {
					continue;
				}

				List<Location> subBranchPath = generateBranchPath(
						subBranchStart,
						subBranchDirection,
						mainLength * style.getSubBranchLength(),
						style,
						blockHits
				);
				if (subBranchPath.size() > 1) {
					branches.add(subBranchPath);
				}
			}
		}

		return branches;
	}

	private static List<Location> generateBranchPath(Location start, Vector baseDirection, double length, LightningStyle style, List<Location> blockHits) {
		if (length <= 0.001) {
			return new ArrayList<>();
		}

		double yawOffset = Math.toRadians((RANDOM.nextDouble() - 0.5) * 70);
		double pitchOffset = Math.toRadians((RANDOM.nextDouble() - 0.5) * 50);

		Vector forward = baseDirection.clone().normalize();
		Vector right = forward.clone().crossProduct(new Vector(0, 1, 0));
		if (right.lengthSquared() < 0.01) {
			right = forward.clone().crossProduct(new Vector(1, 0, 0));
		}
		right.normalize();

		Vector up = right.clone().crossProduct(forward).normalize();
		Vector branchDirection = rotateVectorAroundAxis(forward, up, yawOffset);
		branchDirection = rotateVectorAroundAxis(branchDirection, right, pitchOffset).normalize();

		Location branchEnd = start.clone().add(branchDirection.multiply(length));
		return generatePath(start, branchEnd, style.getBranchJitter(), style, blockHits);
	}

	private static List<Location> generatePath(Location start, Location end, double jitterMultiplier, LightningStyle style, List<Location> blockHits) {
		List<Location> path = new ArrayList<>();
		path.add(start.clone());

		Vector overallDirection = end.toVector().subtract(start.toVector());
		double totalDistance = overallDirection.length();

		if (totalDistance < 0.1) {
			path.add(end.clone());
		} else {

		overallDirection.normalize();

		Vector perpendicular1 = overallDirection.clone().crossProduct(new Vector(0, 1, 0));
		if (perpendicular1.lengthSquared() < 0.01) {
			perpendicular1 = overallDirection.clone().crossProduct(new Vector(1, 0, 0));
		}
		perpendicular1.normalize();

		Vector perpendicular2 = overallDirection.clone().crossProduct(perpendicular1).normalize();

		Location current = start.clone();
		double distanceCovered = 0.0;
		int segmentCount = 0;

		while (distanceCovered < totalDistance && segmentCount < style.getMaxSegments()) {
			double stepSize = Math.min(style.getSegmentLength(), totalDistance - distanceCovered);
			Vector step = overallDirection.clone().multiply(stepSize);

			double jitterX = (RANDOM.nextDouble() - 0.5) * 2.0 * style.getJitter() * jitterMultiplier;
			double jitterY = (RANDOM.nextDouble() - 0.5) * 2.0 * style.getJitter() * jitterMultiplier;
			Vector jitter = perpendicular1.clone().multiply(jitterX)
					.add(perpendicular2.clone().multiply(jitterY));

			current = current.add(step).add(jitter);
			path.add(current.clone());

			distanceCovered += stepSize;
			segmentCount++;
		}

		Location lastPoint = path.get(path.size() - 1);
		double remainingDistance = lastPoint.distance(end);
		if (remainingDistance > 0.01) {
			if (remainingDistance <= 0.45) {
				path.set(path.size() - 1, end.clone());
			} else {
				path.add(end.clone());
			}
		}
		}

		if (style.isClipToBlocks()) {
			path = clipGeneratedPath(path, blockHits);
		}

		return path;
	}

	private static List<Location> clipGeneratedPath(List<Location> path, List<Location> blockHits) {
		if (path.size() < 2) {
			return path;
		}

		List<Location> clipped = new ArrayList<>();
		clipped.add(path.get(0).clone());

		for (int i = 1; i < path.size(); i++) {
			Location from = clipped.get(clipped.size() - 1);
			Location to = path.get(i);
			if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
				clipped.add(to.clone());
				continue;
			}

			double distance = from.distance(to);
			if (distance < 0.001) {
				clipped.add(to.clone());
				continue;
			}

			Vector direction = to.toVector().subtract(from.toVector());
			RayTraceResult hit = from.getWorld().rayTraceBlocks(
					from,
					direction,
					distance,
					FluidCollisionMode.NEVER,
					true
			);

			if (hit != null && hit.getHitPosition() != null) {
				Location impact = hit.getHitPosition().toLocation(from.getWorld());
				clipped.add(impact.clone());
				if (blockHits != null) {
					blockHits.add(impact.clone());
				}
				break;
			}

			clipped.add(to.clone());
		}

		return clipped.size() < 2 ? path : clipped;
	}

	private static Vector direction(Location from, Location to) {
		Vector direction = to.toVector().subtract(from.toVector());
		if (direction.lengthSquared() < 0.0001) {
			return null;
		}
		return direction.normalize();
	}

	private static Vector rotateVectorAroundAxis(Vector vector, Vector axis, double angle) {
		Vector normalizedAxis = axis.clone().normalize();
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);

		return vector.clone().multiply(cos)
				.add(normalizedAxis.clone().crossProduct(vector).multiply(sin))
				.add(normalizedAxis.clone().multiply(normalizedAxis.dot(vector) * (1.0 - cos)));
	}

	private static double pathLength(List<Location> path) {
		double length = 0.0;
		for (int i = 0; i < path.size() - 1; i++) {
			length += path.get(i).distance(path.get(i + 1));
		}
		return length;
	}

	private static List<Location> copyLocations(List<Location> locations) {
		List<Location> copy = new ArrayList<>(locations.size());
		for (Location location : locations) {
			copy.add(location.clone());
		}
		return copy;
	}

	public static final class Segment {
		private final Location from;
		private final Location to;
		private final double progress;
		private final boolean branch;

		public Segment(Location from, Location to) {
			this(from, to, 0.0, false);
		}

		public Segment(Location from, Location to, double progress, boolean branch) {
			this.from = from.clone();
			this.to = to.clone();
			this.progress = Math.max(0.0, Math.min(1.0, progress));
			this.branch = branch;
		}

		public Location getFrom() {
			return from;
		}

		public Location getTo() {
			return to;
		}

		public double getProgress() {
			return progress;
		}

		public boolean isBranch() {
			return branch;
		}

		public Location getMidpoint() {
			return from.clone().add(to).multiply(0.5);
		}

		public Location getDisplayLocation() {
			Location displayLocation = getMidpoint();
			displayLocation.setYaw(0.0f);
			displayLocation.setPitch(0.0f);
			return displayLocation;
		}

		public double getLength() {
			return from.distance(to);
		}
	}
}
