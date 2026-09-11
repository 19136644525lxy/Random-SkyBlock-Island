package yifei.rsl;

import yifei.rsl.commands.StartCommand;
import yifei.rsl.commands.StopCommand;
import yifei.rsl.commands.AreaCommand;
import yifei.rsl.commands.DelayCommand;
import yifei.rsl.commands.ResetCommand;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 随机空岛模组主类
 *
 * 移植说明：Fabric 使用 ModInitializer 入口点 + 各类 Callback 事件注册，
 * Forge 使用 @Mod 注解 + EventBus 事件订阅机制。
 */
@Mod(RandomSkyBlockIsland.MOD_ID)
public class RandomSkyBlockIsland {
    public static final String MOD_ID = "rsl";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static IslandGame islandGame;

    public RandomSkyBlockIsland(FMLJavaModLoadingContext context) {
        // 注册到 FORGE 事件总线（用于服务端事件：Tick、命令注册、玩家登录、服务器停止）
        MinecraftForge.EVENT_BUS.register(this);

        // 初始化配置（保留原 JSON 配置系统，与 Fabric 版格式完全一致）
        ModConfig.loadConfig();

        // 注册配置界面工厂到 Forge ConfigScreenHandler
        // Catalogue（Forge 版 ModMenu）会通过此扩展点识别并显示"配置"按钮
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {
                    // 委托给 Cloth Config 构建的配置界面
                    return yifei.rsl.config.ModConfigScreen.create(parent);
                })
        );
    }

    /**
     * 命令注册事件 - 替代 Fabric 的 CommandRegistrationCallback
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        StartCommand.register(event.getDispatcher());
        StopCommand.register(event.getDispatcher());
        AreaCommand.register(event.getDispatcher());
        DelayCommand.register(event.getDispatcher());
        ResetCommand.register(event.getDispatcher());
    }

    /**
     * 服务端 Tick 事件 - 替代 Fabric 的 ServerTickEvents.END_SERVER_TICK
     * 使用 END 阶段以保持与 Fabric 行为一致
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (islandGame == null) {
            islandGame = new IslandGame(server);
        }
        islandGame.tick();
    }

    /**
     * 玩家登录事件 - 替代 Fabric 的 ServerPlayConnectionEvents.JOIN
     * 用于在游戏运行时为新加入的玩家添加 Boss 条
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (islandGame != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            if (islandGame.isRunning()) {
                islandGame.getBossbar().addPlayer(player);
            }
        }
    }

    /**
     * 玩家登出事件 - 替代 Fabric 的 ServerPlayConnectionEvents.DISCONNECT
     * 用于从 Boss 条集合中移除玩家引用，防止内存泄漏
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (islandGame != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            islandGame.onPlayerLogout(player);
        }
    }

    /**
     * 服务器停止事件 - 替代 Fabric 的 ServerLifecycleEvents.SERVER_STOPPING
     * 用于保存游戏运行状态到配置文件
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (islandGame != null) {
            ModConfig.isRunning = islandGame.isRunning();
            if (islandGame.isRunning()) {
                ModConfig.secondsRemaining = islandGame.getSecondsRemaining();
            } else {
                ModConfig.secondsRemaining = 0;
            }
            ModConfig.saveConfig();
            // 同步保存存档级区域配置
            ModConfig.saveSaveAreas();
        }
    }

    public static IslandGame getIslandGame() {
        return islandGame;
    }
}
