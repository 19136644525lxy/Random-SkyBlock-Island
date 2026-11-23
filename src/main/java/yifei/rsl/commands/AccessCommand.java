package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import yifei.rsl.common.config.PoolAccessEntry;
import yifei.rsl.permission.PoolPermissions;
import yifei.rsl.config.ModConfig;

import java.util.List;

/**
 * Owner-only management of the block-pool access list. The list is stored in the
 * shared settings file and is deliberately independent of vanilla OP levels.
 */
public final class AccessCommand {
	private AccessCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.then(CommandManager.literal("access")
					.requires(PoolPermissions::canManage)
					.then(CommandManager.literal("list")
						.executes(AccessCommand::executeList))
					.then(CommandManager.literal("grant")
						.then(CommandManager.argument("player", EntityArgumentType.player())
							.executes(context -> executeGrant(context, false))))
					.then(CommandManager.literal("grantowner")
						.then(CommandManager.argument("player", EntityArgumentType.player())
							.executes(context -> executeGrant(context, true))))
					.then(CommandManager.literal("revoke")
						.then(CommandManager.argument("player", EntityArgumentType.player())
							.executes(AccessCommand::executeRevoke)))
				)
		);
	}

	private static int executeList(CommandContext<ServerCommandSource> context) {
		List<PoolAccessEntry> entries = ModConfig.getPoolAccess();
		context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.access_list_header", entries.size()), false);
		if (entries.isEmpty()) {
			context.getSource().sendFeedback(
					() -> Text.translatable("rsl.message.access_list_empty"), false);
			return 1;
		}
		for (PoolAccessEntry entry : entries) {
			Text role = Text.translatable(entry.role == PoolAccessEntry.Role.OWNER
					? "rsl.message.access_role_owner"
					: "rsl.message.access_role_editor");
			context.getSource().sendFeedback(
					() -> Text.translatable("rsl.message.access_list_entry",
							entry.name == null ? "-" : entry.name,
							entry.uuid, role), false);
		}
		return 1;
	}

	private static int executeGrant(CommandContext<ServerCommandSource> context, boolean owner) {
		ServerPlayerEntity target;
		try {
			target = EntityArgumentType.getPlayer(context, "player");
		} catch (Exception e) {
			context.getSource().sendError(Text.translatable("rsl.message.access_player_only"));
			return 0;
		}
		PoolAccessEntry existing = find(ModConfig.getPoolAccess(), target.getUuid().toString());
		boolean already = existing != null
				&& (owner ? existing.role == PoolAccessEntry.Role.OWNER : true);
		if (already) {
			context.getSource().sendFeedback(
					() -> Text.translatable("rsl.message.access_already", target.getGameProfile().getName()), false);
			return 0;
		}
		if (!ModConfig.grantPoolAccess(target, owner)) {
			context.getSource().sendError(Text.translatable("rsl.message.access_save_failed"));
			return 0;
		}
		String name = target.getGameProfile().getName();
		context.getSource().sendFeedback(
				() -> Text.translatable(owner
						? "rsl.message.access_owner_granted"
						: "rsl.message.access_granted", name), false);
		return 1;
	}

	private static int executeRevoke(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity target;
		try {
			target = EntityArgumentType.getPlayer(context, "player");
		} catch (Exception e) {
			context.getSource().sendError(Text.translatable("rsl.message.access_player_only"));
			return 0;
		}
		if (find(ModConfig.getPoolAccess(), target.getUuid().toString()) == null) {
			context.getSource().sendError(Text.translatable("rsl.message.access_not_found",
					target.getGameProfile().getName()));
			return 0;
		}
		if (!ModConfig.revokePoolAccess(target.getUuid())) {
			context.getSource().sendError(Text.translatable("rsl.message.access_save_failed"));
			return 0;
		}
		String name = target.getGameProfile().getName();
		context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.access_revoked", name), false);
		return 1;
	}

	private static PoolAccessEntry find(List<PoolAccessEntry> entries, String uuid) {
		for (PoolAccessEntry entry : entries) {
			if (entry != null && entry.uuid != null && entry.uuid.equalsIgnoreCase(uuid)) {
				return entry;
			}
		}
		return null;
	}
}
