package yifei.rsl.config;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.common.block.BlockId;
import yifei.rsl.common.block.DefaultBlocks;
import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.common.config.AreasFile;
import yifei.rsl.common.config.BlockMutationResult;
import yifei.rsl.common.config.ConfigCodec;
import yifei.rsl.common.config.DimensionId;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.common.config.RslSettings;
import yifei.rsl.common.config.RslState;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Platform adapter around the shared {@link ConfigCodec}. Keeps the old static access points
 * used by commands and the game loop, but all persistence and validation lives in {@code common}.
 */
public final class ModConfig {
	private static final int GAME_PERMISSION_LEVEL = 4;
	private static final int MAX_POOL_SIZE = 4096;

	private static ConfigCodec codec;
	private static RslSettings settings = new RslSettings();
	private static RslState state = new RslState();
	/**
	 * Default area of the currently loaded save. The global {@code settings.defaultArea}
	 * is only the initial value used when a save has no stored default yet.
	 */
	private static AreaSpec worldDefaultArea = AreaSpec.defaultArea();
	private static boolean worldLoaded;
	/**
	 * Dimension locked by the current running game for the loaded save. {@code null}
	 * while idle or before a save is loaded.
	 */
	private static String activeDimension;
	private static final List<AreaSpec> saveAreas = new ArrayList<>();
	private static final List<String> allowedBlockIds = new ArrayList<>();
	private static final List<Block> allowedBlocks = new ArrayList<>();
	private static final List<String> invalidBlockIds = new ArrayList<>();
	private static List<AreaSpec> pendingLegacyAreas = new ArrayList<>();
	private static Path saveDir;
	private static boolean loaded;

	private ModConfig() {
	}

	public static void loadConfig() {
		Path configDir = FMLPaths.CONFIGDIR.get();
		codec = new ConfigCodec(configDir);
		try {
			ConfigCodec.LoadResult result = codec.load();
			settings = result.settings;
			state = result.state;
			worldDefaultArea = settings.defaultArea == null
					? AreaSpec.defaultArea()
					: settings.defaultArea.copy();
			worldLoaded = false;
			activeDimension = null;
			pendingLegacyAreas = new ArrayList<>(result.legacySaveAreas);
			for (String warning : result.warnings) {
				RandomSkyBlockIsland.LOGGER.warn("[RSL] {}", warning);
			}
			refreshBlockPool();
			loaded = true;
			RandomSkyBlockIsland.LOGGER.info(
					"[RSL] Loaded config: delay={}s, blocks={} (invalid={}), blocksPermission={}",
					settings.transformDelaySeconds, allowedBlocks.size(), invalidBlockIds.size(),
					settings.blocksPermissionLevel);
		} catch (IOException e) {
			RandomSkyBlockIsland.LOGGER.error("[RSL] Failed to load config, using in-memory defaults", e);
			settings = new RslSettings();
			settings.allowedBlocks = new ArrayList<>(DefaultBlocks.load());
			settings.sanitize();
			state = new RslState();
			worldDefaultArea = settings.defaultArea.copy();
			worldLoaded = false;
			activeDimension = null;
			refreshBlockPool();
			loaded = true;
		}
	}

	public static boolean isLoaded() {
		return loaded;
	}

	public static RslSettings getSettings() {
		return settings;
	}

	public static RslState getState() {
		return state;
	}

	public static int getTransformDelaySeconds() {
		return settings.transformDelaySeconds;
	}

	public static void setTransformDelaySeconds(int seconds) {
		settings.transformDelaySeconds = Math.max(1, Math.min(86_400, seconds));
		saveSettings();
	}

	public static int getMaxAreaVolume() {
		return settings.maxAreaVolume;
	}

	public static int getBlocksPerTick() {
		return settings.blocksPerTick;
	}

	public static String getBroadcastMode() {
		return settings.broadcastMode;
	}

	public static int getBroadcastRadius() {
		return settings.broadcastRadius;
	}

	public static int getGamePermissionLevel() {
		return GAME_PERMISSION_LEVEL;
	}

	public static int getBlocksPermissionLevel() {
		return settings.blocksPermissionLevel;
	}

	public static int getMaxPoolSize() {
		return MAX_POOL_SIZE;
	}

	public static boolean isRunning() {
		return state.isRunning;
	}

	// ========== 维度锁定 ==========

	/**
	 * @return the locked dimension id, or {@code null} when no game is running
	 */
	public static String getActiveDimension() {
		return activeDimension;
	}

	public static boolean hasActiveDimension() {
		return activeDimension != null;
	}

	/**
	 * @return true only while a game is running and a dimension is locked
	 */
	public static boolean isDimensionLocked() {
		return state.isRunning && activeDimension != null;
	}

	/**
	 * Sets the locked dimension in memory. The caller persists it with
	 * {@link #saveSaveAreas()}.
	 *
	 * @return false when {@code raw} is not a valid dimension id
	 */
	public static boolean setActiveDimension(String raw) {
		String id = DimensionId.normalize(raw);
		if (id == null) {
			return false;
		}
		activeDimension = id;
		return true;
	}

	public static void clearActiveDimension() {
		activeDimension = null;
	}

	public static boolean isActiveDimension(ServerLevel world) {
		return world != null && activeDimension != null
				&& world.dimension().location().toString().equals(activeDimension);
	}

	/**
	 * Resolves the locked dimension to a loaded server level, or {@code null} when the
	 * dimension is not loaded / not registered.
	 */
	public static ServerLevel findActiveWorld(MinecraftServer server) {
		if (server == null || activeDimension == null) {
			return null;
		}
		ResourceLocation id = ResourceLocation.tryParse(activeDimension);
		if (id == null) {
			return null;
		}
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
		return server.getLevel(key);
	}

	/**
	 * @return true when the player is in the locked dimension or no game is locked
	 */
	public static boolean isPlayerInActiveDimension(ServerPlayer player) {
		if (activeDimension == null || player == null) {
			return true;
		}
		return player.serverLevel().dimension().location().toString().equals(activeDimension);
	}

	public static void setRunning(boolean running) {
		state.isRunning = running;
	}

	public static int getSecondsRemaining() {
		return state.secondsRemaining;
	}

	public static void setSecondsRemaining(int seconds) {
		state.secondsRemaining = Math.max(0, seconds);
	}

	public static boolean saveSettings() {
		if (codec == null) {
			return false;
		}
		try {
			codec.saveSettings(settings);
			return true;
		} catch (IOException e) {
			RandomSkyBlockIsland.LOGGER.error("[RSL] Failed to save settings", e);
			return false;
		}
	}

	public static void saveState() {
		if (codec == null) {
			return;
		}
		try {
			codec.saveState(state);
		} catch (IOException e) {
			RandomSkyBlockIsland.LOGGER.error("[RSL] Failed to save state", e);
		}
	}

	// ========== 方块池 ==========

	/**
	 * @return raw, normalized ids exactly as stored in the settings file (unknown ids included)
	 */
	public static List<String> getAllowedBlockIds() {
		return List.copyOf(allowedBlockIds);
	}

	/**
	 * @return the subset of the configured pool that currently resolves to a non-air block
	 */
	public static List<Block> getAllowedBlocks() {
		return List.copyOf(allowedBlocks);
	}

	public static List<String> getInvalidBlockIds() {
		return List.copyOf(invalidBlockIds);
	}

	public static boolean isBlockPoolEmpty() {
		return allowedBlocks.isEmpty();
	}

	public static BlockMutationResult addBlockId(String raw) {
		String id = BlockId.normalize(raw);
		if (id == null) {
			return BlockMutationResult.INVALID_ID;
		}
		if (allowedBlockIds.contains(id)) {
			return BlockMutationResult.DUPLICATE;
		}
		ResourceLocation identifier = ResourceLocation.tryParse(id);
		if (identifier == null) {
			return BlockMutationResult.INVALID_ID;
		}
		if (!ForgeRegistries.BLOCKS.containsKey(identifier)) {
			return BlockMutationResult.UNKNOWN_BLOCK;
		}
		Block block = ForgeRegistries.BLOCKS.getValue(identifier);
		if (block == null || block == Blocks.AIR) {
			return BlockMutationResult.AIR_BLOCK;
		}
		if (settings.allowedBlocks.size() >= MAX_POOL_SIZE) {
			return BlockMutationResult.POOL_FULL;
		}
		settings.allowedBlocks.add(id);
		refreshBlockPool();
		if (!saveSettings()) {
			settings.allowedBlocks.remove(id);
			refreshBlockPool();
			return BlockMutationResult.IO_ERROR;
		}
		return BlockMutationResult.OK;
	}

	public static BlockMutationResult removeBlockId(String raw) {
		String id = BlockId.normalize(raw);
		int index = id != null
				? settings.allowedBlocks.indexOf(id)
				: settings.allowedBlocks.indexOf(raw.trim());
		if (index < 0) {
			return BlockMutationResult.NOT_FOUND;
		}
		String storedId = settings.allowedBlocks.remove(index);
		refreshBlockPool();
		if (state.isRunning && allowedBlocks.isEmpty()) {
			settings.allowedBlocks.add(Math.min(index, settings.allowedBlocks.size()), storedId);
			refreshBlockPool();
			return BlockMutationResult.WOULD_EMPTY_RUNNING;
		}
		if (!saveSettings()) {
			settings.allowedBlocks.add(Math.min(index, settings.allowedBlocks.size()), storedId);
			refreshBlockPool();
			return BlockMutationResult.IO_ERROR;
		}
		return BlockMutationResult.OK;
	}

	public static BlockMutationResult clearBlockIds() {
		if (state.isRunning) {
			return BlockMutationResult.WOULD_EMPTY_RUNNING;
		}
		List<String> previous = new ArrayList<>(settings.allowedBlocks);
		settings.allowedBlocks.clear();
		refreshBlockPool();
		if (!saveSettings()) {
			settings.allowedBlocks = previous;
			refreshBlockPool();
			return BlockMutationResult.IO_ERROR;
		}
		return BlockMutationResult.OK;
	}

	public static BlockMutationResult resetBlockIds() {
		List<String> previous = new ArrayList<>(settings.allowedBlocks);
		settings.allowedBlocks = new ArrayList<>(DefaultBlocks.load());
		refreshBlockPool();
		if (!saveSettings()) {
			settings.allowedBlocks = previous;
			refreshBlockPool();
			return BlockMutationResult.IO_ERROR;
		}
		return BlockMutationResult.OK;
	}

	/**
	 * Applies a batch of additions/removals atomically. If any id is rejected the
	 * pool is left untouched. Used by the block-pool screen.
	 */
	public static PoolMutationResult applyBlockMutations(List<String> add, List<String> remove) {
		List<String> previous = new ArrayList<>(settings.allowedBlocks);
		List<String> working = new ArrayList<>(settings.allowedBlocks);
		List<String> failed = new ArrayList<>();
		BlockMutationResult firstError = null;

		if (add != null) {
			for (String raw : add) {
				String id = BlockId.normalize(raw);
				if (id == null) {
					failed.add(raw == null ? "" : raw);
					if (firstError == null) firstError = BlockMutationResult.INVALID_ID;
					continue;
				}
				if (working.contains(id)) {
					failed.add(raw);
					if (firstError == null) firstError = BlockMutationResult.DUPLICATE;
					continue;
				}
				ResourceLocation identifier = ResourceLocation.tryParse(id);
				if (identifier == null) {
					failed.add(raw);
					if (firstError == null) firstError = BlockMutationResult.INVALID_ID;
					continue;
				}
				if (!ForgeRegistries.BLOCKS.containsKey(identifier)) {
					failed.add(raw);
					if (firstError == null) firstError = BlockMutationResult.UNKNOWN_BLOCK;
					continue;
				}
				Block block = ForgeRegistries.BLOCKS.getValue(identifier);
				if (block == null || block == Blocks.AIR) {
					failed.add(raw);
					if (firstError == null) firstError = BlockMutationResult.AIR_BLOCK;
					continue;
				}
				if (working.size() >= MAX_POOL_SIZE) {
					failed.add(raw);
					if (firstError == null) firstError = BlockMutationResult.POOL_FULL;
					continue;
				}
				working.add(id);
			}
		}

		if (remove != null) {
			for (String raw : remove) {
				String id = BlockId.normalize(raw);
				String target = id != null ? id : (raw == null ? null : raw.trim());
				int index = target == null ? -1 : working.indexOf(target);
				if (index < 0) {
					failed.add(raw == null ? "" : raw);
					if (firstError == null) firstError = BlockMutationResult.NOT_FOUND;
					continue;
				}
				working.remove(index);
			}
		}

		if (!failed.isEmpty()) {
			String key = switch (firstError == null ? BlockMutationResult.INVALID_ID : firstError) {
				case UNKNOWN_BLOCK -> "rsl.message.blocks_unknown";
				case AIR_BLOCK -> "rsl.message.blocks_air";
				case DUPLICATE -> "rsl.message.blocks_duplicate";
				case POOL_FULL -> "rsl.message.blocks_pool_full";
				case NOT_FOUND -> "rsl.message.blocks_not_found";
				default -> "rsl.message.blocks_invalid_id";
			};
			return PoolMutationResult.fail(firstError == null ? BlockMutationResult.INVALID_ID : firstError, key, failed);
		}

		settings.allowedBlocks = working;
		refreshBlockPool();
		if (state.isRunning && allowedBlocks.isEmpty()) {
			settings.allowedBlocks = previous;
			refreshBlockPool();
			return PoolMutationResult.fail(BlockMutationResult.WOULD_EMPTY_RUNNING,
					"rsl.message.blocks_would_empty", failed);
		}
		if (!saveSettings()) {
			settings.allowedBlocks = previous;
			refreshBlockPool();
			return PoolMutationResult.fail(BlockMutationResult.IO_ERROR,
					"rsl.message.blocks_save_failed", failed);
		}
		return PoolMutationResult.ok();
	}

	private static void refreshBlockPool() {
		allowedBlockIds.clear();
		allowedBlocks.clear();
		invalidBlockIds.clear();
		Set<String> seenIds = new HashSet<>();
		Set<Block> seenBlocks = new HashSet<>();
		for (String raw : settings.allowedBlocks) {
			if (raw == null || raw.trim().isEmpty()) {
				continue;
			}
			String id = BlockId.normalize(raw);
			if (id == null) {
				String rawId = raw.trim();
				if (seenIds.add(rawId)) {
					allowedBlockIds.add(rawId);
					invalidBlockIds.add(rawId);
				}
				continue;
			}
			if (!seenIds.add(id)) {
				continue;
			}
			allowedBlockIds.add(id);
			ResourceLocation identifier = ResourceLocation.tryParse(id);
			if (identifier == null) {
				invalidBlockIds.add(id);
				continue;
			}
			if (!ForgeRegistries.BLOCKS.containsKey(identifier)) {
				invalidBlockIds.add(id);
				continue;
			}
			Block block = ForgeRegistries.BLOCKS.getValue(identifier);
			if (block == null || block == Blocks.AIR) {
				invalidBlockIds.add(id);
				continue;
			}
			if (seenBlocks.add(block)) {
				allowedBlocks.add(block);
			}
		}
		if (!invalidBlockIds.isEmpty()) {
			RandomSkyBlockIsland.LOGGER.warn("[RSL] {} configured block id(s) are unavailable: {}",
					invalidBlockIds.size(), invalidBlockIds);
		}
	}

	// ========== 区域 ==========

	/**
	 * Default area of the currently loaded save. Mutating this object changes the save's
	 * default area; persist it with {@link #saveSaveAreas()}.
	 */
	public static AreaSpec getDefaultArea() {
		return worldDefaultArea;
	}

	/**
	 * Global initial default area. Used to seed saves that do not have their own default
	 * area yet, and by {@code /randomskyblock area initial ...}.
	 */
	public static AreaSpec getInitialDefaultArea() {
		return settings.defaultArea;
	}

	public static boolean isWorldLoaded() {
		return saveDir != null && worldLoaded;
	}

	public static List<AreaSpec> getSaveAreas() {
		return List.copyOf(saveAreas);
	}

	public static List<AreaSpec> getAllAreas() {
		List<AreaSpec> all = new ArrayList<>();
		all.add(worldDefaultArea);
		all.addAll(saveAreas);
		return all;
	}

	public static List<AreaSpec> getUsableAreas() {
		List<AreaSpec> usable = new ArrayList<>();
		for (AreaSpec area : getAllAreas()) {
			if (isValidArea(area)) {
				usable.add(area);
			}
		}
		return usable;
	}

	public static AreaSpec getAreaByName(String name) {
		if (worldDefaultArea.name.equals(name)) {
			return worldDefaultArea;
		}
		return saveAreas.stream().filter(a -> a.name.equals(name)).findFirst().orElse(null);
	}

	public static boolean addArea(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		if (getAreaByName(name) != null) {
			return false;
		}
		AreaSpec area = new AreaSpec(name, minX, minY, minZ, maxX, maxY, maxZ).normalized();
		if (!isValidArea(area)) {
			return false;
		}
		saveAreas.add(area);
		return true;
	}

	public static boolean removeArea(String name) {
		if (worldDefaultArea.name.equals(name)) {
			return false;
		}
		return saveAreas.removeIf(a -> a.name.equals(name));
	}

	public static boolean renameArea(String oldName, String newName) {
		if (newName == null || getAreaByName(newName) != null) {
			return false;
		}
		AreaSpec probe = new AreaSpec(newName, 0, 0, 0, 0, 0, 0);
		if (!probe.hasValidName()) {
			return false;
		}
		if (worldDefaultArea.name.equals(oldName)) {
			worldDefaultArea.name = newName;
			return true;
		}
		AreaSpec area = saveAreas.stream().filter(a -> a.name.equals(oldName)).findFirst().orElse(null);
		if (area == null) {
			return false;
		}
		area.name = newName;
		return true;
	}

	public static boolean replaceArea(String originalName, String newName,
			int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		if (originalName == null || newName == null || worldDefaultArea.name.equals(originalName)) {
			return false;
		}
		AreaSpec target = saveAreas.stream()
				.filter(a -> a.name.equals(originalName))
				.findFirst().orElse(null);
		if (target == null) {
			return false;
		}
		AreaSpec replacement = new AreaSpec(newName, minX, minY, minZ, maxX, maxY, maxZ).normalized();
		if (!isValidArea(replacement)) {
			return false;
		}
		if (!replacement.name.equals(target.name) && getAreaByName(replacement.name) != null) {
			return false;
		}
		int index = saveAreas.indexOf(target);
		if (index < 0) {
			return false;
		}
		saveAreas.set(index, replacement);
		if (!saveSaveAreas()) {
			saveAreas.set(index, target);
			return false;
		}
		return true;
	}

	public static boolean isValidArea(AreaSpec area) {
		return area.hasValidName()
				&& area.hasValidCoordinates()
				&& area.volumeSaturated() > 0
				&& area.volumeSaturated() <= settings.maxAreaVolume;
	}

	public static void setSaveDir(Path dir) {
		saveDir = dir;
		// Starting a new save: seed from the global initial area. The per-save value is
		// loaded below and overrides this when rsl_areas.json already has one.
		AreaSpec initial = settings.defaultArea == null
				? AreaSpec.defaultArea()
				: settings.defaultArea.copy().normalized();
		worldDefaultArea = isValidArea(initial) ? initial : AreaSpec.defaultArea();
		worldLoaded = false;
		activeDimension = null;
	}

	public static void loadSaveAreas() {
		if (saveDir == null || codec == null) {
			return;
		}
		worldLoaded = false;
		try {
			ConfigCodec.AreasLoadResult result = codec.loadAreas(saveDir);
			String storedActive = DimensionId.normalize(result.areas.activeDimension);
			if (result.areas.activeDimension != null && storedActive == null) {
				RandomSkyBlockIsland.LOGGER.warn(
						"[RSL] Ignoring invalid active dimension: {}", result.areas.activeDimension);
			}
			activeDimension = storedActive;
			boolean needsSeed = result.areas.defaultArea == null;
			if (!needsSeed) {
				AreaSpec fileDefault = result.areas.defaultArea.normalized();
				if (isValidArea(fileDefault)) {
					worldDefaultArea = fileDefault;
				} else {
					RandomSkyBlockIsland.LOGGER.warn(
							"[RSL] Ignoring invalid per-save default area: {}", fileDefault.display());
					needsSeed = true;
				}
			}

			List<AreaSpec> loadedAreas = new ArrayList<>();
			Set<String> names = new HashSet<>();
			names.add(worldDefaultArea.name);
			for (AreaSpec area : result.areas.areas) {
				area.normalizeInPlace();
				if (!isValidArea(area)) {
					RandomSkyBlockIsland.LOGGER.warn("[RSL] Ignoring invalid save area: {}", area.display());
					continue;
				}
				if (!names.add(area.name)) {
					RandomSkyBlockIsland.LOGGER.warn("[RSL] Ignoring duplicate save area name: {}", area.name);
					continue;
				}
				loadedAreas.add(area);
			}
			saveAreas.clear();
			saveAreas.addAll(loadedAreas);
			if (result.recovered) {
				RandomSkyBlockIsland.LOGGER.warn("[RSL] {}", result.message);
			}

			boolean needsWrite = needsSeed || result.recovered;
			if (!pendingLegacyAreas.isEmpty()) {
				int added = 0;
				for (AreaSpec area : pendingLegacyAreas) {
					area.normalizeInPlace();
					if (!isValidArea(area) || !names.add(area.name)) {
						continue;
					}
					saveAreas.add(area);
					added++;
				}
				pendingLegacyAreas.clear();
				needsWrite |= added > 0;
			}
			if (needsWrite) {
				saveSaveAreas();
			}
			worldLoaded = true;
			RandomSkyBlockIsland.LOGGER.info(
					"[RSL] Loaded {} save area(s); per-save default={}",
					saveAreas.size(), worldDefaultArea.display());
		} catch (IOException e) {
			RandomSkyBlockIsland.LOGGER.warn("[RSL] Failed to load save areas", e);
		}
	}

	/**
	 * Re-reads settings and save areas from disk. Refuses to apply a configuration that would
	 * leave a running game without blocks or without a usable area.
	 */
	public static boolean reload() {
		if (codec == null) {
			return false;
		}
		RslSettings oldSettings = settings;
		List<AreaSpec> oldSaveAreas = new ArrayList<>(saveAreas);
		AreaSpec oldWorldDefault = worldDefaultArea == null ? null : worldDefaultArea.copy();
		String oldActiveDimension = activeDimension;
		try {
			RslSettings fresh = codec.reloadSettings();
			settings = fresh;
			refreshBlockPool();
			if (state.isRunning && allowedBlocks.isEmpty()) {
				restoreAfterFailedReload(oldSettings, oldSaveAreas, oldWorldDefault, oldActiveDimension);
				return false;
			}
			loadSaveAreas();
			if (state.isRunning) {
				// Reloading settings/areas must not silently move a running game to
				// another dimension.
				activeDimension = oldActiveDimension;
			}
			if (state.isRunning && getUsableAreas().isEmpty()) {
				restoreAfterFailedReload(oldSettings, oldSaveAreas, oldWorldDefault, oldActiveDimension);
				return false;
			}
			if (!saveSettings()) {
				restoreAfterFailedReload(oldSettings, oldSaveAreas, oldWorldDefault, oldActiveDimension);
				return false;
			}
			return true;
		} catch (IOException e) {
			RandomSkyBlockIsland.LOGGER.error("[RSL] Failed to reload config", e);
			restoreAfterFailedReload(oldSettings, oldSaveAreas, oldWorldDefault, oldActiveDimension);
			return false;
		}
	}

	private static void restoreAfterFailedReload(
			RslSettings oldSettings, List<AreaSpec> oldSaveAreas, AreaSpec oldWorldDefault,
			String oldActiveDimension) {
		settings = oldSettings;
		worldDefaultArea = oldWorldDefault == null ? AreaSpec.defaultArea() : oldWorldDefault;
		activeDimension = oldActiveDimension;
		saveAreas.clear();
		saveAreas.addAll(oldSaveAreas);
		refreshBlockPool();
	}

	public static boolean saveSaveAreas() {
		if (saveDir == null || codec == null) {
			return false;
		}
		AreasFile file = new AreasFile();
		file.activeDimension = activeDimension;
		file.defaultArea = worldDefaultArea == null ? null : worldDefaultArea.copy();
		file.areas = new ArrayList<>(saveAreas);
		try {
			codec.saveAreas(saveDir, file);
			return true;
		} catch (IOException e) {
			RandomSkyBlockIsland.LOGGER.error("[RSL] Failed to save areas", e);
			return false;
		}
	}

	/**
	 * Resets the current save's default area to the global initial area. Returns false when
	 * no save is loaded or the initial area is invalid.
	 */
	public static boolean resetWorldDefaultToInitial() {
		if (saveDir == null || codec == null || settings.defaultArea == null) {
			return false;
		}
		AreaSpec initial = settings.defaultArea.normalized();
		if (!isValidArea(initial)) {
			return false;
		}
		AreaSpec previous = worldDefaultArea;
		worldDefaultArea = initial;
		if (!saveSaveAreas()) {
			worldDefaultArea = previous;
			return false;
		}
		return true;
	}
}
