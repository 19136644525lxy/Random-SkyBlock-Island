package yifei.rsl.common.world;

import yifei.rsl.common.config.AreaSpec;

/**
 * Iterates an inclusive cuboid in the same order as the old nested loops:
 * x outer, y middle, z inner. Coordinates outside the world height are skipped.
 */
public final class FillCursor {
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;
	private final long total;

	private long done;
	private int x;
	private int y;
	private int z;

	public FillCursor(AreaSpec area, int worldBottomY, int worldTopYExclusive) {
		AreaSpec normalized = area.normalized();
		this.minX = normalized.minX;
		this.maxX = normalized.maxX;
		this.minZ = normalized.minZ;
		this.maxZ = normalized.maxZ;
		this.minY = Math.max(normalized.minY, worldBottomY);
		this.maxY = (int) Math.min((long) normalized.maxY, (long) worldTopYExclusive - 1L);

		if (this.minY > this.maxY || this.minX > this.maxX || this.minZ > this.maxZ) {
			this.total = 0L;
			this.x = this.minX;
			this.y = this.minY;
			this.z = this.minZ;
			return;
		}

		long dx = (long) this.maxX - this.minX + 1L;
		long dy = (long) this.maxY - this.minY + 1L;
		long dz = (long) this.maxZ - this.minZ + 1L;
		this.total = saturatingMultiply(saturatingMultiply(dx, dy), dz);
		this.x = this.minX;
		this.y = this.minY;
		this.z = this.minZ;
	}

	private static long saturatingMultiply(long a, long b) {
		if (a <= 0L || b <= 0L) {
			return 0L;
		}
		return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
	}

	public boolean hasNext() {
		return done < total;
	}

	public void advance() {
		if (!hasNext()) {
			return;
		}
		done++;
		if (z < maxZ) {
			z++;
			return;
		}
		z = minZ;
		if (y < maxY) {
			y++;
			return;
		}
		y = minY;
		x++;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	public long total() {
		return total;
	}

	public long done() {
		return done;
	}

	public long remaining() {
		return total - done;
	}

	public boolean isEmpty() {
		return total == 0L;
	}
}
