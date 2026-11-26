package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import yifei.rsl.config.ModConfig;

/**
 * Toggles the mod's feedback sounds and adjusts their master volume.
 * Permission 4 keeps it next to the other game-admin commands.
 */
public class SoundCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.then(CommandManager.literal("sound")
					.requires(source -> source.hasPermissionLevel(4))
					.executes(SoundCommand::executeStatus)
					.then(CommandManager.literal("on")
						.executes(context -> setEnabled(context, true))
					)
					.then(CommandManager.literal("off")
						.executes(context -> setEnabled(context, false))
					)
					.then(CommandManager.literal("volume")
						.then(CommandManager.argument("percent", IntegerArgumentType.integer(0, 100))
							.executes(SoundCommand::executeVolume)
						)
					)
				)
		);
	}

	private static int executeStatus(CommandContext<ServerCommandSource> context) {
		String state = ModConfig.isSoundEnabled() ? "on" : "off";
		int percent = (int) Math.round(ModConfig.getSoundVolume() * 100.0);
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.sound_status",
				Text.translatable("rsl.message.sound_" + state), percent),
			false
		);
		return 1;
	}

	private static int setEnabled(CommandContext<ServerCommandSource> context, boolean enabled) {
		ModConfig.setSoundEnabled(enabled);
		context.getSource().sendFeedback(
			() -> Text.translatable(enabled ? "rsl.message.sound_enabled" : "rsl.message.sound_disabled"),
			false
		);
		return 1;
	}

	private static int executeVolume(CommandContext<ServerCommandSource> context) {
		int percent = IntegerArgumentType.getInteger(context, "percent");
		ModConfig.setSoundVolume(percent);
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.sound_volume_set", percent),
			false
		);
		return 1;
	}
}
