package yifei.rsl.client;

import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.PoolMutationResult;

import java.util.function.Consumer;

/**
 * Client-only delivery of server block-pool packets.
 */
public final class ClientBlockPoolHandler {
    private static Consumer<BlockPoolSnapshot> snapshotHandler = snapshot -> {
    };
    private static Consumer<PoolMutationResult> resultHandler = result -> {
    };

    private ClientBlockPoolHandler() {
    }

    public static void setHandlers(Consumer<BlockPoolSnapshot> snapshot, Consumer<PoolMutationResult> result) {
        snapshotHandler = snapshot == null ? value -> {
        } : snapshot;
        resultHandler = result == null ? value -> {
        } : result;
    }

    public static void snapshot(BlockPoolSnapshot snapshot) {
        snapshotHandler.accept(snapshot);
    }

    public static void result(PoolMutationResult result) {
        resultHandler.accept(result);
    }
}
