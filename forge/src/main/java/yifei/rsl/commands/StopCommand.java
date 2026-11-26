package yifei.rsl.commands;

import yifei.rsl.game.SoundFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 停止游戏命令
 *
 * 移植说明：同 StartCommand，CommandManager → Commands，sendError → sendFailure
 */
public class StopCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("randomskyblock")
                .then(Commands.literal("stop")
                    .requires(source -> source.hasPermission(4))
                    .executes(StopCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        IslandGame game = RandomSkyBlockIsland.getIslandGame();

        if (game == null) {
            context.getSource().sendFailure(Component.translatable("rsl.message.game_not_initialized"));
            return 0;
        }

        if (!game.isRunning()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.not_running"));
            return 0;
        }

        game.stop();

        ServerPlayer stopper = context.getSource().getPlayer();
        if (stopper != null) {
            SoundFx.play(stopper, SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.8F);
        }

        return 1;
    }
}
