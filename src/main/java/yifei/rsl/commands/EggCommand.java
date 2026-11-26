package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import yifei.rsl.common.block.BlockId;
import yifei.rsl.common.egg.EggIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.permission.PoolPermissions;

import java.util.ArrayList;
import java.util.List;

/**
 * Owner-only egg-mode controls: toggle, chance, radius, volume, spacing and the
 * egg block pool. Mirrors the values edited by the in-game "other pools" screen.
 */
public final class EggCommand {
    private EggCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("randomskyblock")
                        .then(CommandManager.literal("egg")
                                .requires(PoolPermissions::canManage)
                                .then(CommandManager.literal("status").executes(EggCommand::executeStatus))
                                .then(CommandManager.literal("on").executes(context -> setEnabled(context, true)))
                                .then(CommandManager.literal("off").executes(context -> setEnabled(context, false)))
                                .then(CommandManager.literal("toggle").executes(EggCommand::executeToggle))
                                .then(CommandManager.literal("chance")
                                        .then(CommandManager.argument("percent", DoubleArgumentType.doubleArg(1, 100))
                                                .executes(EggCommand::executeChance)))
                                .then(CommandManager.literal("radius")
                                        .then(CommandManager.argument("min", IntegerArgumentType.integer(1, 256))
                                                .then(CommandManager.argument("max", IntegerArgumentType.integer(1, 512))
                                                        .executes(EggCommand::executeRadius))))
                                .then(CommandManager.literal("volume")
                                        .then(CommandManager.argument("volume", IntegerArgumentType.integer(1, 4096))
                                                .executes(EggCommand::executeVolume)))
                                .then(CommandManager.literal("distance")
                                        .then(CommandManager.argument("distance", IntegerArgumentType.integer(1, 256))
                                                .executes(EggCommand::executeDistance)))
                                .then(CommandManager.literal("pool")
                                        .then(CommandManager.literal("list").executes(EggCommand::executePoolList))
                                        .then(CommandManager.literal("add")
                                                .then(CommandManager.argument("id", StringArgumentType.string())
                                                        .executes(EggCommand::executePoolAdd)))
                                        .then(CommandManager.literal("remove")
                                                .then(CommandManager.argument("id", StringArgumentType.string())
                                                        .executes(EggCommand::executePoolRemove)))
                                        .then(CommandManager.literal("clear").executes(EggCommand::executePoolClear)))
                        )
        );
    }

    private static int executeStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        boolean enabled = ModConfig.isEggModeEnabled();
        int volume = ModConfig.getEggVolume();
        int edge = EggIsland.edgeLength(volume);
        source.sendFeedback(() -> Text.translatable("rsl.message.egg_status",
                Text.translatable(enabled ? "rsl.message.egg_on" : "rsl.message.egg_off"),
                (int) Math.round(ModConfig.getEggChance() * 100.0)), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.egg_status_radius",
                ModConfig.getEggMinRadius(), ModConfig.getEggMaxRadius()), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.egg_status_volume", edge, volume), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.egg_status_distance",
                ModConfig.getEggMinDistance()), false);
        source.sendFeedback(() -> Text.translatable("rsl.message.egg_status_pool",
                ModConfig.getEggPoolIds().size()), false);
        return 1;
    }

    private static int executeToggle(CommandContext<ServerCommandSource> context) {
        boolean ok = ModConfig.toggleEggMode();
        if (!ok) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        boolean enabled = ModConfig.isEggModeEnabled();
        context.getSource().sendFeedback(() -> Text.translatable(
                enabled ? "rsl.message.egg_enabled" : "rsl.message.egg_disabled"), false);
        return 1;
    }

    private static int setEnabled(CommandContext<ServerCommandSource> context, boolean enabled) {
        if (!ModConfig.setEggModeEnabled(enabled)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable(
                enabled ? "rsl.message.egg_enabled" : "rsl.message.egg_disabled"), false);
        return 1;
    }

    private static int executeChance(CommandContext<ServerCommandSource> context) {
        double percent = DoubleArgumentType.getDouble(context, "percent");
        if (!ModConfig.setEggChance(percent / 100.0)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_chance_set",
                (int) Math.round(ModConfig.getEggChance() * 100.0)), false);
        return 1;
    }

    private static int executeRadius(CommandContext<ServerCommandSource> context) {
        int min = IntegerArgumentType.getInteger(context, "min");
        int max = IntegerArgumentType.getInteger(context, "max");
        if (!ModConfig.setEggRadius(min, max)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_radius_set",
                ModConfig.getEggMinRadius(), ModConfig.getEggMaxRadius()), false);
        return 1;
    }

    private static int executeVolume(CommandContext<ServerCommandSource> context) {
        int volume = IntegerArgumentType.getInteger(context, "volume");
        if (!ModConfig.setEggVolume(volume)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        int edge = EggIsland.edgeLength(ModConfig.getEggVolume());
        context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_volume_set",
                edge, ModConfig.getEggVolume()), false);
        return 1;
    }

    private static int executeDistance(CommandContext<ServerCommandSource> context) {
        int distance = IntegerArgumentType.getInteger(context, "distance");
        if (!ModConfig.setEggMinDistance(distance)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_distance_set",
                ModConfig.getEggMinDistance()), false);
        return 1;
    }

    private static int executePoolList(CommandContext<ServerCommandSource> context) {
        List<String> ids = ModConfig.getEggPoolIds();
        context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_pool_header", ids.size()), false);
        if (ids.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_pool_empty"), false);
            return 1;
        }
        for (String id : ids) {
            context.getSource().sendFeedback(() -> Text.literal("- " + id), false);
        }
        return 1;
    }

    private static int executePoolAdd(CommandContext<ServerCommandSource> context) {
        String raw = StringArgumentType.getString(context, "id");
        String id = BlockId.normalize(raw);
        if (id == null) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_pool_unknown", raw));
            return 0;
        }
        List<String> ids = new ArrayList<>(ModConfig.getEggPoolIds());
        if (ids.contains(id)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_pool_known", id));
            return 0;
        }
        ids.add(id);
        if (!ModConfig.setEggPool(ids) || !ModConfig.getEggPoolIds().contains(id)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_pool_added", id), false);
        return 1;
    }

    private static int executePoolRemove(CommandContext<ServerCommandSource> context) {
        String raw = StringArgumentType.getString(context, "id");
        String id = BlockId.normalize(raw);
        List<String> ids = new ArrayList<>(ModConfig.getEggPoolIds());
        if (id == null || !ids.remove(id)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_pool_not_found", raw));
            return 0;
        }
        if (!ModConfig.setEggPool(ids)) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_pool_removed", id), false);
        return 1;
    }

    private static int executePoolClear(CommandContext<ServerCommandSource> context) {
        if (!ModConfig.setEggPool(new ArrayList<>())) {
            context.getSource().sendError(Text.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendFeedback(() -> Text.translatable("rsl.message.egg_pool_cleared"), false);
        return 1;
    }
}
