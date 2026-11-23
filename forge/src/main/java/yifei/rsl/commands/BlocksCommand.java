package yifei.rsl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import yifei.rsl.common.block.BlockId;
import yifei.rsl.common.config.BlockMutationResult;
import yifei.rsl.config.ModConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 全局方块池管理命令（Forge 映射）。
 */
public final class BlocksCommand {
    private static final int PAGE_SIZE = 20;

    private BlocksCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("randomskyblock")
                .then(Commands.literal("blocks")
                    .requires(yifei.rsl.permission.PoolPermissions::canUse)
                    .then(Commands.literal("list")
                        .executes(context -> executeList(context, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(context -> executeList(context, IntegerArgumentType.getInteger(context, "page")))
                        )
                    )
                    .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .suggests(BlocksCommand::suggestAllBlocks)
                            .executes(BlocksCommand::executeAdd)
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .suggests(BlocksCommand::suggestPoolBlocks)
                            .executes(BlocksCommand::executeRemove)
                        )
                    )
                    .then(Commands.literal("clear")
                        .executes(BlocksCommand::executeClearPrompt)
                        .then(Commands.literal("confirm")
                            .executes(BlocksCommand::executeClear)
                        )
                    )
                    .then(Commands.literal("reset")
                        .executes(BlocksCommand::executeReset)
                    )
                )
        );
    }

    private static int executeList(CommandContext<CommandSourceStack> context, int requestedPage) {
        List<String> ids = ModConfig.getAllowedBlockIds();
        if (ids.isEmpty()) {
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.blocks_list_empty"),
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

        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.blocks_list_header",
                ids.size(), invalid.size(), currentPage, pages),
            false
        );

        for (int i = start; i < end; i++) {
            String id = ids.get(i);
            if (invalid.contains(id)) {
                context.getSource().sendSuccess(
                    () -> Component.translatable("rsl.message.blocks_list_item_invalid", id),
                    false
                );
            } else {
                context.getSource().sendSuccess(
                    () -> Component.translatable("rsl.message.blocks_list_item", id),
                    false
                );
            }
        }

        return 1;
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        BlockMutationResult result = ModConfig.addBlockId(id);

        if (result == BlockMutationResult.OK) {
            String shownId = BlockId.normalize(id);
            final String display = shownId != null ? shownId : id;
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.blocks_added", display),
                false
            );
            return 1;
        }

        context.getSource().sendFailure(errorMessage(result));
        return 0;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        BlockMutationResult result = ModConfig.removeBlockId(id);

        if (result == BlockMutationResult.OK) {
            String shownId = BlockId.normalize(id);
            final String display = shownId != null ? shownId : id;
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.blocks_removed", display),
                false
            );
            return 1;
        }

        context.getSource().sendFailure(errorMessage(result));
        return 0;
    }

    private static int executeClearPrompt(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
            () -> Component.translatable("rsl.message.blocks_clear_confirm"),
            false
        );
        return 1;
    }

    private static int executeClear(CommandContext<CommandSourceStack> context) {
        BlockMutationResult result = ModConfig.clearBlockIds();
        if (result == BlockMutationResult.OK) {
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.blocks_cleared"),
                false
            );
            return 1;
        }
        context.getSource().sendFailure(errorMessage(result));
        return 0;
    }

    private static int executeReset(CommandContext<CommandSourceStack> context) {
        BlockMutationResult result = ModConfig.resetBlockIds();
        if (result == BlockMutationResult.OK) {
            context.getSource().sendSuccess(
                () -> Component.translatable("rsl.message.blocks_reset", ModConfig.getAllowedBlockIds().size()),
                false
            );
            return 1;
        }
        context.getSource().sendFailure(errorMessage(result));
        return 0;
    }

    private static Component errorMessage(BlockMutationResult result) {
        return switch (result) {
            case INVALID_ID -> Component.translatable("rsl.message.blocks_invalid_id");
            case UNKNOWN_BLOCK -> Component.translatable("rsl.message.blocks_unknown");
            case AIR_BLOCK -> Component.translatable("rsl.message.blocks_air");
            case DUPLICATE -> Component.translatable("rsl.message.blocks_duplicate");
            case POOL_FULL -> Component.translatable("rsl.message.blocks_pool_full");
            case WOULD_EMPTY_RUNNING -> Component.translatable("rsl.message.blocks_would_empty");
            case NOT_FOUND -> Component.translatable("rsl.message.blocks_not_found");
            case IO_ERROR -> Component.translatable("rsl.message.blocks_save_failed");
            case OK -> Component.translatable("rsl.message.blocks_save_failed");
        };
    }

    private static CompletableFuture<Suggestions> suggestAllBlocks(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            String value = id.toString();
            if (value.startsWith(remaining)) {
                builder.suggest(value);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPoolBlocks(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (String id : ModConfig.getAllowedBlockIds()) {
            if (id.startsWith(remaining)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    }
}
