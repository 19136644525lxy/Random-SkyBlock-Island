package yifei.rsl;

import yifei.rsl.commands.AccessCommand;
import yifei.rsl.commands.EggCommand;
import yifei.rsl.commands.StartCommand;
import yifei.rsl.commands.StopCommand;
import yifei.rsl.commands.AreaCommand;
import yifei.rsl.commands.BlocksCommand;
import yifei.rsl.commands.DelayCommand;
import yifei.rsl.commands.ReloadCommand;
import yifei.rsl.commands.ResetCommand;
import yifei.rsl.commands.StatusCommand;
import yifei.rsl.commands.WandCommand;
import yifei.rsl.commands.SoundCommand;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import yifei.rsl.item.SelectionWandItem;
import yifei.rsl.item.WandSelectionManager;
import yifei.rsl.network.AccessNetwork;
import yifei.rsl.network.BlockPoolNetwork;
import yifei.rsl.network.CaptureStickNetwork;
import yifei.rsl.network.EggNetwork;
import yifei.rsl.network.GameplayActionNetwork;
import yifei.rsl.network.GameplayNetwork;
import yifei.rsl.network.WandNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
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
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> SELECTION_WAND =
            ITEMS.register("selection_wand", () -> new SelectionWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAPTURE_STICK =
            ITEMS.register("capture_stick", () -> new yifei.rsl.item.CaptureStickItem(new Item.Properties().stacksTo(1)));

    private static IslandGame islandGame;

    public RandomSkyBlockIsland(FMLJavaModLoadingContext context) {
        // 注册到 FORGE 事件总线（用于服务端事件：Tick、命令注册、玩家登录、服务器停止）
        MinecraftForge.EVENT_BUS.register(this);

        // 注册物品与网络
        ITEMS.register(context.getModEventBus());
        WandNetwork.register();
        BlockPoolNetwork.register();
        GameplayNetwork.register();
        GameplayActionNetwork.register();
        AccessNetwork.register();
        EggNetwork.register();
        CaptureStickNetwork.register();

        // 初始化配置（保留原 JSON 配置系统，与 Fabric 版格式完全一致）
        ModConfig.loadConfig();

        // 注册配置界面工厂到 Forge ConfigScreenHandler。
        // Catalogue（Forge 版 ModMenu）会通过此扩展点识别并显示"配置"按钮。
        // Cloth Config 为可选依赖：DistExecutor 确保客户端类不会在服务端被加载。
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> yifei.rsl.config.ClientConfigRegistration.register(context));
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> yifei.rsl.client.ClientKeybinds.register(context.getModEventBus()));
    }

    /**
     * 命令注册事件 - 替代 Fabric 的 CommandRegistrationCallback
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        StartCommand.register(event.getDispatcher());
        StopCommand.register(event.getDispatcher());
        AreaCommand.register(event.getDispatcher());
        AccessCommand.register(event.getDispatcher());
        BlocksCommand.register(event.getDispatcher());
        DelayCommand.register(event.getDispatcher());
        ResetCommand.register(event.getDispatcher());
        StatusCommand.register(event.getDispatcher());
        ReloadCommand.register(event.getDispatcher());
        WandCommand.register(event.getDispatcher());
        EggCommand.register(event.getDispatcher());
        SoundCommand.register(event.getDispatcher());
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
     * 收纳木棍：潜行 + 右键把有内容的容器方块（方块状态 + 方块实体 NBT）加入正常池。
     */
    @SubscribeEvent
    public void onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown() || !event.getItemStack().is(CAPTURE_STICK.get())) {
            return;
        }
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof net.minecraft.world.Container container)) {
            event.getEntity().sendSystemMessage(Component.translatable("rsl.message.capture_not_container"));
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer failedPlayer) {
                yifei.rsl.game.SoundFx.play(failedPlayer, net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.6F, 1.0F);
            }
            event.setCanceled(true);
            return;
        }
        boolean hasItems = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) {
            event.getEntity().sendSystemMessage(Component.translatable("rsl.message.capture_empty"));
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer failedPlayer) {
                yifei.rsl.game.SoundFx.play(failedPlayer, net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.6F, 1.0F);
            }
            event.setCanceled(true);
            return;
        }
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(event.getPos());
        net.minecraft.nbt.CompoundTag tag = blockEntity.saveWithoutMetadata();
        String payload = yifei.rsl.game.NbtPayload.encode(tag);
        if (payload.isEmpty()) {
            event.getEntity().sendSystemMessage(Component.translatable("rsl.message.capture_failed"));
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer failedPlayer) {
                yifei.rsl.game.SoundFx.play(failedPlayer, net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.6F, 1.0F);
            }
            event.setCanceled(true);
            return;
        }
        if (payload.length() > yifei.rsl.common.config.PoolEntry.MAX_NBT_LENGTH) {
            event.getEntity().sendSystemMessage(Component.translatable("rsl.message.capture_too_large"));
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer failedPlayer) {
                yifei.rsl.game.SoundFx.play(failedPlayer, net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.6F, 1.0F);
            }
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        String encodedState = yifei.rsl.game.BlockStateCodec.encode(state);
        CaptureStickNetwork.beginCapture(serverPlayer, id, encodedState, payload, CaptureStickNetwork.displayName(tag));
        event.setCanceled(true);
        event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
        event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
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
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            WandSelectionManager.remove(player.getUUID());
            if (islandGame != null) {
                islandGame.onPlayerLogout(player);
            }
        }
    }

    /**
     * 服务器停止事件 - 替代 Fabric 的 ServerLifecycleEvents.SERVER_STOPPING
     * 用于保存游戏运行状态到配置文件
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (islandGame != null) {
            ModConfig.setRunning(islandGame.isRunning());
            if (islandGame.isRunning()) {
                ModConfig.setSecondsRemaining(islandGame.getSecondsRemaining());
            } else {
                ModConfig.setSecondsRemaining(0);
            }
            ModConfig.saveState();
            // 同步保存存档级区域配置
            ModConfig.saveSaveAreas();
        }
    }

    /**
     * 服务器完全停止后清理静态引用，避免单机切换存档时复用旧的 MinecraftServer。
     */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        islandGame = null;
    }

    public static IslandGame getIslandGame() {
        return islandGame;
    }
}
