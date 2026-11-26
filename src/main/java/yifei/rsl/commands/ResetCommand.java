package yifei.rsl.commands;

import yifei.rsl.game.SoundFx;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.EggIslandSpawner;
import yifei.rsl.game.IslandGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ResetCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("randomskyblock")
				.then(CommandManager.literal("reset")
					.requires(source -> source.hasPermissionLevel(4))
					.executes(ResetCommand::execute)
				)
		);
	}
	
	private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		IslandGame game = RandomSkyBlockIsland.getIslandGame();
		
		if (game == null) {
			context.getSource().sendError(Text.translatable("rsl.message.game_not_initialized"));
			return 0;
		}
		
		if (!DimensionGuard.requireActiveDimension(context.getSource())) {
			return 0;
		}
		
		// 停止游戏（未运行时也会清理本地倒计时字段）
		game.stop();
		
		// 清理上一次生成的彩蛋岛
		new EggIslandSpawner(context.getSource().getServer()).clearRecordedIsland();
		
		// 把区域方块还原到第一次开始前的快照
		boolean restoring = game.beginRestore();
		
		// 重置运行时状态（保留区域和方块配置）
		ModConfig.setRunning(false);
		ModConfig.setSecondsRemaining(0);
		ModConfig.saveState();
		
		if (restoring) {
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.reset_restoring"),
				false
			);
			ServerPlayerEntity reseter = context.getSource().getPlayer();
			if (reseter != null) {
				SoundFx.play(reseter, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6F, 1.0F);
			}
		} else {
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.reset_no_snapshot"),
				false
			);
			context.getSource().sendFeedback(
				() -> Text.translatable("rsl.message.reset_done", 
					ModConfig.getAllAreas().size(), 
					ModConfig.getAllowedBlocks().size()),
				false
			);
			ServerPlayerEntity reseter = context.getSource().getPlayer();
			if (reseter != null) {
				SoundFx.play(reseter, SoundEvents.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
			}
		}
		
		return 1;
	}
}
