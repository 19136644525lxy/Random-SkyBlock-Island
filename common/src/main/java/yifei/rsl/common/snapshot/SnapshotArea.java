package yifei.rsl.common.snapshot;

import java.util.List;

/**
 * A block snapshot of one cuboid area.
 *
 * <p>The palette stores encoded block states (see the platform-side codec) and
 * {@code indices} maps every position to a palette entry. Positions are
 * iterated in x, then y, then z order.</p>
 */
public record SnapshotArea(String name,
		int minX, int minY, int minZ,
		int sizeX, int sizeY, int sizeZ,
		List<String> palette, int[] indices) {

	public SnapshotArea {
		if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
			throw new IllegalArgumentException("snapshot area has an empty dimension");
		}
		if (palette == null || palette.isEmpty()) {
			throw new IllegalArgumentException("snapshot area has an empty palette");
		}
		long count = (long) sizeX * sizeY * sizeZ;
		if (indices == null || indices.length != count) {
			throw new IllegalArgumentException("snapshot index count does not match the area volume");
		}
		palette = List.copyOf(palette);
		indices = indices.clone();
	}

	@Override
	public int[] indices() {
		return indices.clone();
	}

	public long blockCount() {
		return (long) sizeX * sizeY * sizeZ;
	}
}
