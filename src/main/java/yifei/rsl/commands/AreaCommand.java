package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.config.ModConfig;
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
				.then(CommandManager.literal("area")
					.requires(source -> source.hasPermissionLevel(4))
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
					.then(CommandManager.literal("initial")
						.then(CommandManager.literal("show")
							.executes(AreaCommand::executeInitialShow)
						)
						.then(CommandManager.literal("pos1")
							.executes(AreaCommand::executeInitialPos1)
						)
						.then(CommandManager.literal("pos2")
							.executes(AreaCommand::executeInitialPos2)
						)
					)
					.then(CommandManager.literal("resetdefault")
						.executes(AreaCommand::executeResetDefault)
					)
					.then(CommandManager.literal("show")
						.executes(AreaCommand::executeShow)
					)
					.then(CommandManager.literal("list")
						.executes(context -> executeList(context, 1))
						.then(CommandManager.argument("page", IntegerArgumentType.integer(1))
							.executes(context -> executeList(context,
								IntegerArgumentType.getInteger(context, "page")))
						)
					)
				)
				.then(CommandManager.literal("areas")
					.requires(source -> source.hasPermissionLevel(4))
					.executes(context -> executeAreas(context, 1))
					.then(CommandManager.argument("page", IntegerArgumentType.integer(1))
						.executes(context -> executeAreas(context,
							IntegerArgumentType.getInteger(context, "page")))
					)
				)
		);
	}
	
	private static int executeAdd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
				context.getSource().sendError(Text.translatable("rsl.message.area_save_failed"));
				return 0;
			}
			AreaSpec added = new AreaSpec(name, minX, minY, minZ, maxX, maxY, maxZ).normalized();
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.area_added", added.name,
					added.minX, added.minY, added.minZ, added.maxX, added.maxY, added.maxZ),
				false
			);
		} else {
			context.getSource().sendError(Text.translatable("rsl.message.area_add_failed", name));
		}
		
		return 1;
	}
	
	private static int executeRemove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
			return 0;
		}
		String name = StringArgumentType.getString(context, "name");
		
		if (ModConfig.removeArea(name)) {
			if (!ModConfig.saveSaveAreas()) {
				context.getSource().sendError(Text.translatable("rsl.message.area_save_failed"));
				return 0;
			}
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
		if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
			return 0;
		}
		String oldName = StringArgumentType.getString(context, "oldName");
		String newName = StringArgumentType.getString(context, "newName");
		
		if (ModConfig.renameArea(oldName, newName)) {
			if (!ModConfig.saveSaveAreas()) {
				context.getSource().sendError(Text.translatable("rsl.message.area_save_failed"));
				return 0;
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
		if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
			return 0;
		}
		var vec3 = context.getSource().getPosition();
		BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
		
		ModConfig.getDefaultArea().minX = pos.getX();
		ModConfig.getDefaultArea().minY = pos.getY();
		ModConfig.getDefaultArea().minZ = pos.getZ();
		if (!ModConfig.saveSaveAreas()) {
			context.getSource().sendError(Text.translatable("rsl.message.area_save_failed"));
			return 0;
		}
		
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_pos1", pos.getX(), pos.getY(), pos.getZ()),
			false
		);
		if (!ModConfig.isValidArea(ModConfig.getDefaultArea())) {
			context.getSource().sendFeedback(() -> Text.translatable("rsl.message.area_invalid_warning"), false);
		}
		
		return 1;
	}

	private static int executePos2(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
			return 0;
		}
		var vec3 = context.getSource().getPosition();
		BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
		
		ModConfig.getDefaultArea().maxX = pos.getX();
		ModConfig.getDefaultArea().maxY = pos.getY();
		ModConfig.getDefaultArea().maxZ = pos.getZ();
		if (!ModConfig.saveSaveAreas()) {
			context.getSource().sendError(Text.translatable("rsl.message.area_save_failed"));
			return 0;
		}
		
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_pos2", pos.getX(), pos.getY(), pos.getZ()),
			false
		);
		if (!ModConfig.isValidArea(ModConfig.getDefaultArea())) {
			context.getSource().sendFeedback(() -> Text.translatable("rsl.message.area_invalid_warning"), false);
		}
		
		return 1;
	}

	private static int executeSetFromPos(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
			return 0;
		}
		String name = StringArgumentType.getString(context, "name");
		
		AreaSpec area = ModConfig.getDefaultArea().normalized();
		if (ModConfig.addArea(name, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)) {
			if (!ModConfig.saveSaveAreas()) {
				context.getSource().sendError(Text.translatable("rsl.message.area_save_failed"));
				return 0;
			}
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.area_setfrompos", name,
					area.minX, area.minY, area.minZ,
					area.maxX, area.maxY, area.maxZ),
				false
			);
		} else {
			context.getSource().sendError(Text.translatable("rsl.message.area_add_failed", name));
		}
		
		return 1;
	}

	private static int executeShow(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		if (!ensureWorld(context.getSource())) {
			return 0;
		}
		AreaSpec area = ModConfig.getDefaultArea();
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_show",
				area.name,
				area.minX, area.minY, area.minZ,
				area.maxX, area.maxY, area.maxZ),
			false
		);
		
		return 1;
	}

	private static int executeList(CommandContext<ServerCommandSource> context, int page)
			throws CommandSyntaxException {
		return executeAreas(context, page);
	}

	private static int executeAreas(CommandContext<ServerCommandSource> context, int requestedPage)
			throws CommandSyntaxException {
		if (!ensureWorld(context.getSource())) {
			return 0;
		}
		List<AreaSpec> allAreas = ModConfig.getAllAreas();
		if (allAreas.isEmpty()) {
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.areas_empty"),
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
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.areas_header",
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
			context.getSource().sendFeedback(
				() -> Text.translatable(key, area.name,
					area.minX, area.minY, area.minZ,
					area.maxX, area.maxY, area.maxZ,
					area.volumeSaturated()),
				false
			);
		}
		if (pages > 1) {
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.areas_page", shownPage, pages),
				false
			);
		}
		return 1;
	}

	private static int executeInitialShow(CommandContext<ServerCommandSource> context) {
		AreaSpec area = ModConfig.getInitialDefaultArea();
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_initial_show",
				area.name,
				area.minX, area.minY, area.minZ,
				area.maxX, area.maxY, area.maxZ),
			false
		);
		return 1;
	}

	private static int executeInitialPos1(CommandContext<ServerCommandSource> context) {
		var vec3 = context.getSource().getPosition();
		BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
		AreaSpec area = ModConfig.getInitialDefaultArea();
		area.minX = pos.getX();
		area.minY = pos.getY();
		area.minZ = pos.getZ();
		if (!ModConfig.saveSettings()) {
			context.getSource().sendError(Text.translatable("rsl.message.area_save_failed"));
			return 0;
		}
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_initial_pos1", pos.getX(), pos.getY(), pos.getZ()),
			false
		);
		if (!ModConfig.isValidArea(area)) {
			context.getSource().sendFeedback(() -> Text.translatable("rsl.message.area_invalid_warning"), false);
		}
		return 1;
	}

	private static int executeInitialPos2(CommandContext<ServerCommandSource> context) {
		var vec3 = context.getSource().getPosition();
		BlockPos pos = new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
		AreaSpec area = ModConfig.getInitialDefaultArea();
		area.maxX = pos.getX();
		area.maxY = pos.getY();
		area.maxZ = pos.getZ();
		if (!ModConfig.saveSettings()) {
			context.getSource().sendError(Text.translatable("rsl.message.area_save_failed"));
			return 0;
		}
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_initial_pos2", pos.getX(), pos.getY(), pos.getZ()),
			false
		);
		if (!ModConfig.isValidArea(area)) {
			context.getSource().sendFeedback(() -> Text.translatable("rsl.message.area_invalid_warning"), false);
		}
		return 1;
	}

	private static int executeResetDefault(CommandContext<ServerCommandSource> context) {
		if (!ensureWorld(context.getSource()) || !DimensionGuard.requireActiveDimension(context.getSource())) {
			return 0;
		}
		if (!ModConfig.resetWorldDefaultToInitial()) {
			context.getSource().sendError(Text.translatable("rsl.message.area_reset_default_failed"));
			return 0;
		}
		AreaSpec area = ModConfig.getDefaultArea();
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.area_reset_default",
				area.name,
				area.minX, area.minY, area.minZ,
				area.maxX, area.maxY, area.maxZ),
			false
		);
		return 1;
	}

	private static boolean ensureWorld(ServerCommandSource source) {
		if (!ModConfig.isWorldLoaded()) {
			source.sendError(Text.translatable("rsl.message.world_not_initialized"));
			return false;
		}
		return true;
	}
}
