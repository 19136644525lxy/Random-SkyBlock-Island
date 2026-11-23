package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import yifei.rsl.permission.PoolPermissions;

/**
 * 只读状态查询命令。
 */
public final class StatusCommand {
    private StatusCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("randomskyblock")
                .then(CommandManager.literal("status")
                    .executes(StatusCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        IslandGame game = RandomSkyBlockIsland.getIslandGame();
        if (game == null) {
            source.sendError(Text.translatable("rsl.message.game_not_initialized"));
            return 0;
        }

        source.sendFeedback(() -> Text.translatable("rsl.message.status.header"), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.status.dimension",
            ModConfig.hasActiveDimension() ? ModConfig.getActiveDimension() : "-"), false);
        source.sendFeedback(() -> game.isRunning()
            ? Text.translatable("rsl.message.status.running_yes")
            : Text.translatable("rsl.message.status.running_no"), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.status.delay",
            ModConfig.getTransformDelaySeconds()), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.status.remaining",
            game.getSecondsRemaining()), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.status.areas",
            ModConfig.getUsableAreas().size(), ModConfig.getAllAreas().size()), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.status.blocks",
            ModConfig.getAllowedBlocks().size(), ModConfig.getAllowedBlockIds().size(),
            ModConfig.getInvalidBlockIds().size()), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.status.pending",
            game.getPendingFillCount(), game.getPendingBlockCount()), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.status.pool_access",
            accessLabel(source)), false);

        return 1;
    }

    private static Text accessLabel(ServerCommandSource source) {
        if (source.getPlayer() == null) {
            return Text.translatable("rsl.message.status.pool_access_console");
        }
        if (PoolPermissions.canManage(source)) {
            return Text.translatable("rsl.message.status.pool_access_manage");
        }
        if (PoolPermissions.canUse(source)) {
            return Text.translatable("rsl.message.status.pool_access_edit");
        }
        return Text.translatable("rsl.message.status.pool_access_none");
    }
}
