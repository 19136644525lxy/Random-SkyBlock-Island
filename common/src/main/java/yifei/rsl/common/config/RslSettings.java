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
	/** Probability of spawning the hidden "egg" island on each transform. */
	public static final double DEFAULT_EGG_CHANCE = 0.20;
	public static final int DEFAULT_EGG_MIN_RADIUS = 24;
	public static final int DEFAULT_EGG_MAX_RADIUS = 50;
	public static final int DEFAULT_EGG_VOLUME = 27;
	public static final int DEFAULT_EGG_MIN_DISTANCE = 16;
	/** Default volume (0..1) for the mod's feedback sounds. */
	public static final double DEFAULT_SOUND_VOLUME = 0.7;

	public int configVersion = RslConstants.CONFIG_VERSION;
	public int transformDelaySeconds = DEFAULT_TRANSFORM_DELAY_SECONDS;
	public int maxAreaVolume = DEFAULT_MAX_AREA_VOLUME;
	public int blocksPerTick = DEFAULT_BLOCKS_PER_TICK;
	public String broadcastMode = DEFAULT_BROADCAST_MODE;
	public int broadcastRadius = DEFAULT_BROADCAST_RADIUS;
	public AreaSpec defaultArea = AreaSpec.defaultArea();
	public List<String> allowedBlocks = new ArrayList<>();
	/**
	 * Container entries of the normal pool: block state + captured block-entity
	 * NBT. They only participate in transforms of areas with a volume of at most
	 * {@link #MAX_CONTAINER_AREA_VOLUME} blocks.
	 */
	public List<PoolEntry> nbtPool = new ArrayList<>();
	/** Containers are only rolled into a transform for areas up to this volume. */
	public static final int MAX_CONTAINER_AREA_VOLUME = 27;
	/**
	 * Explicit block-pool editors. Vanilla permission levels are deliberately not
	 * consulted by the pool UI/commands; the server console and single-player host
	 * are handled separately by the platform layer.
	 */
	public List<PoolAccessEntry> poolAccess = new ArrayList<>();
	/**
	 * Hidden "other pool" used by the egg island. Empty means the egg mode never
	 * spawns anything even when enabled.
	 */
	public boolean eggModeEnabled = false;
	public double eggChance = DEFAULT_EGG_CHANCE;
	public int eggMinRadius = DEFAULT_EGG_MIN_RADIUS;
	public int eggMaxRadius = DEFAULT_EGG_MAX_RADIUS;
	public int eggVolume = DEFAULT_EGG_VOLUME;
	public int eggMinDistance = DEFAULT_EGG_MIN_DISTANCE;
	public List<String> eggPool = new ArrayList<>();
	/** Whether the mod plays UI/feedback sounds at all. */
	public boolean soundEnabled = true;
	/** Master volume (0..1) applied on top of each sound's own volume. */
	public double soundVolume = DEFAULT_SOUND_VOLUME;

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
		copy.nbtPool = new ArrayList<>();
		if (nbtPool != null) {
			for (PoolEntry entry : nbtPool) {
				if (entry != null) {
					copy.nbtPool.add(entry.copy());
				}
			}
		}
		copy.poolAccess = new ArrayList<>();
		if (poolAccess != null) {
			for (PoolAccessEntry entry : poolAccess) {
				if (entry != null) {
					copy.poolAccess.add(entry.copy());
				}
			}
		}
		copy.eggModeEnabled = eggModeEnabled;
		copy.eggChance = eggChance;
		copy.eggMinRadius = eggMinRadius;
		copy.eggMaxRadius = eggMaxRadius;
		copy.eggVolume = eggVolume;
		copy.eggMinDistance = eggMinDistance;
		copy.eggPool = new ArrayList<>(eggPool == null ? List.of() : eggPool);
		copy.soundEnabled = soundEnabled;
		copy.soundVolume = soundVolume;
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

		allowedBlocks = normalizeIds(allowedBlocks);
		nbtPool = normalizeEntries(nbtPool);
		PoolAccess.sanitize(poolAccess);

		// Gson skips field initializers, so a missing new field arrives as 0/false.
		// Invalid sentinel values fall back to the documented defaults.
		eggChance = clampDouble(eggChance, 0.0, 1.0);
		if (eggChance <= 0.0) {
			eggChance = DEFAULT_EGG_CHANCE;
		}
		eggMinRadius = clampOr(eggMinRadius, 1, 256, DEFAULT_EGG_MIN_RADIUS);
		eggMaxRadius = clampOr(eggMaxRadius, eggMinRadius, 512, DEFAULT_EGG_MAX_RADIUS);
		eggVolume = clampOr(eggVolume, 1, 4096, DEFAULT_EGG_VOLUME);
		eggMinDistance = clampOr(eggMinDistance, 1, 256, DEFAULT_EGG_MIN_DISTANCE);
		eggPool = normalizeIds(eggPool);

		soundVolume = clampDouble(soundVolume, 0.0, 1.0);
	}

	private static List<String> normalizeIds(List<String> source) {
		if (source == null) {
			return new ArrayList<>();
		}
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
		return cleaned;
	}

	private static List<PoolEntry> normalizeEntries(List<PoolEntry> source) {
		if (source == null) {
			return new ArrayList<>();
		}
		List<PoolEntry> cleaned = new ArrayList<>(source.size());
		Set<String> seen = new HashSet<>();
		for (PoolEntry raw : source) {
			if (raw == null) {
				continue;
			}
			String id = BlockId.normalize(raw.id);
			if (id == null) {
				continue;
			}
			String state = raw.state == null ? "" : raw.state.trim();
			if (state.isEmpty() || !state.startsWith(id)) {
				state = id;
			}
			String nbt = raw.nbt == null ? "" : raw.nbt.trim();
			if (!nbt.isEmpty() && !PoolEntry.isValidBase64(nbt)) {
				nbt = "";
			}
			PoolEntry entry = new PoolEntry(id, state, nbt);
			if (seen.add(entry.key())) {
				cleaned.add(entry);
			}
		}
		return cleaned;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int clampOr(int value, int min, int max, int fallback) {
		if (value <= 0) {
			return fallback;
		}
		return clamp(value, min, max);
	}

	private static double clampDouble(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
