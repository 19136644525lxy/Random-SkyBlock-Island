package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.config.TransformArea;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class AreaCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.requires(source -> source.hasPermissionLevel(4))
				.then(CommandManager.literal("area")
					.then(CommandManager.literal("add")
						.then(CommandManager.argument("name", StringArgumentType.string())
							.then(CommandManager.argument("minX", IntegerArgumentType.integer())
								.then(CommandManager.argument("minY", IntegerArgumentType.integer())
									.then(CommandManager.argument("minZ", IntegerArgumentType.integer())
										.then(CommandManager.argument("maxX", IntegerArgumentType.integer())
											.then(CommandManager.argument("maxY", IntegerArgumentType.integer())
												.then(CommandManager.argument("maxZ", IntegerArgumentType.integer())
													.executes(AreaCommand::executeAdd)
												)
											)
										)
									)
								)
							)
						)
					)
					.then(CommandManager.literal("remove")
						.then(CommandManager.argument("name", StringArgumentType.string())
							.executes(AreaCommand::executeRemove)
						)
					)
					.then(CommandManager.literal("rename")
						.then(CommandManager.argument("oldName", StringArgumentType.string())
							.then(CommandManager.argument("newName", StringArgumentType.string())
								.executes(AreaCommand::executeRename)
							)
						)
					)
					.then(CommandManager.literal("pos1")
						.executes(AreaCommand::executePos1)
					)
					.then(CommandManager.literal("pos2")
						.executes(AreaCommand::executePos2)
					)
					.then(CommandManager.literal("setfrompos")
						.then(CommandManager.argument("name", StringArgumentType.string())
							.executes(AreaCommand::executeSetFromPos)
						)
					)
					.then(CommandManager.literal("show")
						.executes(AreaCommand::executeShow)
					)
					.then(CommandManager.literal("list")
						.executes(AreaCommand::executeList)
					)
				)
		);
	}
	
	private static int executeAdd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		int minX = IntegerArgumentType.getInteger(context, "minX");
		int minY = IntegerArgumentType.getInteger(context, "minY");
		int minZ = IntegerArgumentType.getInteger(context, "minZ");
		int maxX = IntegerArgumentType.getInteger(context, "maxX");
		int maxY = IntegerArgumentType.getInteger(context, "maxY");
		int maxZ = IntegerArgumentType.getInteger(context, "maxZ");
		
		if (ModConfig.addArea(name, minX, minY, minZ, maxX, maxY, maxZ)) {
			ModConfig.saveSaveAreas();
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.area_added", name, minX, minY, minZ, maxX, maxY, maxZ),
				false
			);
		} else {
			context.getSource().sendError(Text.translatable("rsl.message.area_exists", name));
		}
		
		return 1;
	}
	
	private static int executeRemove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		
		if (ModConfig.removeArea(name)) {
			ModConfig.saveSaveAreas();
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.area_removed", name),
				false
			);
		} else {
			context.getSource().sendError(Text.translatable("rsl.message.area_not_found_or_last", name));
		}
		
		return 1;
	}

	private static int executeRename(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String oldName = StringArgumentType.getString(context, "oldName");
		String newName = StringArgumentType.getString(context, "newName");
		
		boolean isDefault = ModConfig.defaultArea.name.equals(oldName);
		
		if (ModConfig.renameArea(oldName, newName)) {
			if (isDefault) {
				ModConfig.saveConfig();
			} else {
				ModConfig.saveSaveAreas();
			}
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.area_renamed", oldName, newName),
				false
			);
		} else {
			context.getSource().sendError(Text.translatable("rsl.message.area_rename_failed"));
		}
		
		return 1;
	}
	
	private static int executePos1(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var vec3 = context.getSource().getPosition();
		BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
		
		ModConfig.defaultArea.minX = pos.getX();
		ModConfig.defaultArea.minY = pos.getY();
		ModConfig.defaultArea.minZ = pos.getZ();
		ModConfig.saveConfig();
		
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_pos1", pos.getX(), pos.getY(), pos.getZ()),
			false
		);
		
		return 1;
	}

	private static int executePos2(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var vec3 = context.getSource().getPosition();
		BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
		
		ModConfig.defaultArea.maxX = pos.getX();
		ModConfig.defaultArea.maxY = pos.getY();
		ModConfig.defaultArea.maxZ = pos.getZ();
		ModConfig.saveConfig();
		
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_pos2", pos.getX(), pos.getY(), pos.getZ()),
			false
		);
		
		return 1;
	}

	private static int executeSetFromPos(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		
		TransformArea area = ModConfig.defaultArea;
		ModConfig.addArea(name, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ);
		ModConfig.saveSaveAreas();
		
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_setfrompos", name,
				area.minX, area.minY, area.minZ,
				area.maxX, area.maxY, area.maxZ),
			false
		);
		
		return 1;
	}

	private static int executeShow(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		TransformArea area = ModConfig.defaultArea;
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_show",
				area.name,
				area.minX, area.minY, area.minZ,
				area.maxX, area.maxY, area.maxZ),
			false
		);
		
		return 1;
	}

	private static int executeList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		List<TransformArea> allAreas = ModConfig.getAllAreas();
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_list_count", allAreas.size()),
			false
		);
		
		for (TransformArea area : allAreas) {
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.area_list_item",
					area.name,
					area.minX, area.minY, area.minZ,
					area.maxX, area.maxY, area.maxZ),
				false
			);
		}
		
		return 1;
	}
}
