package yifei.rsl;

import yifei.rsl.commands.AccessCommand;
import yifei.rsl.commands.StartCommand;
import yifei.rsl.commands.StopCommand;
import yifei.rsl.commands.AreaCommand;
import yifei.rsl.commands.BlocksCommand;
import yifei.rsl.commands.DelayCommand;
import yifei.rsl.commands.ReloadCommand;
import yifei.rsl.commands.ResetCommand;
import yifei.rsl.commands.StatusCommand;
import yifei.rsl.commands.WandCommand;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.IslandGame;
import yifei.rsl.item.SelectionWandItem;
import yifei.rsl.item.WandSelectionManager;
import yifei.rsl.network.AccessNetwork;
import yifei.rsl.network.BlockPoolNetwork;
import yifei.rsl.network.GameplayActionNetwork;
import yifei.rsl.network.GameplayNetwork;
import yifei.rsl.network.WandNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomSkyBlockIsland implements ModInitializer {
	public static final String MOD_ID = "rsl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Item SELECTION_WAND = new SelectionWandItem(new Item.Settings().maxCount(1));
	
	private static IslandGame islandGame;
	
	@Override
	public void onInitialize() {
		LOGGER.info("Random SkyBlock Island mod initialized!");
		
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "selection_wand"), SELECTION_WAND);
		WandNetwork.registerServer();
		BlockPoolNetwork.registerServer();
		GameplayNetwork.registerServer();
		GameplayActionNetwork.registerServer();
		AccessNetwork.registerServer();
		
		ModConfig.loadConfig();
		
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			StartCommand.register(dispatcher);
			StopCommand.register(dispatcher);
			AreaCommand.register(dispatcher);
			AccessCommand.register(dispatcher);
			BlocksCommand.register(dispatcher);
			DelayCommand.register(dispatcher);
			ResetCommand.register(dispatcher);
			StatusCommand.register(dispatcher);
			ReloadCommand.register(dispatcher);
			WandCommand.register(dispatcher);
		});
		
		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
		
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (islandGame != null && islandGame.isRunning()) {
				islandGame.getBossbar().addPlayer(handler.getPlayer());
			}
		});
		
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (islandGame != null) {
				islandGame.getBossbar().removePlayer(handler.getPlayer());
			}
			WandSelectionManager.remove(handler.getPlayer().getUuid());
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (islandGame != null) {
				ModConfig.setRunning(islandGame.isRunning());
				ModConfig.setSecondsRemaining(islandGame.isRunning() ? islandGame.getSecondsRemaining() : 0);
				ModConfig.saveState();
				ModConfig.saveSaveAreas();
			}
		});
		
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> islandGame = null);
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
