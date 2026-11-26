package yifei.rsl.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import yifei.rsl.client.gui.GameplayScreen;
import yifei.rsl.network.AccessClientNetwork;
import yifei.rsl.network.BlockPoolClientNetwork;
import yifei.rsl.network.CaptureStickClientNetwork;
import yifei.rsl.network.EggClientNetwork;

/**
 * Fabric client entrypoint. Only loaded on the physical client.
 */
public class RslClient implements ClientModInitializer {
    private static KeyBinding openPoolKey;
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        WandClientNetwork.register();
        BlockPoolClientNetwork.registerClient();
        GameplayClientNetwork.registerClient();
        GameplayActionClientNetwork.registerClient();
        AccessClientNetwork.registerClient();
        EggClientNetwork.registerClient();
        CaptureStickClientNetwork.registerClient();
        openPoolKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rsl.open_pool", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.rsl"));
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rsl.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_0, "key.categories.rsl"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPoolKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    BlockPoolClientNetwork.requestOpenScreen();
                }
            }
            while (openMenuKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new GameplayScreen());
                }
            }
        });
    }
}
