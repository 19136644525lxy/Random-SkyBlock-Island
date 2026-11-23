package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import yifei.rsl.common.block.BlockId;
import yifei.rsl.common.config.BlockMutationResult;
import yifei.rsl.config.ModConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 全局方块池管理命令。
 *
 * <p>方块池保存的是方块 id 字符串，因此可以包含任意已安装模组注册的方块；
 * 真正解析为 {@code Block} 在运行时完成，缺失的 id 会保留并标记。</p>
 */
public final class BlocksCommand {
	private static final int PAGE_SIZE = 20;

	private BlocksCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.then(CommandManager.literal("blocks")
					.requires(source -> source.hasPermissionLevel(ModConfig.getBlocksPermissionLevel()))
					.then(CommandManager.literal("list")
						.executes(context -> executeList(context, 1))
						.then(CommandManager.argument("page", IntegerArgumentType.integer(1))
							.executes(context -> executeList(context, IntegerArgumentType.getInteger(context, "page")))
						)
					)
					.then(CommandManager.literal("add")
						.then(CommandManager.argument("id", StringArgumentType.word())
							.suggests(BlocksCommand::suggestAllBlocks)
							.executes(BlocksCommand::executeAdd)
						)
					)
					.then(CommandManager.literal("remove")
						.then(CommandManager.argument("id", StringArgumentType.word())
							.suggests(BlocksCommand::suggestPoolBlocks)
							.executes(BlocksCommand::executeRemove)
						)
					)
					.then(CommandManager.literal("clear")
						.executes(BlocksCommand::executeClearPrompt)
						.then(CommandManager.literal("confirm")
							.executes(BlocksCommand::executeClear)
						)
					)
					.then(CommandManager.literal("reset")
						.executes(BlocksCommand::executeReset)
					)
				)
		);
	}

	private static int executeList(CommandContext<ServerCommandSource> context, int requestedPage) {
		List<String> ids = ModConfig.getAllowedBlockIds();
		if (ids.isEmpty()) {
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.blocks_list_empty"),
				false
			);
			return 1;
		}

		Set<String> invalid = new HashSet<>(ModConfig.getInvalidBlockIds());
		int pages = (ids.size() + PAGE_SIZE - 1) / PAGE_SIZE;
		int page = Math.max(1, Math.min(requestedPage, pages));
		int start = (page - 1) * PAGE_SIZE;
		int end = Math.min(start + PAGE_SIZE, ids.size());
		final int currentPage = page;

		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.blocks_list_header",
				ids.size(), invalid.size(), currentPage, pages),
			false
		);

		for (int i = start; i < end; i++) {
			String id = ids.get(i);
			if (invalid.contains(id)) {
				context.getSource().sendFeedback(
					() -> Text.translatable("rsl.message.blocks_list_item_invalid", id),
					false
				);
			} else {
				context.getSource().sendFeedback(
					() -> Text.translatable("rsl.message.blocks_list_item", id),
					false
				);
			}
		}

		return 1;
	}

	private static int executeAdd(CommandContext<ServerCommandSource> context) {
		String id = StringArgumentType.getString(context, "id");
		BlockMutationResult result = ModConfig.addBlockId(id);

		if (result == BlockMutationResult.OK) {
			String shownId = BlockId.normalize(id);
			final String display = shownId != null ? shownId : id;
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.blocks_added", display),
				false
			);
			return 1;
		}

		context.getSource().sendError(errorMessage(result));
		return 0;
	}

	private static int executeRemove(CommandContext<ServerCommandSource> context) {
		String id = StringArgumentType.getString(context, "id");
		BlockMutationResult result = ModConfig.removeBlockId(id);

		if (result == BlockMutationResult.OK) {
			String shownId = BlockId.normalize(id);
			final String display = shownId != null ? shownId : id;
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.blocks_removed", display),
				false
			);
			return 1;
		}

		context.getSource().sendError(errorMessage(result));
		return 0;
	}

	private static int executeClearPrompt(CommandContext<ServerCommandSource> context) {
		context.getSource().sendFeedback(
			() -> Text.translatable("rsl.message.blocks_clear_confirm"),
			false
		);
		return 1;
	}

	private static int executeClear(CommandContext<ServerCommandSource> context) {
		BlockMutationResult result = ModConfig.clearBlockIds();
		if (result == BlockMutationResult.OK) {
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.blocks_cleared"),
				false
			);
			return 1;
		}
		context.getSource().sendError(errorMessage(result));
		return 0;
	}

	private static int executeReset(CommandContext<ServerCommandSource> context) {
		BlockMutationResult result = ModConfig.resetBlockIds();
		if (result == BlockMutationResult.OK) {
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.blocks_reset", ModConfig.getAllowedBlockIds().size()),
				false
			);
			return 1;
		}
		context.getSource().sendError(errorMessage(result));
		return 0;
	}

	private static Text errorMessage(BlockMutationResult result) {
		return switch (result) {
			case INVALID_ID -> Text.translatable("rsl.message.blocks_invalid_id");
			case UNKNOWN_BLOCK -> Text.translatable("rsl.message.blocks_unknown");
			case AIR_BLOCK -> Text.translatable("rsl.message.blocks_air");
			case DUPLICATE -> Text.translatable("rsl.message.blocks_duplicate");
			case POOL_FULL -> Text.translatable("rsl.message.blocks_pool_full");
			case WOULD_EMPTY_RUNNING -> Text.translatable("rsl.message.blocks_would_empty");
			case NOT_FOUND -> Text.translatable("rsl.message.blocks_not_found");
			case IO_ERROR -> Text.translatable("rsl.message.blocks_save_failed");
			case OK -> Text.translatable("rsl.message.blocks_save_failed");
		};
	}

	private static CompletableFuture<Suggestions> suggestAllBlocks(
			CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		String remaining = builder.getRemainingLowerCase();
		for (Identifier id : Registries.BLOCK.getIds()) {
			String value = id.toString();
			if (value.startsWith(remaining)) {
				builder.suggest(value);
			}
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestPoolBlocks(
			CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		String remaining = builder.getRemainingLowerCase();
		for (String id : ModConfig.getAllowedBlockIds()) {
			if (id.startsWith(remaining)) {
				builder.suggest(id);
			}
		}
		return builder.buildFuture();
	}
}
