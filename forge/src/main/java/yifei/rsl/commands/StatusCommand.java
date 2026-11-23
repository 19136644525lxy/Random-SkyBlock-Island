package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;

/**
 * 只读状态查询命令（Forge 映射）。
 */
public final class StatusCommand {
    private StatusCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("randomskyblock")
                .then(Commands.literal("status")
                    .executes(StatusCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        IslandGame game = RandomSkyBlockIsland.getIslandGame();
        if (game == null) {
            source.sendFailure(Component.translatable("rsl.message.game_not_initialized"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("rsl.message.status.header"), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.status.dimension",
            ModConfig.hasActiveDimension() ? ModConfig.getActiveDimension() : "-"), false);
        source.sendSuccess(() -> game.isRunning()
            ? Component.translatable("rsl.message.status.running_yes")
            : Component.translatable("rsl.message.status.running_no"), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.status.delay",
            ModConfig.getTransformDelaySeconds()), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.status.remaining",
            game.getSecondsRemaining()), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.status.areas",
            ModConfig.getUsableAreas().size(), ModConfig.getAllAreas().size()), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.status.blocks",
            ModConfig.getAllowedBlocks().size(), ModConfig.getAllowedBlockIds().size(),
            ModConfig.getInvalidBlockIds().size()), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.status.pending",
            game.getPendingFillCount(), game.getPendingBlockCount()), false);

        return 1;
    }
}
