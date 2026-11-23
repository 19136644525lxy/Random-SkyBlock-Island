package yifei.rsl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import yifei.rsl.client.gui.BlockPoolScreen;
import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.network.BlockPoolNetwork;

import java.util.function.Consumer;

/**
 * Client-only delivery of server block-pool packets. Also gates opening the
 * editor: the snapshot arrives first, a denial closes the flow with a hint.
 */
public final class ClientBlockPoolHandler {
    private static Consumer<BlockPoolSnapshot> snapshotHandler = snapshot -> {
    };
    private static Consumer<PoolMutationResult> resultHandler = result -> {
    };
    private static boolean pendingOpen;

    private ClientBlockPoolHandler() {
    }

    public static void setHandlers(Consumer<BlockPoolSnapshot> snapshot, Consumer<PoolMutationResult> result) {
        snapshotHandler = snapshot == null ? value -> {
        } : snapshot;
        resultHandler = result == null ? value -> {
        } : result;
    }

    /**
     * Probes the server before opening the editor. A snapshot opens the screen; a
     * denial only shows the action-bar hint and leaves the gameplay view untouched.
     */
    public static void requestOpenScreen() {
        pendingOpen = true;
        BlockPoolNetwork.CHANNEL.sendToServer(new BlockPoolNetwork.RequestC2S());
    }

    public static void snapshot(BlockPoolSnapshot snapshot) {
        if (pendingOpen) {
            pendingOpen = false;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null) {
                minecraft.setScreen(new BlockPoolScreen());
                snapshotHandler.accept(snapshot);
                return;
            }
        }
        snapshotHandler.accept(snapshot);
    }

    public static void result(PoolMutationResult result) {
        if (pendingOpen) {
            pendingOpen = false;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable("rsl.pool.no_permission"), true);
            }
            return;
        }
        resultHandler.accept(result);
    }
}
