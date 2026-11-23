package yifei.rsl.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import yifei.rsl.config.ModConfig;

/**
 * Restricts player commands to the dimension where the current game is locked.
 * Console commands are always allowed so an admin cannot lock themselves out.
 */
public final class DimensionGuard {
	private DimensionGuard() {
	}

	public static boolean requireActiveDimension(ServerCommandSource source) {
		if (!ModConfig.isDimensionLocked()) {
			return true;
		}
		ServerPlayerEntity player = source.getPlayer();
		if (player == null || ModConfig.isPlayerInActiveDimension(player)) {
			return true;
		}
		source.sendError(Text.translatable("rsl.message.dimension_locked", ModConfig.getActiveDimension()));
		return false;
	}
}
