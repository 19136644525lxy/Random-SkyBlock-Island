package yifei.rsl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yifei.rsl.client.ClientGameplayHandler;
import yifei.rsl.config.ModConfig;

import java.util.function.Supplier;

/**
 * Forge gameplay settings channel. Mirrors the Fabric channel: the menu asks the
 * server for the transform delay so multiplayer clients show the authoritative
 * value instead of their own local config. Client-only classes are touched solely
 * inside a DistExecutor client lambda.
 */
public final class GameplayNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("rsl", "gameplay_settings"), () -> "1", "1"::equals, "1"::equals);

    private GameplayNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, RequestC2S.class, RequestC2S::encode, RequestC2S::decode, RequestC2S::handle);
        CHANNEL.registerMessage(1, SnapshotS2C.class, SnapshotS2C::encode, SnapshotS2C::decode, SnapshotS2C::handle);
    }

    public static void sendSnapshot(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SnapshotS2C(ModConfig.getTransformDelaySeconds()));
    }

    public static void sendRequest() {
        CHANNEL.sendToServer(new RequestC2S());
    }

    public static class RequestC2S {
        public static void encode(RequestC2S msg, FriendlyByteBuf buf) {
        }

        public static RequestC2S decode(FriendlyByteBuf buf) {
            return new RequestC2S();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    sendSnapshot(player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SnapshotS2C {
        private final int seconds;

        public SnapshotS2C(int seconds) {
            this.seconds = seconds;
        }

        public static void encode(SnapshotS2C msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.seconds);
        }

        public static SnapshotS2C decode(FriendlyByteBuf buf) {
            return new SnapshotS2C(buf.readVarInt());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientGameplayHandler.snapshot(seconds)));
            ctx.get().setPacketHandled(true);
        }
    }
}
