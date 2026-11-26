package yifei.rsl.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.common.config.BlockMutationResult;
import yifei.rsl.common.config.PoolEntry;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.NbtPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Forge mirror of the Fabric capture-stick network: give the stick, ask the client
 * to name the captured container, then store the container with that name.
 */
public final class CaptureStickNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("rsl", "capture_stick"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static final int MAX_NAME_LENGTH = 32;
    private static final long TIMEOUT_MS = 60_000L;
    private static final String CUSTOM_NAME = "CustomName";
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private record Pending(String id, String state, String payload, long createdAt) {
    }

    private CaptureStickNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, RequestC2S.class, RequestC2S::encode, RequestC2S::decode, RequestC2S::handle);
        CHANNEL.registerMessage(id++, OpenS2C.class, OpenS2C::encode, OpenS2C::decode, OpenS2C::handle);
        CHANNEL.registerMessage(id++, ConfirmC2S.class, ConfirmC2S::encode, ConfirmC2S::decode, ConfirmC2S::handle);
        CHANNEL.registerMessage(id, CancelC2S.class, CancelC2S::encode, CancelC2S::decode, CancelC2S::handle);
    }

    private static void give(ServerPlayer player) {
        ItemStack stack = new ItemStack(RandomSkyBlockIsland.CAPTURE_STICK.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendSystemMessage(Component.translatable("rsl.message.stick_given"), true);
    }

    public static void beginCapture(ServerPlayer player, String id, String state, String payload, String defaultName) {
        PENDING.put(player.getUUID(), new Pending(id, state, payload, System.currentTimeMillis()));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenS2C(state, defaultName == null ? "" : defaultName));
    }

    private static void confirm(ServerPlayer player, String rawName) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null || System.currentTimeMillis() - pending.createdAt() > TIMEOUT_MS) {
            player.sendSystemMessage(Component.translatable("rsl.message.capture_failed"));
            return;
        }
        CompoundTag tag = NbtPayload.decode(pending.payload());
        if (tag == null) {
            player.sendSystemMessage(Component.translatable("rsl.message.capture_failed"));
            return;
        }
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        if (name.isEmpty()) {
            tag.remove(CUSTOM_NAME);
        } else {
            tag.putString(CUSTOM_NAME, Component.Serializer.toJson(Component.literal(name)));
        }
        String payload = NbtPayload.encode(tag);
        if (payload.isEmpty() || payload.length() > PoolEntry.MAX_NBT_LENGTH) {
            player.sendSystemMessage(Component.translatable("rsl.message.capture_too_large"));
            return;
        }
        BlockMutationResult result = ModConfig.addNbtPoolEntry(new PoolEntry(pending.id(), pending.state(), payload));
        Component message = switch (result) {
            case OK -> Component.translatable("rsl.message.capture_added");
            case DUPLICATE -> Component.translatable("rsl.message.capture_exists");
            case POOL_FULL -> Component.translatable("rsl.message.capture_pool_full");
            default -> Component.translatable("rsl.message.capture_failed");
        };
        if (result == BlockMutationResult.OK) {
            yifei.rsl.game.SoundFx.play(player, net.minecraft.sounds.SoundEvents.BOTTLE_FILL, 0.6F, 1.0F);
        } else {
            yifei.rsl.game.SoundFx.play(player, net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.6F, 1.0F);
        }
        player.sendSystemMessage(message);
        RandomSkyBlockIsland.LOGGER.info("[RSL] Captured container {} ({})", pending.id(), result);
    }

    public static String displayName(CompoundTag tag) {
        if (tag == null || !tag.contains(CUSTOM_NAME)) {
            return "";
        }
        try {
            return Component.Serializer.fromJson(tag.getString(CUSTOM_NAME)).getString();
        } catch (Exception e) {
            return "";
        }
    }

    public static class RequestC2S {
        public RequestC2S() {
        }

        public void encode(FriendlyByteBuf buf) {
        }

        public static RequestC2S decode(FriendlyByteBuf buf) {
            return new RequestC2S();
        }

        public static void handle(RequestC2S msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    give(player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class OpenS2C {
        private final String state;
        private final String defaultName;

        public OpenS2C(String state, String defaultName) {
            this.state = state;
            this.defaultName = defaultName;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(state, 512);
            buf.writeUtf(defaultName, 256);
        }

        public static OpenS2C decode(FriendlyByteBuf buf) {
            return new OpenS2C(buf.readUtf(512), buf.readUtf(256));
        }

        public static void handle(OpenS2C msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> yifei.rsl.client.ClientCaptureStickNetwork.open(msg.state, msg.defaultName)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ConfirmC2S {
        private final String name;

        public ConfirmC2S(String name) {
            this.name = name;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name, MAX_NAME_LENGTH + 8);
        }

        public static ConfirmC2S decode(FriendlyByteBuf buf) {
            return new ConfirmC2S(buf.readUtf(MAX_NAME_LENGTH + 8));
        }

        public static void handle(ConfirmC2S msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    confirm(player, msg.name);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class CancelC2S {
        public CancelC2S() {
        }

        public void encode(FriendlyByteBuf buf) {
        }

        public static CancelC2S decode(FriendlyByteBuf buf) {
            return new CancelC2S();
        }

        public static void handle(CancelC2S msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    PENDING.remove(player.getUUID());
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
