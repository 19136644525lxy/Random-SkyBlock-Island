package yifei.rsl.common.config;

import yifei.rsl.common.RslConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Contents of the per-save {@code rsl_areas.json}.
 */
public final class AreasFile {
	public int configVersion = RslConstants.CONFIG_VERSION;
	/**
	 * Dimension currently locked by the running game for this save. {@code null}
	 * means no game is running. Files written by older versions simply omit it.
	 */
	public String activeDimension;
	/**
	 * Per-save default area. {@code null} for files written by older versions; the
	 * platform layer seeds it from the global initial area on first load.
	 */
	public AreaSpec defaultArea;
	public List<AreaSpec> areas = new ArrayList<>();

	public AreasFile() {
	}

	public AreasFile copy() {
		AreasFile copy = new AreasFile();
		copy.configVersion = configVersion;
		copy.activeDimension = activeDimension;
		copy.defaultArea = defaultArea == null ? null : defaultArea.copy();
		copy.areas = new ArrayList<>();
		if (areas != null) {
			for (AreaSpec area : areas) {
				if (area != null) {
					copy.areas.add(area.copy());
				}
			}
		}
		return copy;
	}

	/**
	 * Normalizes every area and drops null entries. Invalid names/volumes are kept so the
	 * platform layer can report them instead of silently deleting a user's area.
	 */
	public void sanitize() {
		configVersion = RslConstants.CONFIG_VERSION;
		activeDimension = DimensionId.normalize(activeDimension);
		if (defaultArea != null) {
			defaultArea = defaultArea.normalized();
		}
		if (areas == null) {
			areas = new ArrayList<>();
			return;
		}
		List<AreaSpec> cleaned = new ArrayList<>(areas.size());
		for (AreaSpec area : areas) {
			if (area != null) {
				cleaned.add(area.normalized());
			}
		}
		areas = cleaned;
	}
}
