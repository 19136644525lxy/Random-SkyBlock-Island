package yifei.rsl.commands;

import yifei.rsl.game.SoundFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

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
                .then(Commands.literal("start")
                    .requires(source -> source.hasPermission(4))
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

        if (!DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }

        if (game.isRunning()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.already_running"));
            return 0;
        }

        if (ModConfig.getUsableAreas().isEmpty()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.no_valid_areas"));
            return 0;
        }

        ServerLevel world = context.getSource().getLevel();
        if (!game.start(world)) {
            if (ModConfig.isBlockPoolEmpty()) {
                context.getSource().sendFailure(Component.translatable("rsl.message.block_pool_empty_start"));
            } else if (ModConfig.getUsableAreas().isEmpty()) {
                context.getSource().sendFailure(Component.translatable("rsl.message.no_valid_areas"));
            } else {
                context.getSource().sendFailure(Component.translatable("rsl.message.start_failed"));
            }
            return 0;
        }
        game.getBossbar().addAllPlayers(context.getSource().getServer().getPlayerList().getPlayers());
        ServerPlayer starter = context.getSource().getPlayer();
        if (starter != null) {
            SoundFx.play(starter, SoundEvents.NOTE_BLOCK_PLING.value(), 0.7F, 1.2F);
        }

        return 1;
    }
}
