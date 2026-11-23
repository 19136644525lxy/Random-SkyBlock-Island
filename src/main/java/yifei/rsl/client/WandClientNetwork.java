package yifei.rsl.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import yifei.rsl.common.wand.WandAction;
import yifei.rsl.common.wand.WandResult;
import yifei.rsl.common.wand.WandScreenData;
import yifei.rsl.network.WandNetwork;
import yifei.rsl.client.gui.AreaWandScreen;

/**
 * Client-only receivers for the wand confirmation flow.
 */
public final class WandClientNetwork {
    private WandClientNetwork() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WandNetwork.OPEN, (client, handler, buf, sender) -> {
            WandScreenData data = WandNetwork.readData(buf);
            client.execute(() -> MinecraftClient.getInstance().setScreen(new AreaWandScreen(data)));
        });
        ClientPlayNetworking.registerGlobalReceiver(WandNetwork.RESULT, (client, handler, buf, sender) -> {
            WandResult result = WandNetwork.readResult(buf);
            client.execute(() -> {
                if (MinecraftClient.getInstance().currentScreen instanceof AreaWandScreen screen) {
                    screen.handleResult(result);
                }
            });
        });
    }

    public static void sendAction(WandAction action) {
        PacketByteBuf buf = PacketByteBufs.create();
        WandNetwork.writeAction(buf, action);
        ClientPlayNetworking.send(WandNetwork.ACTION, buf);
    }
}
