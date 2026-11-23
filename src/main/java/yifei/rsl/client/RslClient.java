package yifei.rsl.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import yifei.rsl.client.gui.BlockPoolScreen;
import yifei.rsl.network.BlockPoolClientNetwork;

/**
 * Fabric client entrypoint. Only loaded on the physical client.
 */
public class RslClient implements ClientModInitializer {
    private static KeyBinding openPoolKey;

    @Override
    public void onInitializeClient() {
        WandClientNetwork.register();
        BlockPoolClientNetwork.registerClient();
        openPoolKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rsl.open_pool", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.rsl"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPoolKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new BlockPoolScreen());
                }
            }
        });
    }
}
