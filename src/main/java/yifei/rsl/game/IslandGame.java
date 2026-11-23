package yifei.rsl.game;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.common.world.FillCursor;
import yifei.rsl.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class IslandGame {
	private final MinecraftServer server;
	private final Random random;
	private final IslandBossbar bossbar;
	private final ArrayDeque<PendingAreaFill> pendingFills = new ArrayDeque<>();
	private final List<String> completedSummaries = new ArrayList<>();
	
	private boolean isRunning = false;
	private int tickCount = 0;
	private int secondsRemaining = 0;
	private int stateFlushTicks = 0;
	
	public IslandGame(MinecraftServer server) {
		this.server = server;
		this.random = new Random();
		this.bossbar = new IslandBossbar();
		
		// 设置存档目录并加载存档区域
		ModConfig.setSaveDir(server.getSavePath(WorldSavePath.ROOT));
		ModConfig.loadSaveAreas();
		
		// A stale active dimension from a crashed run must not lock an idle save.
		if (!ModConfig.isRunning() && ModConfig.hasActiveDimension()) {
			ModConfig.clearActiveDimension();
			ModConfig.saveSaveAreas();
		}
		
		// 从配置恢复运行状态
		if (ModConfig.isRunning()) {
			ServerWorld activeWorld = ModConfig.findActiveWorld(server);
			boolean usable = !ModConfig.isBlockPoolEmpty()
					&& !ModConfig.getUsableAreas().isEmpty()
					&& activeWorld != null
					&& hasUsableAreaIn(activeWorld);
			if (!usable) {
				RandomSkyBlockIsland.LOGGER.warn(
						"Configured game state was running, but its dimension, block pool or area list is unusable; resetting state");
				ModConfig.setRunning(false);
				ModConfig.setSecondsRemaining(0);
				ModConfig.clearActiveDimension();
				ModConfig.saveState();
				ModConfig.saveSaveAreas();
			} else {
				this.isRunning = true;
				this.secondsRemaining = ModConfig.getSecondsRemaining();
				this.tickCount = 0;
				this.bossbar.createBossbar();
				updateBossbar();
				RandomSkyBlockIsland.LOGGER.info(
						"Restored island game in dimension {}", ModConfig.getActiveDimension());
			}
		}
	}
	
	public void tick() {
		if (!isRunning) {
			return;
		}
		
		processPendingFills();
		
		tickCount++;
		
		if (tickCount >= 20) {
			tickCount = 0;
			secondsRemaining = Math.max(0, secondsRemaining - 1);
			updateBossbar();
			
			ModConfig.setSecondsRemaining(secondsRemaining);
			stateFlushTicks++;
			if (stateFlushTicks >= 10) {
				stateFlushTicks = 0;
				ModConfig.saveState();
			}
			
			if (secondsRemaining == 0 && pendingFills.isEmpty()) {
				transformIsland();
			}
		}
	}
	
	public boolean start(ServerWorld world) {
		if (isRunning || world == null) {
			return false;
		}
		if (ModConfig.isBlockPoolEmpty()) {
			return false;
		}
		if (ModConfig.getUsableAreas().isEmpty() || !hasUsableAreaIn(world)) {
			return false;
		}
		
		if (!ModConfig.setActiveDimension(world.getRegistryKey().getValue().toString())) {
			return false;
		}
		if (!ModConfig.saveSaveAreas()) {
			ModConfig.clearActiveDimension();
			return false;
		}
		
		isRunning = true;
		tickCount = 0;
		stateFlushTicks = 0;
		secondsRemaining = ModConfig.getTransformDelaySeconds();
		pendingFills.clear();
		completedSummaries.clear();
		
		bossbar.createBossbar();
		updateBossbar();
		
		ModConfig.setRunning(true);
		ModConfig.setSecondsRemaining(secondsRemaining);
		ModConfig.saveState();
		
		RandomSkyBlockIsland.LOGGER.info(
				"Random SkyBlock Island game started in dimension {}", ModConfig.getActiveDimension());
		
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable("rsl.message.started", ModConfig.getTransformDelaySeconds()), false);
		}
		
		return true;
	}
	
	public boolean start() {
		return start(server.getWorld(World.OVERWORLD));
	}
	
	public void stop() {
		boolean wasRunning = isRunning;
		isRunning = false;
		secondsRemaining = 0;
		tickCount = 0;
		stateFlushTicks = 0;
		pendingFills.clear();
		completedSummaries.clear();
		if (wasRunning) {
			bossbar.removeBossbar();
		}
		
		ModConfig.setRunning(false);
		ModConfig.setSecondsRemaining(0);
		ModConfig.clearActiveDimension();
		ModConfig.saveState();
		if (!ModConfig.saveSaveAreas()) {
			RandomSkyBlockIsland.LOGGER.warn("Failed to persist cleared active dimension");
		}
		
		if (wasRunning) {
			RandomSkyBlockIsland.LOGGER.info("Random SkyBlock Island game stopped!");
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				player.sendMessage(Text.translatable("rsl.message.stopped"), false);
			}
		}
	}
	
	private boolean hasUsableAreaIn(ServerWorld world) {
		for (AreaSpec area : ModConfig.getUsableAreas()) {
			if (!new FillCursor(area, world.getBottomY(), world.getTopY()).isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
	private void failMissingDimension() {
		RandomSkyBlockIsland.LOGGER.error(
				"Active dimension {} is not loaded; stopping the game", ModConfig.getActiveDimension());
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable("rsl.message.dimension_missing"), false);
		}
		stop();
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	private void transformIsland() {
		ServerWorld world = ModConfig.findActiveWorld(server);
		if (world == null) {
			failMissingDimension();
			return;
		}
		
		List<Block> pool = ModConfig.getAllowedBlocks();
		if (pool.isEmpty()) {
			RandomSkyBlockIsland.LOGGER.error("Block pool is empty; stopping the game");
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				player.sendMessage(Text.translatable("rsl.message.block_pool_empty"), false);
			}
			stop();
			return;
		}
		
		List<AreaSpec> areas = ModConfig.getUsableAreas();
		if (areas.isEmpty()) {
			RandomSkyBlockIsland.LOGGER.error("No usable areas; stopping the game");
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				player.sendMessage(Text.translatable("rsl.message.no_valid_areas"), false);
			}
			stop();
			return;
		}
		
		completedSummaries.clear();
		int worldBottom = world.getBottomY();
		int worldTop = world.getTopY();
		
		for (AreaSpec area : areas) {
			Block selectedBlock = pool.get(random.nextInt(pool.size()));
			BlockState state = selectedBlock.getDefaultState();
			FillCursor cursor = new FillCursor(area, worldBottom, worldTop);
			
			if (cursor.isEmpty()) {
				RandomSkyBlockIsland.LOGGER.warn("Area {} is entirely outside the world height; skipping", area.name);
				continue;
			}
			
			pendingFills.addLast(new PendingAreaFill(area, selectedBlock, state, cursor));
		}
		
		secondsRemaining = ModConfig.getTransformDelaySeconds();
		stateFlushTicks = 0;
		updateBossbar();
		
		ModConfig.setSecondsRemaining(secondsRemaining);
		ModConfig.saveState();
		
		// 小区域在当前 tick 内直接完成，保持旧版的即时反馈
		processPendingFills();
	}
	
	private void processPendingFills() {
		if (pendingFills.isEmpty()) {
			return;
		}
		
		ServerWorld world = ModConfig.findActiveWorld(server);
		if (world == null) {
			pendingFills.clear();
			completedSummaries.clear();
			failMissingDimension();
			return;
		}
		
		int budget = Math.max(1, ModConfig.getBlocksPerTick());
		Iterator<PendingAreaFill> iterator = pendingFills.iterator();
		
		while (iterator.hasNext() && budget > 0) {
			PendingAreaFill fill = iterator.next();
			
			while (budget > 0 && fill.cursor.hasNext()) {
				world.setBlockState(
					new BlockPos(fill.cursor.x(), fill.cursor.y(), fill.cursor.z()),
					fill.state,
					3
				);
				fill.cursor.advance();
				budget--;
			}
			
			if (!fill.cursor.hasNext()) {
				iterator.remove();
				onAreaFilled(world, fill);
			}
		}
		
		if (pendingFills.isEmpty()) {
			sendSummaryIfNeeded();
		}
	}
	
	private void onAreaFilled(ServerWorld world, PendingAreaFill fill) {
		String summary = fill.area.name + "=" + fill.block.getName().getString();
		completedSummaries.add(summary);
		RandomSkyBlockIsland.LOGGER.info("Island area transformed: {}", summary);
		
		String mode = ModConfig.getBroadcastMode();
		if ("summary".equals(mode)) {
			return;
		}
		
		if ("nearby".equals(mode)) {
			double centerX = (fill.area.minX + (double) fill.area.maxX) / 2.0;
			double centerY = (fill.area.minY + (double) fill.area.maxY) / 2.0;
			double centerZ = (fill.area.minZ + (double) fill.area.maxZ) / 2.0;
			double radius = ModConfig.getBroadcastRadius();
			double radiusSq = radius * radius;
			
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (player.getWorld() == world && player.squaredDistanceTo(centerX, centerY, centerZ) <= radiusSq) {
					player.sendMessage(Text.translatable("rsl.message.changed.area", fill.area.name, fill.block.getName()), false);
				}
			}
			return;
		}
		
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable("rsl.message.changed.area", fill.area.name, fill.block.getName()), false);
		}
	}
	
	private void sendSummaryIfNeeded() {
		if (!"summary".equals(ModConfig.getBroadcastMode()) || completedSummaries.isEmpty()) {
			return;
		}
		
		String joined = String.join(", ", completedSummaries);
		completedSummaries.clear();
		
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable("rsl.message.changed.summary", joined), false);
		}
	}
	
	private void updateBossbar() {
		if (isRunning) {
			bossbar.update(secondsRemaining, ModConfig.getTransformDelaySeconds());
		}
	}
	
	public void updateDelay() {
		if (isRunning) {
			secondsRemaining = Math.min(secondsRemaining, ModConfig.getTransformDelaySeconds());
			ModConfig.setSecondsRemaining(secondsRemaining);
			ModConfig.saveState();
			updateBossbar();
		}
	}
	
	public int getSecondsRemaining() {
		return secondsRemaining;
	}
	
	public int getPendingFillCount() {
		return pendingFills.size();
	}
	
	public long getPendingBlockCount() {
		long remaining = 0L;
		for (PendingAreaFill fill : pendingFills) {
			remaining += fill.cursor.remaining();
		}
		return remaining;
	}
	
	public boolean hasPendingFills() {
		return !pendingFills.isEmpty();
	}
	
	public void onConfigReloaded() {
		if (isRunning) {
			secondsRemaining = Math.min(secondsRemaining, ModConfig.getTransformDelaySeconds());
			ModConfig.setSecondsRemaining(secondsRemaining);
			ModConfig.saveState();
			updateBossbar();
		}
	}
	
	public IslandBossbar getBossbar() {
		return bossbar;
	}
	
	private static final class PendingAreaFill {
		private final AreaSpec area;
		private final Block block;
		private final BlockState state;
		private final FillCursor cursor;
		
		private PendingAreaFill(AreaSpec area, Block block, BlockState state, FillCursor cursor) {
			this.area = area;
			this.block = block;
			this.state = state;
			this.cursor = cursor;
		}
	}
}
