package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ResetCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.requires(source -> source.hasPermissionLevel(4))
				.then(CommandManager.literal("reset")
					.executes(ResetCommand::execute)
				)
		);
	}
	
	private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		IslandGame game = RandomSkyBlockIsland.getIslandGame();
		
		if (game == null) {
			context.getSource().sendError(Text.translatable("rsl.message.game_not_initialized"));
			return 0;
		}
		
		// 停止游戏
		if (game.isRunning()) {
			game.stop();
		}
		
		// 重置运行时状态（保留区域和方块配置）
		ModConfig.isRunning = false;
		ModConfig.secondsRemaining = 0;
		ModConfig.saveConfig();
		
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.reset_done", 
				ModConfig.getAllAreas().size(), 
				ModConfig.allowedBlocks.size()),
			false
		);
		
		return 1;
	}
}
