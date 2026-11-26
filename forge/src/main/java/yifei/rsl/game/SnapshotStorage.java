package yifei.rsl.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.common.snapshot.RegionSnapshot;
import yifei.rsl.common.snapshot.SnapshotArea;
import yifei.rsl.common.snapshot.SnapshotCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Takes the pre-start block snapshot of every usable area and reads it back for
 * {@code reset}. The file lives next to {@code rsl_areas.json} so it follows the
 * save file and is never shared between worlds.
 */
public final class SnapshotStorage {
	public static final String FILE_NAME = "rsl_snapshot.bin";

	private SnapshotStorage() {
	}

	public static Path path(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
	}

	public static boolean exists(MinecraftServer server) {
		return Files.isRegularFile(path(server));
	}

	/**
	 * Records the snapshot only when none exists yet, so {@code reset} always
	 * returns to the state before the first start.
	 */
	public static boolean saveIfAbsent(MinecraftServer server, ServerLevel world, List<AreaSpec> areas) {
		Path file = path(server);
		if (Files.exists(file)) {
			return false;
		}
		RegionSnapshot snapshot = capture(world, areas);
		if (snapshot.areas().isEmpty()) {
			return false;
		}
		try {
			SnapshotCodec.save(file, snapshot);
			RandomSkyBlockIsland.LOGGER.info(
					"Saved the pre-start block snapshot ({} areas) to {}", snapshot.areas().size(), file);
			return true;
		} catch (IOException exception) {
			RandomSkyBlockIsland.LOGGER.warn("Could not save the pre-start block snapshot", exception);
			return false;
		}
	}

	public static RegionSnapshot load(MinecraftServer server) {
		Path file = path(server);
		if (!Files.isRegularFile(file)) {
			return null;
		}
		try {
			return SnapshotCodec.load(file);
		} catch (IOException exception) {
			RandomSkyBlockIsland.LOGGER.warn("Could not read the block snapshot {}", file, exception);
			return null;
		}
	}

	private static RegionSnapshot capture(ServerLevel world, List<AreaSpec> areas) {
		String dimension = world.dimension().location().toString();
		int worldBottom = world.getMinBuildHeight();
		int worldTop = world.getMaxBuildHeight();
		List<SnapshotArea> captured = new ArrayList<>();
		for (AreaSpec raw : areas) {
			AreaSpec area = raw.normalized();
			int minX = area.minX;
			int minY = Math.max(area.minY, worldBottom);
			int minZ = area.minZ;
			int maxX = area.maxX;
			int maxY = Math.min(area.maxY, worldTop);
			int maxZ = area.maxZ;
			if (minY > maxY) {
				continue;
			}
			int sizeX = maxX - minX + 1;
			int sizeY = maxY - minY + 1;
			int sizeZ = maxZ - minZ + 1;
			List<String> palette = new ArrayList<>();
			Map<String, Integer> lookup = new HashMap<>();
			int[] indices = new int[sizeX * sizeY * sizeZ];
			int index = 0;
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {
						BlockState state = world.getBlockState(new BlockPos(x, y, z));
						String encoded = BlockStateCodec.encode(state);
						Integer paletteIndex = lookup.get(encoded);
						if (paletteIndex == null) {
							paletteIndex = palette.size();
							palette.add(encoded);
							lookup.put(encoded, paletteIndex);
						}
						indices[index++] = paletteIndex;
					}
				}
			}
			captured.add(new SnapshotArea(area.name, minX, minY, minZ, sizeX, sizeY, sizeZ, palette, indices));
		}
		return new RegionSnapshot(dimension, captured);
	}
}
