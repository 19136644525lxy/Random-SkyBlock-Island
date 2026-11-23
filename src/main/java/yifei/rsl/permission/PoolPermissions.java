package yifei.rsl.permission;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
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

	public static boolean canUse(ServerCommandSource source) {
		if (source == null) {
			return false;
		}
		ServerPlayerEntity player = source.getPlayer();
		return player == null || canUse(player);
	}

	public static boolean canUse(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		return isHost(player) || PoolAccess.canUse(ModConfig.getPoolAccess(), player.getUuid());
	}

	public static boolean canManage(ServerCommandSource source) {
		if (source == null) {
			return false;
		}
		ServerPlayerEntity player = source.getPlayer();
		return player == null || canManage(player);
	}

	public static boolean canManage(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		return isHost(player) || PoolAccess.isOwner(ModConfig.getPoolAccess(), player.getUuid());
	}

	private static boolean isHost(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		return server != null && server.isHost(player.getGameProfile());
	}
}
