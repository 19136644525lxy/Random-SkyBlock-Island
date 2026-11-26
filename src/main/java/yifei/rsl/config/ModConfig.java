package yifei.rsl.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.common.block.BlockId;
import yifei.rsl.common.block.DefaultBlocks;
import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.common.config.AreasFile;
import yifei.rsl.common.config.BlockMutationResult;
import yifei.rsl.common.config.ConfigCodec;
import yifei.rsl.common.config.DimensionId;
import yifei.rsl.common.config.PoolAccess;
import yifei.rsl.common.config.PoolAccessEntry;
import yifei.rsl.common.config.PoolEntry;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.common.config.RslSettings;
import yifei.rsl.common.config.RslState;
import yifei.rsl.game.BlockStateCodec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
	private static final List<PoolEntry> nbtPoolEntries = new ArrayList<>();
	private static final List<String> invalidBlockIds = new ArrayList<>();
	private static List<AreaSpec> pendingLegacyAreas = new ArrayList<>();
	private static Path saveDir;
	private static boolean loaded;

	private ModConfig() {
	}

	public static void loadConfig() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
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
			refreshEggPool();
			refreshNbtPool();
			loaded = true;
			RandomSkyBlockIsland.LOGGER.info(
					"[RSL] Loaded config: delay={}s, blocks={} (invalid={}), poolEditors={}",
					settings.transformDelaySeconds, allowedBlocks.size(), invalidBlockIds.size(),
					settings.poolAccess == null ? 0 : settings.poolAccess.size());
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
			refreshEggPool();
			refreshNbtPool();
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

	public static boolean isSoundEnabled() {
		return settings.soundEnabled;
	}

	public static void setSoundEnabled(boolean enabled) {
		settings.soundEnabled = enabled;
		saveSettings();
	}

	public static double getSoundVolume() {
		return settings.soundVolume;
	}

	public static void setSoundVolume(int percent) {
		settings.soundVolume = Math.max(0, Math.min(100, percent)) / 100.0;
		saveSettings();
	}

	public static int getGamePermissionLevel() {
		return GAME_PERMISSION_LEVEL;
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

	public static boolean isActiveDimension(ServerWorld world) {
		return world != null && activeDimension != null
				&& world.getRegistryKey().getValue().toString().equals(activeDimension);
	}

	/**
	 * Resolves the locked dimension to a loaded server world, or {@code null} when the
	 * dimension is not loaded / not registered.
	 */
	public static ServerWorld findActiveWorld(MinecraftServer server) {
		if (server == null || activeDimension == null) {
			return null;
		}
		Identifier id = Identifier.tryParse(activeDimension);
		if (id == null) {
			return null;
		}
		RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, id);
		return server.getWorld(key);
	}

	/**
	 * @return true when the player is in the locked dimension or no game is locked
	 */
	public static boolean isPlayerInActiveDimension(ServerPlayerEntity player) {
		if (activeDimension == null || player == null) {
			return true;
		}
		return player.getServerWorld().getRegistryKey().getValue().toString().equals(activeDimension);
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

	// ========== 方块池访问权限 ==========

	public static List<PoolAccessEntry> getPoolAccess() {
		List<PoolAccessEntry> copy = new ArrayList<>();
		if (settings.poolAccess != null) {
			for (PoolAccessEntry entry : settings.poolAccess) {
				if (entry != null) {
					copy.add(entry.copy());
				}
			}
		}
		return copy;
	}

	/**
	 * Grants EDITOR or OWNER access and persists it. Returns false for a no-op or a
	 * failed save; callers should inspect the role before calling to distinguish
	 * "already granted" from an I/O failure.
	 */
	public static boolean grantPoolAccess(ServerPlayerEntity target, boolean owner) {
		if (target == null) {
			return false;
		}
		List<PoolAccessEntry> previous = getPoolAccess();
		List<PoolAccessEntry> working = getPoolAccess();
		if (!PoolAccess.grant(working, target.getUuid(),
				target.getGameProfile().getName(), owner)) {
			return false;
		}
		settings.poolAccess = working;
		if (!saveSettings()) {
			settings.poolAccess = previous;
			return false;
		}
		return true;
	}

	public static boolean revokePoolAccess(UUID target) {
		if (target == null) {
			return false;
		}
		List<PoolAccessEntry> previous = getPoolAccess();
		List<PoolAccessEntry> working = getPoolAccess();
		if (!PoolAccess.revoke(working, target)) {
			return false;
		}
		settings.poolAccess = working;
		if (!saveSettings()) {
			settings.poolAccess = previous;
			return false;
		}
		return true;
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
		return allowedBlocks.isEmpty() && nbtPoolEntries.isEmpty();
	}

	public static BlockMutationResult addBlockId(String raw) {
		String id = BlockId.normalize(raw);
		if (id == null) {
			return BlockMutationResult.INVALID_ID;
		}
		if (allowedBlockIds.contains(id)) {
			return BlockMutationResult.DUPLICATE;
		}
		Identifier identifier = Identifier.tryParse(id);
		if (identifier == null) {
			return BlockMutationResult.INVALID_ID;
		}
		Optional<Block> optional = Registries.BLOCK.getOrEmpty(identifier);
		if (optional.isEmpty()) {
			return BlockMutationResult.UNKNOWN_BLOCK;
		}
		if (optional.get() == Blocks.AIR) {
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
		if (state.isRunning && allowedBlocks.isEmpty() && nbtPoolEntries.isEmpty()) {
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
		return applyBlockMutations(add, remove, List.of());
	}

	/**
	 * Same as {@link #applyBlockMutations(List, List)} but also removes container
	 * entries by their {@link PoolEntry#key()}.
	 */
	public static PoolMutationResult applyBlockMutations(List<String> add, List<String> remove,
														 List<String> removeNbtKeys) {
		List<String> previous = new ArrayList<>(settings.allowedBlocks);
		List<PoolEntry> previousNbt = copyEntries(settings.nbtPool);
		List<String> working = new ArrayList<>(settings.allowedBlocks);
		List<PoolEntry> workingNbt = copyEntries(settings.nbtPool);
		List<String> failed = new ArrayList<>();
		BlockMutationResult firstError = null;

		if (removeNbtKeys != null) {
			for (String key : removeNbtKeys) {
				if (key == null) {
					continue;
				}
				workingNbt.removeIf(entry -> entry != null && entry.key().equals(key));
			}
		}

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
				Identifier identifier = Identifier.tryParse(id);
				if (identifier == null) {
					failed.add(raw);
					if (firstError == null) firstError = BlockMutationResult.INVALID_ID;
					continue;
				}
				Optional<Block> optional = Registries.BLOCK.getOrEmpty(identifier);
				if (optional.isEmpty()) {
					failed.add(raw);
					if (firstError == null) firstError = BlockMutationResult.UNKNOWN_BLOCK;
					continue;
				}
				if (optional.get() == Blocks.AIR) {
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
		settings.nbtPool = workingNbt;
		refreshBlockPool();
		refreshNbtPool();
		if (state.isRunning && allowedBlocks.isEmpty() && nbtPoolEntries.isEmpty()) {
			settings.allowedBlocks = previous;
			settings.nbtPool = previousNbt;
			refreshBlockPool();
			refreshNbtPool();
			return PoolMutationResult.fail(BlockMutationResult.WOULD_EMPTY_RUNNING,
					"rsl.message.blocks_would_empty", failed);
		}
		if (!saveSettings()) {
			settings.allowedBlocks = previous;
			settings.nbtPool = previousNbt;
			refreshBlockPool();
			refreshNbtPool();
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
			Identifier identifier = Identifier.tryParse(id);
			if (identifier == null) {
				invalidBlockIds.add(id);
				continue;
			}
			Optional<Block> optional = Registries.BLOCK.getOrEmpty(identifier);
			if (optional.isEmpty() || optional.get() == Blocks.AIR) {
				invalidBlockIds.add(id);
				continue;
			}
			if (seenBlocks.add(optional.get())) {
				allowedBlocks.add(optional.get());
			}
		}
		if (!invalidBlockIds.isEmpty()) {
			RandomSkyBlockIsland.LOGGER.warn("[RSL] {} configured block id(s) are unavailable: {}",
					invalidBlockIds.size(), invalidBlockIds);
		}
	}

	// ========== NBT 容器池（正常池的一部分） ==========

	public static List<PoolEntry> getNbtPoolEntries() {
		return List.copyOf(nbtPoolEntries);
	}

	public static int getNbtPoolSize() {
		return nbtPoolEntries.size();
	}

	/**
	 * Adds a container entry (block state + captured block-entity NBT) to the
	 * normal pool.
	 */
	public static BlockMutationResult addNbtPoolEntry(PoolEntry raw) {
		if (raw == null) {
			return BlockMutationResult.INVALID_ID;
		}
		String id = BlockId.normalize(raw.id);
		if (id == null) {
			return BlockMutationResult.INVALID_ID;
		}
		Identifier identifier = Identifier.tryParse(id);
		if (identifier == null) {
			return BlockMutationResult.INVALID_ID;
		}
		Optional<Block> optional = Registries.BLOCK.getOrEmpty(identifier);
		if (optional.isEmpty()) {
			return BlockMutationResult.UNKNOWN_BLOCK;
		}
		Block block = optional.get();
		if (block == Blocks.AIR) {
			return BlockMutationResult.AIR_BLOCK;
		}
		net.minecraft.block.BlockState state = block.getDefaultState();
		if (raw.state != null && !raw.state.isEmpty()) {
			net.minecraft.block.BlockState decoded = BlockStateCodec.decode(raw.state);
			if (decoded != null && decoded.getBlock() == block) {
				state = decoded;
			}
		}
		String nbt = raw.nbt != null && PoolEntry.isValidBase64(raw.nbt) ? raw.nbt.trim() : "";
		PoolEntry entry = new PoolEntry(id, BlockStateCodec.encode(state), nbt);
		if (settings.allowedBlocks.size() + settings.nbtPool.size() >= MAX_POOL_SIZE) {
			return BlockMutationResult.POOL_FULL;
		}
		for (PoolEntry existing : settings.nbtPool) {
			if (existing != null && existing.key().equals(entry.key())) {
				return BlockMutationResult.DUPLICATE;
			}
		}
		settings.nbtPool.add(entry.copy());
		refreshNbtPool();
		if (!saveSettings()) {
			settings.nbtPool.remove(settings.nbtPool.size() - 1);
			refreshNbtPool();
			return BlockMutationResult.IO_ERROR;
		}
		return BlockMutationResult.OK;
	}

	public static boolean removeNbtPoolKey(String key) {
		List<PoolEntry> previous = copyEntries(settings.nbtPool);
		if (key == null) {
			return false;
		}
		if (!settings.nbtPool.removeIf(entry -> entry != null && entry.key().equals(key))) {
			return false;
		}
		refreshNbtPool();
		if (!saveSettings()) {
			settings.nbtPool = previous;
			refreshNbtPool();
			return false;
		}
		return true;
	}

	private static void refreshNbtPool() {
		nbtPoolEntries.clear();
		Set<String> seen = new HashSet<>();
		for (PoolEntry raw : settings.nbtPool) {
			if (raw == null) {
				continue;
			}
			String id = BlockId.normalize(raw.id);
			if (id == null) {
				continue;
			}
			Identifier identifier = Identifier.tryParse(id);
			if (identifier == null) {
				continue;
			}
			Optional<Block> optional = Registries.BLOCK.getOrEmpty(identifier);
			if (optional.isEmpty() || optional.get() == Blocks.AIR) {
				continue;
			}
			net.minecraft.block.BlockState state = optional.get().getDefaultState();
			if (raw.state != null && !raw.state.isEmpty()) {
				net.minecraft.block.BlockState decoded = BlockStateCodec.decode(raw.state);
				if (decoded != null && decoded.getBlock() == optional.get()) {
					state = decoded;
				}
			}
			String nbt = raw.nbt != null && PoolEntry.isValidBase64(raw.nbt) ? raw.nbt.trim() : "";
			PoolEntry entry = new PoolEntry(id, BlockStateCodec.encode(state), nbt);
			if (seen.add(entry.key())) {
				nbtPoolEntries.add(entry);
			}
		}
	}

	private static List<PoolEntry> copyEntries(List<PoolEntry> source) {
		List<PoolEntry> copy = new ArrayList<>();
		if (source != null) {
			for (PoolEntry entry : source) {
				if (entry != null) {
					copy.add(entry.copy());
				}
			}
		}
		return copy;
	}

	// ========== 彩蛋模式 ==========

	private static final List<String> eggPoolIds = new ArrayList<>();
	private static final List<Block> eggPoolBlocks = new ArrayList<>();

	public static boolean isEggModeEnabled() {
		return settings.eggModeEnabled;
	}

	/** Toggles the egg mode and persists it; rolls back on I/O failure. */
	public static boolean setEggModeEnabled(boolean enabled) {
		boolean previous = settings.eggModeEnabled;
		settings.eggModeEnabled = enabled;
		if (!saveSettings()) {
			settings.eggModeEnabled = previous;
			return false;
		}
		return true;
	}

	public static boolean toggleEggMode() {
		return setEggModeEnabled(!settings.eggModeEnabled);
	}

	public static double getEggChance() {
		return settings.eggChance;
	}

	public static boolean setEggChance(double chance) {
		double previous = settings.eggChance;
		settings.eggChance = Math.max(0.01, Math.min(1.0, chance));
		if (!saveSettings()) {
			settings.eggChance = previous;
			return false;
		}
		return true;
	}

	public static int getEggMinRadius() {
		return settings.eggMinRadius;
	}

	public static int getEggMaxRadius() {
		return settings.eggMaxRadius;
	}

	public static boolean setEggRadius(int min, int max) {
		int previousMin = settings.eggMinRadius;
		int previousMax = settings.eggMaxRadius;
		int clampedMin = Math.max(1, Math.min(256, min));
		int clampedMax = Math.max(clampedMin, Math.min(512, max));
		settings.eggMinRadius = clampedMin;
		settings.eggMaxRadius = clampedMax;
		if (!saveSettings()) {
			settings.eggMinRadius = previousMin;
			settings.eggMaxRadius = previousMax;
			return false;
		}
		return true;
	}

	public static int getEggVolume() {
		return settings.eggVolume;
	}

	public static boolean setEggVolume(int volume) {
		int previous = settings.eggVolume;
		settings.eggVolume = Math.max(1, Math.min(4096, volume));
		if (!saveSettings()) {
			settings.eggVolume = previous;
			return false;
		}
		return true;
	}

	public static int getEggMinDistance() {
		return settings.eggMinDistance;
	}

	public static boolean setEggMinDistance(int distance) {
		int previous = settings.eggMinDistance;
		settings.eggMinDistance = Math.max(1, Math.min(256, distance));
		if (!saveSettings()) {
			settings.eggMinDistance = previous;
			return false;
		}
		return true;
	}

	public static List<String> getEggPoolIds() {
		return List.copyOf(eggPoolIds);
	}

	public static List<Block> getEggPoolBlocks() {
		return List.copyOf(eggPoolBlocks);
	}

	public static boolean isEggPoolEmpty() {
		return eggPoolBlocks.isEmpty();
	}

	/**
	 * Replaces the egg pool with the given ids. Invalid/unknown ids are dropped;
	 * the caller is expected to have filtered them already for a UI-driven flow.
	 */
	public static boolean setEggPool(List<String> ids) {
		List<String> previous = new ArrayList<>(settings.eggPool);
		List<String> normalized = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		if (ids != null) {
			for (String raw : ids) {
				String id = BlockId.normalize(raw);
				if (id == null || !seen.add(id)) {
					continue;
				}
				Identifier identifier = Identifier.tryParse(id);
				if (identifier == null) {
					continue;
				}
				Optional<Block> optional = Registries.BLOCK.getOrEmpty(identifier);
				if (optional.isEmpty() || optional.get() == Blocks.AIR) {
					continue;
				}
				normalized.add(id);
			}
		}
		settings.eggPool = normalized;
		refreshEggPool();
		if (!saveSettings()) {
			settings.eggPool = previous;
			refreshEggPool();
			return false;
		}
		return true;
	}

	private static void refreshEggPool() {
		eggPoolIds.clear();
		eggPoolBlocks.clear();
		Set<String> seenIds = new HashSet<>();
		Set<Block> seenBlocks = new HashSet<>();
		for (String raw : settings.eggPool) {
			String id = BlockId.normalize(raw);
			if (id == null || !seenIds.add(id)) {
				continue;
			}
			Identifier identifier = Identifier.tryParse(id);
			if (identifier == null) {
				continue;
			}
			Optional<Block> optional = Registries.BLOCK.getOrEmpty(identifier);
			if (optional.isEmpty() || optional.get() == Blocks.AIR) {
				continue;
			}
			eggPoolIds.add(id);
			if (seenBlocks.add(optional.get())) {
				eggPoolBlocks.add(optional.get());
			}
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
