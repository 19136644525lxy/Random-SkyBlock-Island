package yifei.rsl.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.PoolMutationResult;

import java.util.List;
import java.util.function.Consumer;

/**
 * Client-only block-pool channel. Kept out of BlockPoolNetwork so dedicated
 * servers never load client classes.
 */
public final class BlockPoolClientNetwork {
    private static Consumer<BlockPoolSnapshot> snapshotHandler = snapshot -> {
    };
    private static Consumer<PoolMutationResult> resultHandler = result -> {
    };

    private BlockPoolClientNetwork() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BlockPoolNetwork.SNAPSHOT,
                (client, handler, buf, sender) -> {
                    BlockPoolSnapshot snapshot = BlockPoolNetwork.readSnapshot(buf);
                    client.execute(() -> snapshotHandler.accept(snapshot));
                });
        ClientPlayNetworking.registerGlobalReceiver(BlockPoolNetwork.RESULT,
                (client, handler, buf, sender) -> {
                    PoolMutationResult result = BlockPoolNetwork.readResult(buf);
                    client.execute(() -> resultHandler.accept(result));
                });
    }

    public static void setHandlers(Consumer<BlockPoolSnapshot> snapshot, Consumer<PoolMutationResult> result) {
        snapshotHandler = snapshot == null ? snapshotValue -> {
        } : snapshot;
        resultHandler = result == null ? resultValue -> {
        } : result;
    }

    public static void requestSnapshot() {
        ClientPlayNetworking.send(BlockPoolNetwork.REQUEST, PacketByteBufs.create());
    }

    public static void sendMutation(List<String> add, List<String> remove) {
        PacketByteBuf buf = PacketByteBufs.create();
        BlockPoolNetwork.writeMutation(buf, add, remove);
        ClientPlayNetworking.send(BlockPoolNetwork.MUTATION, buf);
    }
}
