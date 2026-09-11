package yifei.rsl.commands;

import yifei.rsl.config.ModConfig;
import yifei.rsl.config.TransformArea;
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
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("area")
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
                    .then(Commands.literal("show")
                        .executes(AreaCommand::executeShow)
                    )
                    .then(Commands.literal("list")
                        .executes(AreaCommand::executeList)
                    )
                )
        );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        int minX = IntegerArgumentType.getInteger(context, "minX");
        int minY = IntegerArgumentType.getInteger(context, "minY");
        int minZ = IntegerArgumentType.getInteger(context, "minZ");
        int maxX = IntegerArgumentType.getInteger(context, "maxX");
        int maxY = IntegerArgumentType.getInteger(context, "maxY");
        int maxZ = IntegerArgumentType.getInteger(context, "maxZ");

        if (ModConfig.addArea(name, minX, minY, minZ, maxX, maxY, maxZ)) {
            ModConfig.saveSaveAreas();
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.area_added", name, minX, minY, minZ, maxX, maxY, maxZ),
                false
            );
        } else {
            context.getSource().sendFailure(Component.translatable("rsl.message.area_exists", name));
        }

        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        if (ModConfig.removeArea(name)) {
            ModConfig.saveSaveAreas();
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
        String oldName = StringArgumentType.getString(context, "oldName");
        String newName = StringArgumentType.getString(context, "newName");

        // 默认区域重命名需写全局配置，额外区域写存档级配置
        boolean isDefault = ModConfig.defaultArea.name.equals(oldName);

        if (ModConfig.renameArea(oldName, newName)) {
            if (isDefault) {
                ModConfig.saveConfig();
            } else {
                ModConfig.saveSaveAreas();
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
        var vec3 = context.getSource().getPosition();
        BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);

        // 直接修改默认区域（pos1/pos2 仅作用于默认区域）
        ModConfig.defaultArea.minX = pos.getX();
        ModConfig.defaultArea.minY = pos.getY();
        ModConfig.defaultArea.minZ = pos.getZ();
        ModConfig.saveConfig();

        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_pos1", pos.getX(), pos.getY(), pos.getZ()),
            false
        );

        return 1;
    }

    private static int executePos2(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var vec3 = context.getSource().getPosition();
        BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);

        ModConfig.defaultArea.maxX = pos.getX();
        ModConfig.defaultArea.maxY = pos.getY();
        ModConfig.defaultArea.maxZ = pos.getZ();
        ModConfig.saveConfig();

        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_pos2", pos.getX(), pos.getY(), pos.getZ()),
            false
        );

        return 1;
    }

    private static int executeSetFromPos(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        // 把当前默认区域的坐标作为新区域添加到存档级配置
        TransformArea area = ModConfig.defaultArea;
        ModConfig.addArea(name, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ);
        ModConfig.saveSaveAreas();

        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_setfrompos", name,
                area.minX, area.minY, area.minZ,
                area.maxX, area.maxY, area.maxZ),
            false
        );

        return 1;
    }

    private static int executeShow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // 显示默认区域的当前坐标
        TransformArea area = ModConfig.defaultArea;
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_show",
                area.name,
                area.minX, area.minY, area.minZ,
                area.maxX, area.maxY, area.maxZ),
            false
        );

        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // 列出所有区域（默认区域 + 存档区域）
        List<TransformArea> allAreas = ModConfig.getAllAreas();
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.area_list_count", allAreas.size()),
            false
        );

        for (TransformArea area : allAreas) {
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.area_list_item",
                    area.name,
                    area.minX, area.minY, area.minZ,
                    area.maxX, area.maxY, area.maxZ),
                false
            );
        }

        return 1;
    }
}
