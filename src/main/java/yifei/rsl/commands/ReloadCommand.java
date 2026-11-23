package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;

/**
 * 从磁盘重新加载配置与存档区域，不重启服务器。
 */
public final class ReloadCommand {
    private ReloadCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("randomskyblock")
                .then(CommandManager.literal("reload")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(ReloadCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        IslandGame game = RandomSkyBlockIsland.getIslandGame();

        if (game != null && game.hasPendingFills()) {
            source.sendError(Text.translatable("rsl.message.reload_pending"));
            return 0;
        }

        if (!ModConfig.reload()) {
            source.sendError(Text.translatable("rsl.message.reload_failed"));
            return 0;
        }

        if (game != null) {
            game.onConfigReloaded();
        }

        source.sendFeedback(() -> Text.translatable("rsl.message.reload_success",
            ModConfig.getTransformDelaySeconds(),
            ModConfig.getAllowedBlocks().size(),
            ModConfig.getUsableAreas().size()), false);

        return 1;
    }
}
