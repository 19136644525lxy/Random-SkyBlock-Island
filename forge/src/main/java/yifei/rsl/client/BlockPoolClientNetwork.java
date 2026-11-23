package yifei.rsl.client;

import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.network.BlockPoolNetwork;

import java.util.List;
import java.util.function.Consumer;

/**
 * Client-only entry point for the block-pool channel.
 */
public final class BlockPoolClientNetwork {
    private BlockPoolClientNetwork() {
    }

    public static void registerClient() {
    }

    public static void setHandlers(Consumer<BlockPoolSnapshot> snapshot, Consumer<PoolMutationResult> result) {
        ClientBlockPoolHandler.setHandlers(snapshot, result);
    }

    public static void requestSnapshot() {
        BlockPoolNetwork.CHANNEL.sendToServer(new BlockPoolNetwork.RequestC2S());
    }

    public static void sendMutation(List<String> add, List<String> remove) {
        BlockPoolNetwork.sendMutationToServer(add, remove);
    }
}
