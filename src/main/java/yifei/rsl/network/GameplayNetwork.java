package yifei.rsl.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import yifei.rsl.config.ModConfig;

/**
 * Server-safe gameplay settings channel. Answers the client's request with the
 * server's authoritative transform delay, so a multiplayer client shows the same
 * value the game loop actually uses instead of its own local config.
 */
public final class GameplayNetwork {
    public static final Identifier REQUEST = new Identifier("rsl", "gameplay_delay_request");
    public static final Identifier SNAPSHOT = new Identifier("rsl", "gameplay_delay_snapshot");

    private GameplayNetwork() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) ->
                server.execute(() -> sendSnapshot(player)));
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(ModConfig.getTransformDelaySeconds());
        ServerPlayNetworking.send(player, SNAPSHOT, buf);
    }
}
