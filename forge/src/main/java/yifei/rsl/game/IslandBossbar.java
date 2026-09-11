package yifei.rsl.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss 条管理类
 *
 * 移植说明：
 * - CommandBossBar → ServerBossEvent（Forge 中 Boss 条管理方式不同）
 * - Fabric 版通过 BossBarManager 注册自定义 Boss 条，Forge 版直接创建 ServerBossEvent 实例
 * - Text → Component
 * - Identifier → ResourceLocation
 * - ServerPlayerEntity → ServerPlayer
 *
 * 线程安全说明：使用并发集合防止登出事件与主线程 tick 的竞态，同时避免玩家登出后仍保留引用导致内存泄漏。
 */
public class IslandBossbar {
    private ServerBossEvent bossbar;
    // 使用 ConcurrentHashMap 支撑的 Set 支持并发遍历与修改，避免玩家集合内存泄漏
    private final Set<ServerPlayer> players = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void createBossbar() {
        if (bossbar != null) {
            removeBossbar();
        }

        bossbar = new ServerBossEvent(
                Component.translatable("rsl.bossbar.title", 0),
                net.minecraft.world.BossEvent.BossBarColor.BLUE,
                net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS
        );
        bossbar.setCreateWorldFog(false);
        bossbar.setDarkenScreen(false);
    }

    public void removeBossbar() {
        if (bossbar != null) {
            for (ServerPlayer player : players) {
                bossbar.removePlayer(player);
            }
            players.clear();
            bossbar = null;
        }
    }

    public void update(int secondsRemaining, int totalSeconds) {
        if (bossbar == null) {
            return;
        }

        bossbar.setName(Component.translatable("rsl.bossbar.title", secondsRemaining));
        float progress = (float) secondsRemaining / totalSeconds;
        bossbar.setProgress(progress);
    }

    public void addPlayer(ServerPlayer player) {
        if (bossbar != null && !players.contains(player)) {
            bossbar.addPlayer(player);
            players.add(player);
        }
    }

    public void removePlayer(ServerPlayer player) {
        if (bossbar != null && players.contains(player)) {
            bossbar.removePlayer(player);
            players.remove(player);
        }
    }

    public void addAllPlayers(java.util.Collection<ServerPlayer> playerList) {
        for (ServerPlayer player : playerList) {
            addPlayer(player);
        }
    }
}
