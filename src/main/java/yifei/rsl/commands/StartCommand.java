package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class StartCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.then(CommandManager.literal("start")
					.requires(source -> source.hasPermissionLevel(4))
					.executes(StartCommand::execute)
				)
		);
	}
	
	private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		IslandGame game = RandomSkyBlockIsland.getIslandGame();
		
		if (game == null) {
			context.getSource().sendError(Text.translatable("rsl.message.game_not_initialized"));
			return 0;
		}
		
		if (!DimensionGuard.requireActiveDimension(context.getSource())) {
			return 0;
		}
		
		if (game.isRunning()) {
			context.getSource().sendError(Text.translatable("rsl.message.already_running"));
			return 0;
		}
		
		if (ModConfig.getUsableAreas().isEmpty()) {
			context.getSource().sendError(Text.translatable("rsl.message.no_valid_areas"));
			return 0;
		}
		
		ServerWorld world = context.getSource().getWorld();
		if (!game.start(world)) {
			if (ModConfig.isBlockPoolEmpty()) {
				context.getSource().sendError(Text.translatable("rsl.message.block_pool_empty_start"));
			} else if (ModConfig.getUsableAreas().isEmpty()) {
				context.getSource().sendError(Text.translatable("rsl.message.no_valid_areas"));
			} else {
				context.getSource().sendError(Text.translatable("rsl.message.start_failed"));
			}
			return 0;
		}
		game.getBossbar().addAllPlayers(context.getSource().getServer().getPlayerManager().getPlayerList());
		
		return 1;
	}
}
