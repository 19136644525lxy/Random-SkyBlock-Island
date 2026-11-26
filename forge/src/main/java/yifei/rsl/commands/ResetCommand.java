package yifei.rsl.commands;

import yifei.rsl.game.SoundFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.EggIslandSpawner;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 重置游戏命令
 *
 * 移植说明：同其他命令类，映射转换 + sendSuccess
 */
public class ResetCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("randomskyblock")
                .then(Commands.literal("reset")
                    .requires(source -> source.hasPermission(4))
                    .executes(ResetCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        IslandGame game = RandomSkyBlockIsland.getIslandGame();

        if (game == null) {
            context.getSource().sendFailure(Component.translatable("rsl.message.game_not_initialized"));
            return 0;
        }

        if (!DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }

        // 停止游戏（未运行时也会清理本地倒计时字段）
        game.stop();

        // 清理上一次生成的彩蛋岛
        new EggIslandSpawner(context.getSource().getServer()).clearRecordedIsland();

        // 把区域方块还原到第一次开始前的快照
        boolean restoring = game.beginRestore();

        // 重置运行时状态（保留区域和方块配置）
        ModConfig.setRunning(false);
        ModConfig.setSecondsRemaining(0);
        ModConfig.saveState();

        if (restoring) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("rsl.message.reset_restoring"), false);
            ServerPlayer reseter = context.getSource().getPlayer();
            if (reseter != null) {
                SoundFx.play(reseter, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.0F);
            }
        } else {
            context.getSource().sendSuccess(
                    () -> Component.translatable("rsl.message.reset_no_snapshot"), false);
            context.getSource().sendSuccess(() -> Component.translatable("rsl.message.reset_done",
                    ModConfig.getAllAreas().size(),
                    ModConfig.getAllowedBlocks().size()), false);
            ServerPlayer reseter = context.getSource().getPlayer();
            if (reseter != null) {
                SoundFx.play(reseter, SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            }
        }

        return 1;
    }
}
