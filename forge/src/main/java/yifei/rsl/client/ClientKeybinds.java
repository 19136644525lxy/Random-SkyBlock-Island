package yifei.rsl.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;
import yifei.rsl.client.gui.GameplayScreen;

/**
 * Client-only key binding. Never loaded on a dedicated server.
 */
public final class ClientKeybinds {
    private static final KeyMapping OPEN_POOL = new KeyMapping(
            "key.rsl.open_pool", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.rsl");
    private static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.rsl.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_0, "key.categories.rsl");

    private ClientKeybinds() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientKeybinds::onRegisterKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientKeybinds::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_POOL);
        event.register(OPEN_MENU);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_POOL.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientBlockPoolHandler.requestOpenScreen();
            }
        }
        while (OPEN_MENU.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new GameplayScreen());
            }
        }
    }
}
