package yifei.rsl.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import yifei.rsl.client.gui.BlockPoolScreen;
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
    private static boolean pendingOpen;

    private BlockPoolClientNetwork() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BlockPoolNetwork.SNAPSHOT,
                (client, handler, buf, sender) -> {
                    BlockPoolSnapshot snapshot = BlockPoolNetwork.readSnapshot(buf);
                    client.execute(() -> {
                        if (pendingOpen) {
                            pendingOpen = false;
                            if (client.currentScreen == null) {
                                // Ask the server before showing the editor: without
                                // permission the screen must never flash into view.
                                client.setScreen(new BlockPoolScreen());
                                snapshotHandler.accept(snapshot);
                                return;
                            }
                        }
                        snapshotHandler.accept(snapshot);
                    });
                });
        ClientPlayNetworking.registerGlobalReceiver(BlockPoolNetwork.RESULT,
                (client, handler, buf, sender) -> {
                    PoolMutationResult result = BlockPoolNetwork.readResult(buf);
                    client.execute(() -> {
                        if (pendingOpen) {
                            pendingOpen = false;
                            if (client.player != null) {
                                client.player.sendMessage(
                                        Text.translatable("rsl.pool.no_permission"), true);
                            }
                            return;
                        }
                        resultHandler.accept(result);
                    });
                });
    }

    public static void setHandlers(Consumer<BlockPoolSnapshot> snapshot, Consumer<PoolMutationResult> result) {
        snapshotHandler = snapshot == null ? snapshotValue -> {
        } : snapshot;
        resultHandler = result == null ? resultValue -> {
        } : result;
    }

    /**
     * Probes the server before opening the editor. A snapshot opens the screen; a
     * denial only shows the action-bar hint and leaves the gameplay view untouched.
     */
    public static void requestOpenScreen() {
        pendingOpen = true;
        requestSnapshot();
    }

    public static void requestSnapshot() {
        ClientPlayNetworking.send(BlockPoolNetwork.REQUEST, PacketByteBufs.create());
    }

    public static void sendMutation(List<String> add, List<String> remove) {
        sendMutation(add, remove, List.of());
    }

    public static void sendMutation(List<String> add, List<String> remove, List<String> removeNbt) {
        PacketByteBuf buf = PacketByteBufs.create();
        BlockPoolNetwork.writeMutation(buf, add, remove, removeNbt);
        ClientPlayNetworking.send(BlockPoolNetwork.MUTATION, buf);
    }
}
