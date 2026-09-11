package yifei.rsl.commands;

import yifei.rsl.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 设置变换间隔命令
 *
 * 移植说明：
 * - CommandManager → Commands
 * - sendFeedback(supplier, false) → sendSuccess(supplier)
 */
public class DelayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("randomskyblock")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("delay")
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                        .executes(DelayCommand::execute)
                    )
                    .then(Commands.literal("show")
                        .executes(DelayCommand::executeShow)
                    )
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");

        ModConfig.transformDelaySeconds = seconds;
        ModConfig.saveConfig();

        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.delay_set", seconds), false);

        return 1;
    }

    private static int executeShow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        context.getSource().sendSuccess(() -> Component.translatable("rsl.message.delay_show", ModConfig.transformDelaySeconds), false);

        return 1;
    }
}
