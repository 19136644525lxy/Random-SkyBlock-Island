package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class StopCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.then(CommandManager.literal("stop")
					.requires(source -> source.hasPermissionLevel(4))
					.executes(StopCommand::execute)
				)
		);
	}
	
	private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		IslandGame game = RandomSkyBlockIsland.getIslandGame();
		
		if (game == null) {
			context.getSource().sendError(Text.translatable("rsl.message.game_not_initialized"));
			return 0;
		}
		
		if (!game.isRunning()) {
			context.getSource().sendError(Text.translatable("rsl.message.not_running"));
			return 0;
		}
		
		game.stop();
		
		return 1;
	}
}
