package yifei.rsl.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

/**
 * Client-only entry points for the capture-stick flow: requests a stick from the
 * gameplay screen and answers the server's "rename this container" prompt.
 */
@Environment(EnvType.CLIENT)
public final class CaptureStickClientNetwork {
    private CaptureStickClientNetwork() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(CaptureStickNetwork.OPEN, (client, handler, buf, sender) -> {
            String state = buf.readString(512);
            String name = buf.readString(256);
            client.execute(() -> client.setScreen(new yifei.rsl.client.gui.CaptureNameScreen(state, name)));
        });
    }

    public static void request() {
        ClientPlayNetworking.send(CaptureStickNetwork.REQUEST, CaptureStickNetwork.emptyRequest());
    }

    public static void confirm(String name) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(name == null ? "" : name, CaptureStickNetwork.MAX_NAME_LENGTH + 8);
        ClientPlayNetworking.send(CaptureStickNetwork.CONFIRM, buf);
    }

    public static void cancel() {
        ClientPlayNetworking.send(CaptureStickNetwork.CANCEL, PacketByteBufs.create());
    }

    public static void openEditor(String state, String defaultName) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new yifei.rsl.client.gui.CaptureNameScreen(state, defaultName));
    }
}
