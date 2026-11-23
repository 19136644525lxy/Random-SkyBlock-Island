package yifei.rsl.client;

import yifei.rsl.network.GameplayNetwork;

import java.util.function.IntConsumer;

/**
 * Client-only delivery of the server-reported transform delay. Kept in the client
 * package so dedicated servers never load it.
 */
public final class ClientGameplayHandler {
    private static int syncedDelaySeconds = -1;
    private static IntConsumer listener = seconds -> {
    };

    private ClientGameplayHandler() {
    }

    public static void setHandler(IntConsumer newListener) {
        listener = newListener == null ? seconds -> {
        } : newListener;
    }

    public static void snapshot(int seconds) {
        syncedDelaySeconds = seconds;
        listener.accept(seconds);
    }

    public static int getSyncedDelaySeconds() {
        return syncedDelaySeconds;
    }

    public static void requestSnapshot() {
        GameplayNetwork.sendRequest();
    }
}
