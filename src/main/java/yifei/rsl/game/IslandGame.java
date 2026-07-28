package yifei.rsl.game;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.config.ModConfig;
import yifei.rsl.config.TransformArea;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

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
		
		int blockIndex = random.nextInt(ModConfig.allowedBlocks.size());
		var selectedBlock = ModConfig.allowedBlocks.get(blockIndex);
		
		for (TransformArea area : ModConfig.areas) {
			BlockReplacer.fillArea(world,
				area.minX, area.minY, area.minZ,
				area.maxX, area.maxY, area.maxZ,
				selectedBlock.getDefaultState()
			);
		}
		
		secondsRemaining = ModConfig.transformDelaySeconds;
		updateBossbar();
		
		String blockName = selectedBlock.getName().getString();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable("rsl.message.changed", selectedBlock.getName()), false);
		}
		
		RandomSkyBlockIsland.LOGGER.info("Island transformed to: {}", blockName);
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
	
	public IslandBossbar getBossbar() {
		return bossbar;
	}
}
