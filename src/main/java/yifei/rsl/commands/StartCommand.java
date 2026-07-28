package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class StartCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.requires(source -> source.hasPermissionLevel(4))
				.then(CommandManager.literal("start")
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
		
		if (game.isRunning()) {
			context.getSource().sendError(Text.translatable("rsl.message.already_running"));
			return 0;
		}
		
		game.start();
		game.getBossbar().addAllPlayers(context.getSource().getServer().getPlayerManager().getPlayerList());
		
		return 1;
	}
}
