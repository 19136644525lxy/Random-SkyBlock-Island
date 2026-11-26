package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import yifei.rsl.config.ModConfig;

/**
 * Forge mirror of the Fabric SoundCommand.
 */
public class SoundCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("randomskyblock")
				.then(Commands.literal("sound")
					.requires(source -> source.hasPermission(4))
					.executes(SoundCommand::executeStatus)
					.then(Commands.literal("on")
						.executes(context -> setEnabled(context, true))
					)
					.then(Commands.literal("off")
						.executes(context -> setEnabled(context, false))
					)
					.then(Commands.literal("volume")
						.then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
							.executes(SoundCommand::executeVolume)
						)
					)
				)
		);
	}

	private static int executeStatus(CommandContext<CommandSourceStack> context) {
		String state = ModConfig.isSoundEnabled() ? "on" : "off";
		int percent = (int) Math.round(ModConfig.getSoundVolume() * 100.0);
		context.getSource().sendSuccess(
			() -> Component.translatable("rsl.message.sound_status",
				Component.translatable("rsl.message.sound_" + state), percent),
			false
		);
		return 1;
	}

	private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		ModConfig.setSoundEnabled(enabled);
		context.getSource().sendSuccess(
			() -> Component.translatable(enabled ? "rsl.message.sound_enabled" : "rsl.message.sound_disabled"),
			false
		);
		return 1;
	}

	private static int executeVolume(CommandContext<CommandSourceStack> context) {
		int percent = IntegerArgumentType.getInteger(context, "percent");
		ModConfig.setSoundVolume(percent);
		context.getSource().sendSuccess(
			() -> Component.translatable("rsl.message.sound_volume_set", percent),
			false
		);
		return 1;
	}
}
