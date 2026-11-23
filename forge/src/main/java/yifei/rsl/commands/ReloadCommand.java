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
 * 从磁盘重新加载配置与存档区域（Forge 映射）。
 */
public final class ReloadCommand {
    private ReloadCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("randomskyblock")
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(4))
                    .executes(ReloadCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        IslandGame game = RandomSkyBlockIsland.getIslandGame();

        if (game != null && game.hasPendingFills()) {
            source.sendFailure(Component.translatable("rsl.message.reload_pending"));
            return 0;
        }

        if (!ModConfig.reload()) {
            source.sendFailure(Component.translatable("rsl.message.reload_failed"));
            return 0;
        }

        if (game != null) {
            game.onConfigReloaded();
        }

        source.sendSuccess(() -> Component.translatable("rsl.message.reload_success",
            ModConfig.getTransformDelaySeconds(),
            ModConfig.getAllowedBlocks().size(),
            ModConfig.getUsableAreas().size()), false);

        return 1;
    }
}
