package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import yifei.rsl.common.config.PoolAccessEntry;
import yifei.rsl.config.ModConfig;
import yifei.rsl.permission.PoolPermissions;

import java.util.List;

/**
 * Owner-only management of the block-pool access list. The list is stored in the
 * shared settings file and is deliberately independent of vanilla OP levels.
 */
public final class AccessCommand {
	private AccessCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("randomskyblock")
				.then(Commands.literal("access")
					.requires(PoolPermissions::canManage)
					.then(Commands.literal("list")
						.executes(AccessCommand::executeList))
					.then(Commands.literal("grant")
						.then(Commands.argument("player", EntityArgument.player())
							.executes(context -> executeGrant(context, false))))
					.then(Commands.literal("grantowner")
						.then(Commands.argument("player", EntityArgument.player())
							.executes(context -> executeGrant(context, true))))
					.then(Commands.literal("revoke")
						.then(Commands.argument("player", EntityArgument.player())
							.executes(AccessCommand::executeRevoke)))
				)
		);
	}

	private static int executeList(CommandContext<CommandSourceStack> context) {
		List<PoolAccessEntry> entries = ModConfig.getPoolAccess();
		context.getSource().sendSuccess(
				() -> Component.translatable("rsl.message.access_list_header", entries.size()), false);
		if (entries.isEmpty()) {
			context.getSource().sendSuccess(
					() -> Component.translatable("rsl.message.access_list_empty"), false);
			return 1;
		}
		for (PoolAccessEntry entry : entries) {
			Component role = Component.translatable(entry.role == PoolAccessEntry.Role.OWNER
					? "rsl.message.access_role_owner"
					: "rsl.message.access_role_editor");
			context.getSource().sendSuccess(
					() -> Component.translatable("rsl.message.access_list_entry",
							entry.name == null ? "-" : entry.name,
							entry.uuid, role), false);
		}
		return 1;
	}

	private static int executeGrant(CommandContext<CommandSourceStack> context, boolean owner) {
		ServerPlayer target;
		try {
			target = EntityArgument.getPlayer(context, "player");
		} catch (CommandSyntaxException e) {
			context.getSource().sendFailure(Component.translatable("rsl.message.access_player_only"));
			return 0;
		}
		PoolAccessEntry existing = find(ModConfig.getPoolAccess(), target.getUUID().toString());
		boolean already = existing != null
				&& (owner ? existing.role == PoolAccessEntry.Role.OWNER : true);
		if (already) {
			context.getSource().sendSuccess(
					() -> Component.translatable("rsl.message.access_already",
							target.getGameProfile().getName()), false);
			return 0;
		}
		if (!ModConfig.grantPoolAccess(target, owner)) {
			context.getSource().sendFailure(Component.translatable("rsl.message.access_save_failed"));
			return 0;
		}
		String name = target.getGameProfile().getName();
		context.getSource().sendSuccess(
				() -> Component.translatable(owner
						? "rsl.message.access_owner_granted"
						: "rsl.message.access_granted", name), false);
		return 1;
	}

	private static int executeRevoke(CommandContext<CommandSourceStack> context) {
		ServerPlayer target;
		try {
			target = EntityArgument.getPlayer(context, "player");
		} catch (CommandSyntaxException e) {
			context.getSource().sendFailure(Component.translatable("rsl.message.access_player_only"));
			return 0;
		}
		if (find(ModConfig.getPoolAccess(), target.getUUID().toString()) == null) {
			context.getSource().sendFailure(Component.translatable("rsl.message.access_not_found",
					target.getGameProfile().getName()));
			return 0;
		}
		if (!ModConfig.revokePoolAccess(target.getUUID())) {
			context.getSource().sendFailure(Component.translatable("rsl.message.access_save_failed"));
			return 0;
		}
		String name = target.getGameProfile().getName();
		context.getSource().sendSuccess(
				() -> Component.translatable("rsl.message.access_revoked", name), false);
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
