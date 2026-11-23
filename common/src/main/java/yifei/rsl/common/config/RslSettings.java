package yifei.rsl.common.config;

import yifei.rsl.common.RslConstants;
import yifei.rsl.common.block.BlockId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Contents of {@code randomskyblockisland.json}. Pure data, safe for Gson.
 */
public final class RslSettings {
	public static final int DEFAULT_TRANSFORM_DELAY_SECONDS = 180;
	public static final int DEFAULT_MAX_AREA_VOLUME = 65536;
	public static final int DEFAULT_BLOCKS_PER_TICK = 8192;
	public static final String DEFAULT_BROADCAST_MODE = "all";
	public static final int DEFAULT_BROADCAST_RADIUS = 64;

	public int configVersion = RslConstants.CONFIG_VERSION;
	public int transformDelaySeconds = DEFAULT_TRANSFORM_DELAY_SECONDS;
	public int maxAreaVolume = DEFAULT_MAX_AREA_VOLUME;
	public int blocksPerTick = DEFAULT_BLOCKS_PER_TICK;
	public String broadcastMode = DEFAULT_BROADCAST_MODE;
	public int broadcastRadius = DEFAULT_BROADCAST_RADIUS;
	public AreaSpec defaultArea = AreaSpec.defaultArea();
	public List<String> allowedBlocks = new ArrayList<>();
	/**
	 * Explicit block-pool editors. Vanilla permission levels are deliberately not
	 * consulted by the pool UI/commands; the server console and single-player host
	 * are handled separately by the platform layer.
	 */
	public List<PoolAccessEntry> poolAccess = new ArrayList<>();

	public RslSettings() {
	}

	public RslSettings copy() {
		RslSettings copy = new RslSettings();
		copy.configVersion = configVersion;
		copy.transformDelaySeconds = transformDelaySeconds;
		copy.maxAreaVolume = maxAreaVolume;
		copy.blocksPerTick = blocksPerTick;
		copy.broadcastMode = broadcastMode;
		copy.broadcastRadius = broadcastRadius;
		copy.defaultArea = defaultArea == null ? AreaSpec.defaultArea() : defaultArea.copy();
		copy.allowedBlocks = new ArrayList<>(allowedBlocks == null ? List.of() : allowedBlocks);
		copy.poolAccess = new ArrayList<>();
		if (poolAccess != null) {
			for (PoolAccessEntry entry : poolAccess) {
				if (entry != null) {
					copy.poolAccess.add(entry.copy());
				}
			}
		}
		return copy;
	}

	/**
	 * Clamps numeric values, normalizes the default area and de-duplicates raw block ids.
	 * Invalid block ids are intentionally kept (trimmed) so they can be reported instead of
	 * being silently dropped from a user's config.
	 */
	public void sanitize() {
		configVersion = RslConstants.CONFIG_VERSION;
		transformDelaySeconds = clamp(transformDelaySeconds, 1, 86_400);
		maxAreaVolume = clamp(maxAreaVolume, 1, 4_000_000);
		blocksPerTick = clamp(blocksPerTick, 256, 1_048_576);
		broadcastRadius = clamp(broadcastRadius, 1, 512);

		String mode = broadcastMode == null ? "" : broadcastMode.trim().toLowerCase(Locale.ROOT);
		if (!mode.equals("all") && !mode.equals("nearby") && !mode.equals("summary")) {
			mode = DEFAULT_BROADCAST_MODE;
		}
		broadcastMode = mode;

		if (defaultArea == null) {
			defaultArea = AreaSpec.defaultArea();
		} else {
			defaultArea = defaultArea.normalized();
			if (defaultArea.name == null || defaultArea.name.trim().isEmpty()) {
				defaultArea.name = "default";
			}
		}

		List<String> source = allowedBlocks == null ? List.of() : allowedBlocks;
		List<String> cleaned = new ArrayList<>(source.size());
		Set<String> seen = new HashSet<>();
		for (String raw : source) {
			if (raw == null || raw.trim().isEmpty()) {
				continue;
			}
			String normalized = BlockId.normalize(raw);
			String key = normalized != null ? normalized : raw.trim().toLowerCase(Locale.ROOT);
			if (seen.add(key)) {
				cleaned.add(normalized != null ? normalized : raw.trim());
			}
		}
		allowedBlocks = cleaned;
		PoolAccess.sanitize(poolAccess);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
