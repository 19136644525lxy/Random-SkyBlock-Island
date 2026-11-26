package yifei.rsl.game;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.common.config.RslState;
import yifei.rsl.common.egg.EggIsland;
import yifei.rsl.config.ModConfig;

import java.util.List;
import java.util.Random;

/**
 * Spawns and clears the hidden "egg" island. The island is a solid cube drawn
 * from the egg pool, placed near an online player in the active dimension. Its
 * position is remembered in the state file so the next transform can clear it
 * even after a restart.
 */
public final class EggIslandSpawner {
    private static final int MAX_ATTEMPTS = 32;
    private static final int CLEARANCE_ABOVE = 8;
    private static final int VERTICAL_SEARCH = 24;

    private final MinecraftServer server;
    private final Random random = new Random();

    public EggIslandSpawner(MinecraftServer server) {
        this.server = server;
    }

    /** Clears the previously spawned cube in whichever loaded dimension it lives. */
    public void clearRecordedIsland() {
        RslState state = ModConfig.getState();
        if (!state.eggIslandPresent || state.eggIslandSize <= 0) {
            return;
        }
        String dimension = state.eggIslandDimension;
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(dimension)) {
                clearCube(world, state);
                return;
            }
        }
        // The old dimension is not loaded; keep the record so it is cleared next time
        // that dimension becomes active again.
    }

    private void clearCube(ServerWorld world, RslState state) {
        int size = Math.min(state.eggIslandSize, EggIsland.MAX_EDGE);
        int minX = state.eggIslandMinX;
        int minY = state.eggIslandMinY;
        int minZ = state.eggIslandMinZ;
        String dimension = state.eggIslandDimension;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    world.setBlockState(new BlockPos(minX + x, minY + y, minZ + z),
                            Blocks.AIR.getDefaultState(), 3);
                }
            }
        }
        state.clearEggIsland();
        ModConfig.saveState();
        RandomSkyBlockIsland.LOGGER.info(
                "Cleared the previous egg island in {} at ({},{},{})",
                dimension, minX, minY, minZ);
    }

    /** Tries to place a new egg island near a random online player of the world. */
    public boolean trySpawn(ServerWorld world) {
        List<Block> pool = ModConfig.getEggPoolBlocks();
        if (pool.isEmpty()) {
            return false;
        }
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList().stream()
                .filter(player -> player.getWorld() == world)
                .toList();
        if (players.isEmpty()) {
            return false;
        }
        ServerPlayerEntity anchor = players.get(this.random.nextInt(players.size()));
        Block chosen = pool.get(this.random.nextInt(pool.size()));

        int edge = EggIsland.edgeLength(ModConfig.getEggVolume());
        int bottom = world.getBottomY();
        int top = world.getTopY();
        int minRadius = ModConfig.getEggMinRadius();
        int maxRadius = Math.max(minRadius, ModConfig.getEggMaxRadius());
        int anchorY = Math.max(bottom, Math.min(top - edge - CLEARANCE_ABOVE, anchor.getBlockY()));
        int maxY = Math.min(top - edge - CLEARANCE_ABOVE, anchorY + VERTICAL_SEARCH);
        if (maxY < anchorY) {
            return false;
        }

        List<yifei.rsl.common.config.AreaSpec> areas = ModConfig.getAllAreas();
        int minDistance = ModConfig.getEggMinDistance();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = minRadius + this.random.nextDouble() * (maxRadius - minRadius);
            int minX = anchor.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int minZ = anchor.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            int minY = anchorY + this.random.nextInt(maxY - anchorY + 1);
            int maxX = minX + edge - 1;
            int maxYBox = minY + edge - 1;
            int maxZ = minZ + edge - 1;

            if (!EggIsland.insideWorldHeight(minY, edge, bottom, top)) {
                continue;
            }
            if (EggIsland.anyAreaTooClose(minX, minY, minZ, maxX, maxYBox, maxZ, areas, minDistance)) {
                continue;
            }
            if (!isCubeClear(world, minX, minY, minZ, edge)) {
                continue;
            }
            if (!hasHeadroom(world, minX, minY + edge, minZ, edge, CLEARANCE_ABOVE)) {
                continue;
            }

            for (int x = 0; x < edge; x++) {
                for (int y = 0; y < edge; y++) {
                    for (int z = 0; z < edge; z++) {
                        world.setBlockState(new BlockPos(minX + x, minY + y, minZ + z),
                                chosen.getDefaultState(), 3);
                    }
                }
            }
            ModConfig.getState().setEggIsland(
                    world.getRegistryKey().getValue().toString(), minX, minY, minZ, edge);
            ModConfig.saveState();
            RandomSkyBlockIsland.LOGGER.info(
                    "Spawned an egg island ({}) at ({},{},{}) in {}",
                    edge, minX, minY, minZ, world.getRegistryKey().getValue());
            int centerX = minX + edge / 2;
            int centerY = minY + edge / 2;
            int centerZ = minZ + edge / 2;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(
                        Text.translatable("rsl.message.egg_spawned", centerX, centerY, centerZ), false);
                SoundFx.play(player, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.5F, 1.2F);
            }
            return true;
        }
        return false;
    }

    private boolean isCubeClear(ServerWorld world, int minX, int minY, int minZ, int edge) {
        for (int x = 0; x < edge; x++) {
            for (int y = 0; y < edge; y++) {
                for (int z = 0; z < edge; z++) {
                    if (!world.isAir(new BlockPos(minX + x, minY + y, minZ + z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean hasHeadroom(ServerWorld world, int minX, int minY, int minZ, int edge, int clearance) {
        for (int x = 0; x < edge; x++) {
            for (int z = 0; z < edge; z++) {
                for (int y = 0; y < clearance; y++) {
                    if (!world.isAir(new BlockPos(minX + x, minY + y, minZ + z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
