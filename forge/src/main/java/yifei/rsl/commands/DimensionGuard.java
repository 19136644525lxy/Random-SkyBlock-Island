package yifei.rsl.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import yifei.rsl.config.ModConfig;

/**
 * Restricts player commands to the dimension where the current game is locked.
 * Console commands are always allowed so an admin cannot lock themselves out.
 */
public final class DimensionGuard {
    private DimensionGuard() {
    }

    public static boolean requireActiveDimension(CommandSourceStack source) {
        if (!ModConfig.isDimensionLocked()) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null || ModConfig.isPlayerInActiveDimension(player)) {
            return true;
        }
        source.sendFailure(Component.translatable("rsl.message.dimension_locked", ModConfig.getActiveDimension()));
        return false;
    }
}
