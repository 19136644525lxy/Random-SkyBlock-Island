package yifei.rsl.common.config;

import java.util.regex.Pattern;

/**
 * A loader-independent cuboid area. Coordinates are inclusive on both ends.
 *
 * <p>The class is mutable so that Gson can populate it, but all consumers should
 * call {@link #normalized()} before doing math or comparisons.</p>
 */
public final class AreaSpec {
	private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");
	private static final long MAX_WORLD_COORDINATE = 30_000_000L;

	public String name;
	public int minX;
	public int minY;
	public int minZ;
	public int maxX;
	public int maxY;
	public int maxZ;

	public AreaSpec() {
		this("default", 0, 0, 0, 0, 0, 0);
	}

	public AreaSpec(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.name = name;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	public static AreaSpec defaultArea() {
		return new AreaSpec("default", -1, 64, -4, 4, 66, 1);
	}

	public AreaSpec copy() {
		return new AreaSpec(name, minX, minY, minZ, maxX, maxY, maxZ);
	}

	/**
	 * @return a new instance with every axis ordered from low to high.
	 */
	public AreaSpec normalized() {
		return new AreaSpec(
				name,
				Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
				Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ));
	}

	/**
	 * Normalizes this instance in place.
	 */
	public void normalizeInPlace() {
		int x0 = Math.min(minX, maxX);
		int y0 = Math.min(minY, maxY);
		int z0 = Math.min(minZ, maxZ);
		int x1 = Math.max(minX, maxX);
		int y1 = Math.max(minY, maxY);
		int z1 = Math.max(minZ, maxZ);
		minX = x0;
		minY = y0;
		minZ = z0;
		maxX = x1;
		maxY = y1;
		maxZ = z1;
	}

	/**
	 * Volume of the normalized area, saturated at {@link Long#MAX_VALUE} instead of
	 * overflowing. The raw coordinates are ints, but their product is not.
	 */
	public long volumeSaturated() {
		AreaSpec n = normalized();
		long dx = (long) n.maxX - n.minX + 1L;
		long dy = (long) n.maxY - n.minY + 1L;
		long dz = (long) n.maxZ - n.minZ + 1L;
		if (dx <= 0L || dy <= 0L || dz <= 0L) {
			return 0L;
		}
		if (dx > Long.MAX_VALUE / dy) {
			return Long.MAX_VALUE;
		}
		long dxy = dx * dy;
		if (dxy > Long.MAX_VALUE / dz) {
			return Long.MAX_VALUE;
		}
		return dxy * dz;
	}

	public boolean contains(int x, int y, int z) {
		int x0 = Math.min(minX, maxX);
		int y0 = Math.min(minY, maxY);
		int z0 = Math.min(minZ, maxZ);
		int x1 = Math.max(minX, maxX);
		int y1 = Math.max(minY, maxY);
		int z1 = Math.max(minZ, maxZ);
		return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
	}

	public boolean hasValidName() {
		return name != null && NAME_PATTERN.matcher(name).matches();
	}

	public boolean hasValidCoordinates() {
		return insideWorldCoordinate(minX) && insideWorldCoordinate(minY) && insideWorldCoordinate(minZ)
				&& insideWorldCoordinate(maxX) && insideWorldCoordinate(maxY) && insideWorldCoordinate(maxZ);
	}

	private static boolean insideWorldCoordinate(int value) {
		return Math.abs((long) value) <= MAX_WORLD_COORDINATE;
	}

	public String display() {
		return name + ": (" + minX + "," + minY + "," + minZ + ") ~ (" + maxX + "," + maxY + "," + maxZ + ")";
	}

	@Override
	public String toString() {
		return display();
	}
}
