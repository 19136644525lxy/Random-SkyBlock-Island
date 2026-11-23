package yifei.rsl.client;

import net.minecraft.client.Minecraft;
import yifei.rsl.client.gui.AreaWandScreen;
import yifei.rsl.common.wand.WandResult;
import yifei.rsl.common.wand.WandScreenData;

/**
 * Client-only bridge used by the Forge network handler via DistExecutor.
 */
public final class ClientWandHandler {
    private ClientWandHandler() {
    }

    public static void open(WandScreenData data) {
        Minecraft.getInstance().setScreen(new AreaWandScreen(data));
    }

    public static void result(WandResult result) {
        if (Minecraft.getInstance().screen instanceof AreaWandScreen screen) {
            screen.handleResult(result);
        }
    }
}
