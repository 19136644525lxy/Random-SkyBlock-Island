package yifei.rsl.game;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.config.TransformArea;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;
import net.minecraft.world.level.storage.LevelResource;

/**
 * 空岛游戏核心逻辑
 *
 * 移植说明：
 * - ServerPlayerEntity → ServerPlayer
 * - PlayerManager → PlayerList
 * - Text.translatable → Component.translatable
 * - World.OVERWORLD → Level.OVERWORLD
 * - server.getWorld → server.getLevel
 * - sendMessage(text, false) → sendSystemMessage(text)
 */
public class IslandGame {
    private final MinecraftServer server;
    private final Random random;
    private final IslandBossbar bossbar;

    private boolean isRunning = false;
    private int tickCount = 0;
    private int secondsRemaining = 0;

    public IslandGame(MinecraftServer server) {
        this.server = server;
        this.random = new Random();
        this.bossbar = new IslandBossbar();

        // 设置存档目录并加载存档区域
        ModConfig.setSaveDir(server.getWorldPath(LevelResource.ROOT));
        ModConfig.loadSaveAreas();

        // 从配置恢复运行状态
        if (ModConfig.isRunning) {
            this.isRunning = true;
            this.secondsRemaining = ModConfig.secondsRemaining;
            this.tickCount = 0;
            this.bossbar.createBossbar();
            updateBossbar();
        }
    }

    public void tick() {
        if (!isRunning) {
            return;
        }

        tickCount++;

        if (tickCount >= 20) {
            tickCount = 0;
            secondsRemaining--;
            updateBossbar();

            if (secondsRemaining <= 0) {
                transformIsland();
            } else {
                ModConfig.secondsRemaining = secondsRemaining;
                ModConfig.saveConfig();
            }
        }
    }

    public void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        tickCount = 0;
        secondsRemaining = ModConfig.transformDelaySeconds;

        bossbar.createBossbar();
        updateBossbar();

        ModConfig.isRunning = true;
        ModConfig.secondsRemaining = secondsRemaining;
        ModConfig.saveConfig();

        RandomSkyBlockIsland.LOGGER.info("Random SkyBlock Island game started!");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.translatable("rsl.message.started", ModConfig.transformDelaySeconds));
        }
    }

    public void stop() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        bossbar.removeBossbar();

        ModConfig.isRunning = false;
        ModConfig.secondsRemaining = 0;
        ModConfig.saveConfig();

        RandomSkyBlockIsland.LOGGER.info("Random SkyBlock Island game stopped!");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.translatable("rsl.message.stopped"));
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void transformIsland() {
        Level world = server.getLevel(Level.OVERWORLD);
        if (world == null) {
            RandomSkyBlockIsland.LOGGER.warn("Overworld not found!");
            return;
        }

        StringBuilder blockNames = new StringBuilder();

        for (TransformArea area : ModConfig.getAllAreas()) {
            int blockIndex = random.nextInt(ModConfig.allowedBlocks.size());
            var selectedBlock = ModConfig.allowedBlocks.get(blockIndex);

            BlockReplacer.fillArea(world,
                area.minX, area.minY, area.minZ,
                area.maxX, area.maxY, area.maxZ,
                selectedBlock.defaultBlockState()
            );

            if (blockNames.length() > 0) {
                blockNames.append(", ");
            }
            blockNames.append(area.name).append("=").append(selectedBlock.getName().getString());

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.translatable("rsl.message.changed.area", area.name, selectedBlock.getName()));
            }
        }

        secondsRemaining = ModConfig.transformDelaySeconds;
        updateBossbar();

        ModConfig.secondsRemaining = secondsRemaining;
        ModConfig.saveConfig();

        RandomSkyBlockIsland.LOGGER.info("Island transformed: {}", blockNames);
    }

    private void updateBossbar() {
        if (isRunning) {
            bossbar.update(secondsRemaining, ModConfig.transformDelaySeconds);
        }
    }

    public void updateDelay() {
        if (isRunning) {
            secondsRemaining = Math.min(secondsRemaining, ModConfig.transformDelaySeconds);
        }
    }

    /**
     * 玩家登出时调用，从 Bossbar 中移除，避免引用 ServerPlayer 导致内存泄漏
     */
    public void onPlayerLogout(ServerPlayer player) {
        bossbar.removePlayer(player);
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    public IslandBossbar getBossbar() {
        return bossbar;
    }
}
