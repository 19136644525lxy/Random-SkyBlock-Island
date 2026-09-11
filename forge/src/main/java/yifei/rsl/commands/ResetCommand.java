package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
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
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("reset")
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

        // 停止游戏
        if (game.isRunning()) {
            game.stop();
        }

        // 重置运行时状态（保留区域和方块配置）
        ModConfig.isRunning = false;
        ModConfig.secondsRemaining = 0;
        ModConfig.saveConfig();

        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.reset_done",
                ModConfig.getAllAreas().size(),
                ModConfig.allowedBlocks.size()), false);

        return 1;
    }
}
