package yifei.rsl.commands;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class DelayCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.requires(source -> source.hasPermissionLevel(4))
				.then(CommandManager.literal("delay")
					.then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 3600))
						.executes(DelayCommand::execute)
					)
					.then(CommandManager.literal("show")
						.executes(DelayCommand::executeShow)
					)
				)
		);
	}
	
	private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		int seconds = IntegerArgumentType.getInteger(context, "seconds");
		
		ModConfig.transformDelaySeconds = seconds;
		ModConfig.saveConfig();
		
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.delay_set", seconds),
			false
		);
		
		return 1;
	}
	
	private static int executeShow(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.delay_show", ModConfig.transformDelaySeconds),
			false
		);
		
		return 1;
	}
}
