package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("randomskyblock")
                        .then(Commands.literal("egg")
                                .requires(PoolPermissions::canManage)
                                .then(Commands.literal("status").executes(EggCommand::executeStatus))
                                .then(Commands.literal("on").executes(context -> setEnabled(context, true)))
                                .then(Commands.literal("off").executes(context -> setEnabled(context, false)))
                                .then(Commands.literal("toggle").executes(EggCommand::executeToggle))
                                .then(Commands.literal("chance")
                                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(1, 100))
                                                .executes(EggCommand::executeChance)))
                                .then(Commands.literal("radius")
                                        .then(Commands.argument("min", IntegerArgumentType.integer(1, 256))
                                                .then(Commands.argument("max", IntegerArgumentType.integer(1, 512))
                                                        .executes(EggCommand::executeRadius))))
                                .then(Commands.literal("volume")
                                        .then(Commands.argument("volume", IntegerArgumentType.integer(1, 4096))
                                                .executes(EggCommand::executeVolume)))
                                .then(Commands.literal("distance")
                                        .then(Commands.argument("distance", IntegerArgumentType.integer(1, 256))
                                                .executes(EggCommand::executeDistance)))
                                .then(Commands.literal("pool")
                                        .then(Commands.literal("list").executes(EggCommand::executePoolList))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("id", StringArgumentType.string())
                                                        .executes(EggCommand::executePoolAdd)))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("id", StringArgumentType.string())
                                                        .executes(EggCommand::executePoolRemove)))
                                        .then(Commands.literal("clear").executes(EggCommand::executePoolClear)))
                        )
        );
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean enabled = ModConfig.isEggModeEnabled();
        int volume = ModConfig.getEggVolume();
        int edge = EggIsland.edgeLength(volume);
        source.sendSuccess(() -> Component.translatable("rsl.message.egg_status",
                Component.translatable(enabled ? "rsl.message.egg_on" : "rsl.message.egg_off"),
                (int) Math.round(ModConfig.getEggChance() * 100.0)), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.egg_status_radius",
                ModConfig.getEggMinRadius(), ModConfig.getEggMaxRadius()), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.egg_status_volume", edge, volume), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.egg_status_distance",
                ModConfig.getEggMinDistance()), false);
        source.sendSuccess(() -> Component.translatable("rsl.message.egg_status_pool",
                ModConfig.getEggPoolIds().size()), false);
        return 1;
    }

    private static int executeToggle(CommandContext<CommandSourceStack> context) {
        boolean ok = ModConfig.toggleEggMode();
        if (!ok) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        boolean enabled = ModConfig.isEggModeEnabled();
        context.getSource().sendSuccess(() -> Component.translatable(
                enabled ? "rsl.message.egg_enabled" : "rsl.message.egg_disabled"), false);
        return 1;
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        if (!ModConfig.setEggModeEnabled(enabled)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable(
                enabled ? "rsl.message.egg_enabled" : "rsl.message.egg_disabled"), false);
        return 1;
    }

    private static int executeChance(CommandContext<CommandSourceStack> context) {
        double percent = DoubleArgumentType.getDouble(context, "percent");
        if (!ModConfig.setEggChance(percent / 100.0)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_chance_set",
                (int) Math.round(ModConfig.getEggChance() * 100.0)), false);
        return 1;
    }

    private static int executeRadius(CommandContext<CommandSourceStack> context) {
        int min = IntegerArgumentType.getInteger(context, "min");
        int max = IntegerArgumentType.getInteger(context, "max");
        if (!ModConfig.setEggRadius(min, max)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_radius_set",
                ModConfig.getEggMinRadius(), ModConfig.getEggMaxRadius()), false);
        return 1;
    }

    private static int executeVolume(CommandContext<CommandSourceStack> context) {
        int volume = IntegerArgumentType.getInteger(context, "volume");
        if (!ModConfig.setEggVolume(volume)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        int edge = EggIsland.edgeLength(ModConfig.getEggVolume());
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_volume_set",
                edge, ModConfig.getEggVolume()), false);
        return 1;
    }

    private static int executeDistance(CommandContext<CommandSourceStack> context) {
        int distance = IntegerArgumentType.getInteger(context, "distance");
        if (!ModConfig.setEggMinDistance(distance)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_distance_set",
                ModConfig.getEggMinDistance()), false);
        return 1;
    }

    private static int executePoolList(CommandContext<CommandSourceStack> context) {
        List<String> ids = ModConfig.getEggPoolIds();
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_pool_header", ids.size()), false);
        if (ids.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_pool_empty"), false);
            return 1;
        }
        for (String id : ids) {
            context.getSource().sendSuccess(() -> Component.literal("- " + id), false);
        }
        return 1;
    }

    private static int executePoolAdd(CommandContext<CommandSourceStack> context) {
        String raw = StringArgumentType.getString(context, "id");
        String id = BlockId.normalize(raw);
        if (id == null) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_pool_unknown", raw));
            return 0;
        }
        List<String> ids = new ArrayList<>(ModConfig.getEggPoolIds());
        if (ids.contains(id)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_pool_known", id));
            return 0;
        }
        ids.add(id);
        if (!ModConfig.setEggPool(ids) || !ModConfig.getEggPoolIds().contains(id)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_pool_added", id), false);
        return 1;
    }

    private static int executePoolRemove(CommandContext<CommandSourceStack> context) {
        String raw = StringArgumentType.getString(context, "id");
        String id = BlockId.normalize(raw);
        List<String> ids = new ArrayList<>(ModConfig.getEggPoolIds());
        if (id == null || !ids.remove(id)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_pool_not_found", raw));
            return 0;
        }
        if (!ModConfig.setEggPool(ids)) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_pool_removed", id), false);
        return 1;
    }

    private static int executePoolClear(CommandContext<CommandSourceStack> context) {
        if (!ModConfig.setEggPool(new ArrayList<>())) {
            context.getSource().sendFailure(Component.translatable("rsl.message.egg_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.egg_pool_cleared"), false);
        return 1;
    }
}
