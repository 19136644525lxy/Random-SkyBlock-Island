package yifei.rsl.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import yifei.rsl.common.config.PoolAccess;
import yifei.rsl.config.ModConfig;

/**
 * Server-authoritative permission checks for the block-pool editor. Vanilla
 * permission levels are intentionally not consulted: a level-4 operator who is
 * not in the ACL is still denied.
 */
public final class PoolPermissions {
	private PoolPermissions() {
	}

	public static boolean canUse(CommandSourceStack source) {
		if (source == null) {
			return false;
		}
		ServerPlayer player = source.getPlayer();
		return player == null || canUse(player);
	}

	public static boolean canUse(ServerPlayer player) {
		if (player == null) {
			return false;
		}
		return isHost(player) || PoolAccess.canUse(ModConfig.getPoolAccess(), player.getUUID());
	}

	public static boolean canManage(CommandSourceStack source) {
		if (source == null) {
			return false;
		}
		ServerPlayer player = source.getPlayer();
		return player == null || canManage(player);
	}

	public static boolean canManage(ServerPlayer player) {
		if (player == null) {
			return false;
		}
		return isHost(player) || PoolAccess.isOwner(ModConfig.getPoolAccess(), player.getUUID());
	}

	private static boolean isHost(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		return server != null && server.isSingleplayerOwner(player.getGameProfile());
	}
}
