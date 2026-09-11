package yifei.rsl.game;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.config.TransformArea;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class IslandGame {
	private final MinecraftServer server;
	private final Random random;
	private final IslandBossbar bossbar;
	
	private boolean isRunning = false;
	private int tickCount = 0;
	private int secondsRemaining = 0;
	
	public IslandGame(MinecraftServer server) {
		this.server = server;
		this.random = new Random();
		this.bossbar = new IslandBossbar();
		
		// 设置存档目录并加载存档区域
		ModConfig.setSaveDir(server.getSavePath(WorldSavePath.ROOT));
		ModConfig.loadSaveAreas();
		
		// 从配置恢复运行状态
		if (ModConfig.isRunning) {
			this.isRunning = true;
			this.secondsRemaining = ModConfig.secondsRemaining;
			this.tickCount = 0;
			this.bossbar.createBossbar();
			updateBossbar();
		}
	}
	
	public void tick() {
		if (!isRunning) {
			return;
		}
		
		tickCount++;
		
		if (tickCount >= 20) {
			tickCount = 0;
			secondsRemaining--;
			updateBossbar();
			
			if (secondsRemaining <= 0) {
				transformIsland();
			} else {
				ModConfig.secondsRemaining = secondsRemaining;
				ModConfig.saveConfig();
			}
		}
	}
	
	public void start() {
		if (isRunning) {
			return;
		}
		
		isRunning = true;
		tickCount = 0;
		secondsRemaining = ModConfig.transformDelaySeconds;
		
		bossbar.createBossbar();
		updateBossbar();
		
		ModConfig.isRunning = true;
		ModConfig.secondsRemaining = secondsRemaining;
		ModConfig.saveConfig();
		
		RandomSkyBlockIsland.LOGGER.info("Random SkyBlock Island game started!");
		
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable("rsl.message.started", ModConfig.transformDelaySeconds), false);
		}
	}
	
	public void stop() {
		if (!isRunning) {
			return;
		}
		
		isRunning = false;
		bossbar.removeBossbar();
		
		ModConfig.isRunning = false;
		ModConfig.secondsRemaining = 0;
		ModConfig.saveConfig();
		
		RandomSkyBlockIsland.LOGGER.info("Random SkyBlock Island game stopped!");
		
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable("rsl.message.stopped"), false);
		}
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	private void transformIsland() {
		World world = server.getWorld(World.OVERWORLD);
		if (world == null) {
			RandomSkyBlockIsland.LOGGER.warn("Overworld not found!");
			return;
		}
		
		StringBuilder blockNames = new StringBuilder();
		
		for (TransformArea area : ModConfig.getAllAreas()) {
			int blockIndex = random.nextInt(ModConfig.allowedBlocks.size());
			var selectedBlock = ModConfig.allowedBlocks.get(blockIndex);
			
			BlockReplacer.fillArea(world,
				area.minX, area.minY, area.minZ,
				area.maxX, area.maxY, area.maxZ,
				selectedBlock.getDefaultState()
			);
			
			if (blockNames.length() > 0) {
				blockNames.append(", ");
			}
			blockNames.append(area.name).append("=").append(selectedBlock.getName().getString());
			
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				player.sendMessage(Text.translatable("rsl.message.changed.area", area.name, selectedBlock.getName()), false);
			}
		}
		
		secondsRemaining = ModConfig.transformDelaySeconds;
		updateBossbar();
		
		ModConfig.secondsRemaining = secondsRemaining;
		ModConfig.saveConfig();
		
		RandomSkyBlockIsland.LOGGER.info("Island transformed: {}", blockNames);
	}
	
	private void updateBossbar() {
		if (isRunning) {
			bossbar.update(secondsRemaining, ModConfig.transformDelaySeconds);
		}
	}
	
	public void updateDelay() {
		if (isRunning) {
			secondsRemaining = Math.min(secondsRemaining, ModConfig.transformDelaySeconds);
		}
	}
	
	public int getSecondsRemaining() {
		return secondsRemaining;
	}
	
	public IslandBossbar getBossbar() {
		return bossbar;
	}
}
