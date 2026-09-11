package yifei.rsl;

import yifei.rsl.commands.StartCommand;
import yifei.rsl.commands.StopCommand;
import yifei.rsl.commands.AreaCommand;
import yifei.rsl.commands.DelayCommand;
import yifei.rsl.commands.ResetCommand;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomSkyBlockIsland implements ModInitializer {
	public static final String MOD_ID = "rsl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static IslandGame islandGame;
	
	@Override
	public void onInitialize() {
		LOGGER.info("Random SkyBlock Island mod initialized!");
		
		ModConfig.loadConfig();
		
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			StartCommand.register(dispatcher);
			StopCommand.register(dispatcher);
			AreaCommand.register(dispatcher);
			DelayCommand.register(dispatcher);
			ResetCommand.register(dispatcher);
		});
		
		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
		
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (islandGame != null && islandGame.isRunning()) {
				islandGame.getBossbar().addPlayer(handler.getPlayer());
			}
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (islandGame != null) {
				ModConfig.isRunning = islandGame.isRunning();
				if (islandGame.isRunning()) {
					ModConfig.secondsRemaining = islandGame.getSecondsRemaining();
				} else {
					ModConfig.secondsRemaining = 0;
				}
				ModConfig.saveConfig();
				ModConfig.saveSaveAreas();
			}
		});
	}
	
	private void onServerTick(MinecraftServer server) {
		if (islandGame == null) {
			islandGame = new IslandGame(server);
		}
		islandGame.tick();
	}
	
	public static IslandGame getIslandGame() {
		return islandGame;
	}
}
