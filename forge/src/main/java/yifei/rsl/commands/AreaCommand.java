package yifei.rsl.commands;

import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 区域管理命令
 *
 * 移植说明：
 * - CommandManager → Commands
 * - ServerCommandSource → CommandSourceStack
 * - getPosition() 返回 Vec3（Fabric 为 Vec3d）
 * - sendFeedback(supplier, false) → sendSuccess(supplier, false)
 * - sendError → sendFailure
 * - BlockPos 构造方式相同
 */
public class AreaCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("randomskyblock")
                .then(Commands.literal("area")
                    .requires(source -> source.hasPermission(4))
                    .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("minX", IntegerArgumentType.integer())
                                .then(Commands.argument("minY", IntegerArgumentType.integer())
                                    .then(Commands.argument("minZ", IntegerArgumentType.integer())
                                        .then(Commands.argument("maxX", IntegerArgumentType.integer())
                                            .then(Commands.argument("maxY", IntegerArgumentType.integer())
                                                .then(Commands.argument("maxZ", IntegerArgumentType.integer())
                                                    .executes(AreaCommand::executeAdd)
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(AreaCommand::executeRemove)
                        )
                    )
                    .then(Commands.literal("rename")
                        .then(Commands.argument("oldName", StringArgumentType.string())
                            .then(Commands.argument("newName", StringArgumentType.string())
                                .executes(AreaCommand::executeRename)
                            )
                        )
                    )
                    .then(Commands.literal("pos1")
                        .executes(AreaCommand::executePos1)
                    )
                    .then(Commands.literal("pos2")
                        .executes(AreaCommand::executePos2)
                    )
                    .then(Commands.literal("setfrompos")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(AreaCommand::executeSetFromPos)
                        )
                    )
                    .then(Commands.literal("initial")
                        .then(Commands.literal("show")
                            .executes(AreaCommand::executeInitialShow)
                        )
                        .then(Commands.literal("pos1")
                            .executes(AreaCommand::executeInitialPos1)
                        )
                        .then(Commands.literal("pos2")
                            .executes(AreaCommand::executeInitialPos2)
                        )
                    )
                    .then(Commands.literal("resetdefault")
                        .executes(AreaCommand::executeResetDefault)
                    )
                    .then(Commands.literal("show")
                        .executes(AreaCommand::executeShow)
                    )
                    .then(Commands.literal("list")
                        .executes(context -> executeList(context, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(context -> executeList(context,
                                IntegerArgumentType.getInteger(context, "page")))
                        )
                    )
                )
                .then(Commands.literal("areas")
                    .requires(source -> source.hasPermission(4))
                    .executes(context -> executeAreas(context, 1))
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> executeAreas(context,
                            IntegerArgumentType.getInteger(context, "page")))
                    )
                )
        );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }
        String name = StringArgumentType.getString(context, "name");
        int minX = IntegerArgumentType.getInteger(context, "minX");
        int minY = IntegerArgumentType.getInteger(context, "minY");
        int minZ = IntegerArgumentType.getInteger(context, "minZ");
        int maxX = IntegerArgumentType.getInteger(context, "maxX");
        int maxY = IntegerArgumentType.getInteger(context, "maxY");
        int maxZ = IntegerArgumentType.getInteger(context, "maxZ");

        if (ModConfig.addArea(name, minX, minY, minZ, maxX, maxY, maxZ)) {
            if (!ModConfig.saveSaveAreas()) {
                context.getSource().sendFailure(Component.translatable("rsl.message.area_save_failed"));
                return 0;
            }
            AreaSpec added = new AreaSpec(name, minX, minY, minZ, maxX, maxY, maxZ).normalized();
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.area_added", added.name,
                    added.minX, added.minY, added.minZ, added.maxX, added.maxY, added.maxZ),
                false
            );
        } else {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_add_failed", name));
        }

        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }
        String name = StringArgumentType.getString(context, "name");

        if (ModConfig.removeArea(name)) {
            if (!ModConfig.saveSaveAreas()) {
                context.getSource().sendFailure(Component.translatable("rsl.message.area_save_failed"));
                return 0;
            }
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.area_removed", name),
                false
            );
        } else {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_not_found_or_last", name));
        }

        return 1;
    }

    private static int executeRename(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");

        if (ModConfig.renameArea(oldName, newName)) {
            if (!ModConfig.saveSaveAreas()) {
                context.getSource().sendFailure(Component.translatable("rsl.message.area_save_failed"));
                return 0;
            }
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.area_renamed", oldName, newName),
                false
            );
        } else {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_rename_failed"));
        }

        return 1;
    }

    private static int executePos1(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }
        var vec3 = context.getSource().getPosition();
        BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);

        ModConfig.getDefaultArea().minX = pos.getX();
        ModConfig.getDefaultArea().minY = pos.getY();
        ModConfig.getDefaultArea().minZ = pos.getZ();
        if (!ModConfig.saveSaveAreas()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_save_failed"));
            return 0;
        }

        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_pos1", pos.getX(), pos.getY(), pos.getZ()),
            false
        );
        if (!ModConfig.isValidArea(ModConfig.getDefaultArea())) {
            context.getSource().sendSuccess(() -> Component.translatable("rsl.message.area_invalid_warning"), false);
        }

        return 1;
    }

    private static int executePos2(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }
        var vec3 = context.getSource().getPosition();
        BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);

        ModConfig.getDefaultArea().maxX = pos.getX();
        ModConfig.getDefaultArea().maxY = pos.getY();
        ModConfig.getDefaultArea().maxZ = pos.getZ();
        if (!ModConfig.saveSaveAreas()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_save_failed"));
            return 0;
        }

        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_pos2", pos.getX(), pos.getY(), pos.getZ()),
            false
        );
        if (!ModConfig.isValidArea(ModConfig.getDefaultArea())) {
            context.getSource().sendSuccess(() -> Component.translatable("rsl.message.area_invalid_warning"), false);
        }

        return 1;
    }

    private static int executeSetFromPos(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }
        String name = StringArgumentType.getString(context, "name");

        AreaSpec area = ModConfig.getDefaultArea().normalized();
        if (ModConfig.addArea(name, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)) {
            if (!ModConfig.saveSaveAreas()) {
                context.getSource().sendFailure(Component.translatable("rsl.message.area_save_failed"));
                return 0;
            }
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.area_setfrompos", name,
                    area.minX, area.minY, area.minZ,
                    area.maxX, area.maxY, area.maxZ),
                false
            );
        } else {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_add_failed", name));
        }

        return 1;
    }

    private static int executeShow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!ensureWorld(context.getSource())) {
            return 0;
        }
        AreaSpec area = ModConfig.getDefaultArea();
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_show",
                area.name,
                area.minX, area.minY, area.minZ,
                area.maxX, area.maxY, area.maxZ),
            false
        );

        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> context, int page)
            throws CommandSyntaxException {
        return executeAreas(context, page);
    }

    private static int executeAreas(CommandContext<CommandSourceStack> context, int requestedPage)
            throws CommandSyntaxException {
        if (!ensureWorld(context.getSource())) {
            return 0;
        }
        List<AreaSpec> allAreas = ModConfig.getAllAreas();
        if (allAreas.isEmpty()) {
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.areas_empty"),
                false
            );
            return 1;
        }

        final int pageSize = 8;
        int pages = Math.max(1, (allAreas.size() + pageSize - 1) / pageSize);
        int page = Math.max(1, Math.min(requestedPage, pages));
        int from = (page - 1) * pageSize;
        int to = Math.min(allAreas.size(), from + pageSize);
        String activeDimension = ModConfig.getActiveDimension();
        final String dimension = activeDimension == null ? "-" : activeDimension;
        final int shownPage = page;
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.areas_header",
                allAreas.size(), dimension, shownPage, pages),
            false
        );

        AreaSpec defaultArea = ModConfig.getDefaultArea();
        for (int i = from; i < to; i++) {
            AreaSpec area = allAreas.get(i);
            boolean isDefault = defaultArea != null && defaultArea.name.equals(area.name);
            boolean valid = ModConfig.isValidArea(area);
            String key = !valid ? "rsl.message.areas_item_invalid"
                    : isDefault ? "rsl.message.areas_item_default"
                    : "rsl.message.areas_item_named";
            context.getSource().sendSuccess(
                () -> Component.translatable(key, area.name,
                    area.minX, area.minY, area.minZ,
                    area.maxX, area.maxY, area.maxZ,
                    area.volumeSaturated()),
                false
            );
        }
        if (pages > 1) {
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.areas_page", shownPage, pages),
                false
            );
        }
        return 1;
    }

    private static int executeInitialShow(CommandContext<CommandSourceStack> context) {
        AreaSpec area = ModConfig.getInitialDefaultArea();
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_initial_show",
                area.name,
                area.minX, area.minY, area.minZ,
                area.maxX, area.maxY, area.maxZ),
            false
        );
        return 1;
    }

    private static int executeInitialPos1(CommandContext<CommandSourceStack> context) {
        var vec3 = context.getSource().getPosition();
        BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
        AreaSpec area = ModConfig.getInitialDefaultArea();
        area.minX = pos.getX();
        area.minY = pos.getY();
        area.minZ = pos.getZ();
        if (!ModConfig.saveSettings()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_initial_pos1", pos.getX(), pos.getY(), pos.getZ()),
            false
        );
        if (!ModConfig.isValidArea(area)) {
            context.getSource().sendSuccess(() -> Component.translatable("rsl.message.area_invalid_warning"), false);
        }
        return 1;
    }

    private static int executeInitialPos2(CommandContext<CommandSourceStack> context) {
        var vec3 = context.getSource().getPosition();
        BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
        AreaSpec area = ModConfig.getInitialDefaultArea();
        area.maxX = pos.getX();
        area.maxY = pos.getY();
        area.maxZ = pos.getZ();
        if (!ModConfig.saveSettings()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_save_failed"));
            return 0;
        }
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_initial_pos2", pos.getX(), pos.getY(), pos.getZ()),
            false
        );
        if (!ModConfig.isValidArea(area)) {
            context.getSource().sendSuccess(() -> Component.translatable("rsl.message.area_invalid_warning"), false);
        }
        return 1;
    }

    private static int executeResetDefault(CommandContext<CommandSourceStack> context) {
        if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
            return 0;
        }
        if (!ModConfig.resetWorldDefaultToInitial()) {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_reset_default_failed"));
            return 0;
        }
        AreaSpec area = ModConfig.getDefaultArea();
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_reset_default",
                area.name,
                area.minX, area.minY, area.minZ,
                area.maxX, area.maxY, area.maxZ),
            false
        );
        return 1;
    }

    private static boolean ensureWorld(CommandSourceStack source) {
        if (!ModConfig.isWorldLoaded()) {
            source.sendFailure(Component.translatable("rsl.message.world_not_initialized"));
            return false;
        }
        return true;
    }
}
