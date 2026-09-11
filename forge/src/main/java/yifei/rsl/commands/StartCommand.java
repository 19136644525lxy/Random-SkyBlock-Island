package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 启动游戏命令
 *
 * 移植说明：
 * - CommandManager → Commands
 * - ServerCommandSource → CommandSourceStack
 * - hasPermissionLevel(4) → hasPermission(4)
 * - sendError → sendFailure
 */
public class StartCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("randomskyblock")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("start")
                    .executes(StartCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        IslandGame game = RandomSkyBlockIsland.getIslandGame();

        if (game == null) {
            context.getSource().sendFailure(Component.translatable("rsl.message.game_not_initialized"));
            return 0;
        }

        if (game.isRunning()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.already_running"));
            return 0;
        }

        game.start();
        game.getBossbar().addAllPlayers(context.getSource().getServer().getPlayerList().getPlayers());

        return 1;
    }
}
