package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import yifei.rsl.RandomSkyBlockIsland;

public final class WandCommand {
	private WandCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.then(CommandManager.literal("wand")
					.requires(source -> source.hasPermissionLevel(4))
					.executes(WandCommand::execute)
				)
		);
	}

	private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		ItemStack stack = new ItemStack(RandomSkyBlockIsland.SELECTION_WAND);
		if (!player.getInventory().insertStack(stack)) {
			player.dropItem(stack, false);
		}
		context.getSource().sendFeedback(() -> Text.translatable("rsl.message.wand_given"), false);
		return 1;
	}
}
