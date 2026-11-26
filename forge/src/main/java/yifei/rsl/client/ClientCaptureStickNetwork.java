package yifei.rsl.client;

import net.minecraft.client.Minecraft;
import yifei.rsl.client.gui.CaptureNameScreen;
import yifei.rsl.network.CaptureStickNetwork;

/**
 * Client-only helpers for the capture stick: open the naming screen and answer the
 * server's open/confirm/cancel messages.
 */
public final class ClientCaptureStickNetwork {
    private ClientCaptureStickNetwork() {
    }

    public static void open(String state, String defaultName) {
        Minecraft.getInstance().setScreen(new CaptureNameScreen(state, defaultName));
    }

    public static void request() {
        CaptureStickNetwork.CHANNEL.sendToServer(new CaptureStickNetwork.RequestC2S());
    }

    public static void confirm(String name) {
        CaptureStickNetwork.CHANNEL.sendToServer(new CaptureStickNetwork.ConfirmC2S(name == null ? "" : name));
    }

    public static void cancel() {
        CaptureStickNetwork.CHANNEL.sendToServer(new CaptureStickNetwork.CancelC2S());
    }
}
