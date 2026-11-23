package yifei.rsl.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import yifei.rsl.network.GameplayNetwork;

import java.util.function.IntConsumer;

/**
 * Client side of the gameplay settings channel. Caches the delay reported by the
 * server so the menu opens with the authoritative value instead of the local one.
 */
public final class GameplayClientNetwork {
    private static int syncedDelaySeconds = -1;
    private static IntConsumer listener = seconds -> {
    };

    private GameplayClientNetwork() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(GameplayNetwork.SNAPSHOT,
                (client, handler, buf, sender) -> {
                    int seconds = buf.readVarInt();
                    client.execute(() -> {
                        syncedDelaySeconds = seconds;
                        listener.accept(seconds);
                    });
                });
    }

    public static void setHandler(IntConsumer newListener) {
        listener = newListener == null ? seconds -> {
        } : newListener;
    }

    public static void requestSnapshot() {
        ClientPlayNetworking.send(GameplayNetwork.REQUEST, PacketByteBufs.create());
    }

    public static int getSyncedDelaySeconds() {
        return syncedDelaySeconds;
    }
}
