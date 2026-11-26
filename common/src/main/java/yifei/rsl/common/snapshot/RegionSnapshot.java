package yifei.rsl.common.snapshot;

import java.util.List;

/**
 * The block snapshot of every configured area, taken before the first game
 * start. It is stored next to the save file and used by {@code reset} to put
 * the terrain back to that state.
 */
public record RegionSnapshot(String dimension, List<SnapshotArea> areas) {

	public RegionSnapshot {
		if (dimension == null || dimension.isBlank()) {
			throw new IllegalArgumentException("snapshot dimension is missing");
		}
		if (areas == null) {
			throw new IllegalArgumentException("snapshot areas are missing");
		}
		areas = List.copyOf(areas);
	}

	public long blockCount() {
		long total = 0L;
		for (SnapshotArea area : areas) {
			total += area.blockCount();
		}
		return total;
	}
}
