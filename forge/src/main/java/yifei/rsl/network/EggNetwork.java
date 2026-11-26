package yifei.rsl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import yifei.rsl.client.ClientEggHandler;
import yifei.rsl.config.ModConfig;
import yifei.rsl.permission.PoolPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Forge egg-mode channel. Mirrors the Fabric channel: the "other pools" screen
 * asks for a snapshot, and owner-only edits go back to the server. Client-only
 * classes are touched solely inside a DistExecutor client lambda.
 */
public final class EggNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("rsl", "egg_settings"), () -> "1", "1"::equals, "1"::equals);

    private static final int MAX_IDS = 4096;

    private EggNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, RequestC2S.class, RequestC2S::encode, RequestC2S::decode, RequestC2S::handle);
        CHANNEL.registerMessage(1, SnapshotS2C.class, SnapshotS2C::encode, SnapshotS2C::decode, SnapshotS2C::handle);
        CHANNEL.registerMessage(2, ToggleC2S.class, ToggleC2S::encode, ToggleC2S::decode, ToggleC2S::handle);
        CHANNEL.registerMessage(3, ApplyC2S.class, ApplyC2S::encode, ApplyC2S::decode, ApplyC2S::handle);
        CHANNEL.registerMessage(4, ResultS2C.class, ResultS2C::encode, ResultS2C::decode, ResultS2C::handle);
    }

    public static List<String> allBlockIds() {
        List<String> all = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null && block != Blocks.AIR) {
                all.add(id.toString());
            }
        }
        all.sort(String::compareTo);
        return all;
    }

    public static void sendSnapshot(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SnapshotS2C(ModConfig.isEggModeEnabled(), ModConfig.getEggChance(),
                        ModConfig.getEggVolume(), ModConfig.getEggMinRadius(), ModConfig.getEggMaxRadius(),
                        ModConfig.getEggMinDistance(), allBlockIds(), ModConfig.getEggPoolIds()));
    }

    public static void sendRequest() {
        CHANNEL.sendToServer(new RequestC2S());
    }

    public static void sendToggle() {
        CHANNEL.sendToServer(new ToggleC2S());
    }

    public static void sendApply(boolean enabled, double chance, int volume, List<String> ids) {
        CHANNEL.sendToServer(new ApplyC2S(enabled, chance, volume, ids));
    }

    private static void sendResult(ServerPlayer player, boolean success, String key) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ResultS2C(success, key == null ? "" : key));
    }

    private static void handleApply(ServerPlayer player, boolean enabled, double chance, int volume, List<String> ids) {
        if (!PoolPermissions.canManage(player)) {
            sendResult(player, false, "rsl.egg.no_permission");
            return;
        }
        if (ids.size() > MAX_IDS) {
            sendResult(player, false, "rsl.egg.invalid");
            return;
        }
        boolean ok = ModConfig.setEggModeEnabled(enabled);
        ok = ModConfig.setEggChance(chance) && ok;
        ok = ModConfig.setEggVolume(volume) && ok;
        ok = ModConfig.setEggPool(ids) && ok;
        sendResult(player, ok, ok ? "rsl.egg.applied" : "rsl.egg.save_failed");
        sendSnapshot(player);
    }

    private static void handleToggle(ServerPlayer player) {
        if (!PoolPermissions.canManage(player)) {
            sendResult(player, false, "rsl.egg.no_permission");
            return;
        }
        boolean ok = ModConfig.toggleEggMode();
        sendResult(player, ok, ok ? "rsl.egg.applied" : "rsl.egg.save_failed");
        sendSnapshot(player);
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

    public static class ToggleC2S {
        public static void encode(ToggleC2S msg, FriendlyByteBuf buf) {
        }

        public static ToggleC2S decode(FriendlyByteBuf buf) {
            return new ToggleC2S();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    handleToggle(player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ApplyC2S {
        private final boolean enabled;
        private final double chance;
        private final int volume;
        private final List<String> ids;

        public ApplyC2S(boolean enabled, double chance, int volume, List<String> ids) {
            this.enabled = enabled;
            this.chance = chance;
            this.volume = volume;
            this.ids = ids;
        }

        public static void encode(ApplyC2S msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.enabled);
            buf.writeDouble(msg.chance);
            buf.writeVarInt(msg.volume);
            writeIds(buf, msg.ids);
        }

        public static ApplyC2S decode(FriendlyByteBuf buf) {
            boolean enabled = buf.readBoolean();
            double chance = buf.readDouble();
            int volume = buf.readVarInt();
            return new ApplyC2S(enabled, chance, volume, readIds(buf));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    handleApply(player, enabled, chance, volume, ids);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SnapshotS2C {
        private final boolean enabled;
        private final double chance;
        private final int volume;
        private final int minRadius;
        private final int maxRadius;
        private final int minDistance;
        private final List<String> allIds;
        private final List<String> selectedIds;

        public SnapshotS2C(boolean enabled, double chance, int volume, int minRadius, int maxRadius,
                           int minDistance, List<String> allIds, List<String> selectedIds) {
            this.enabled = enabled;
            this.chance = chance;
            this.volume = volume;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.minDistance = minDistance;
            this.allIds = allIds;
            this.selectedIds = selectedIds;
        }

        public static void encode(SnapshotS2C msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.enabled);
            buf.writeDouble(msg.chance);
            buf.writeVarInt(msg.volume);
            buf.writeVarInt(msg.minRadius);
            buf.writeVarInt(msg.maxRadius);
            buf.writeVarInt(msg.minDistance);
            writeIds(buf, msg.allIds);
            writeIds(buf, msg.selectedIds);
        }

        public static SnapshotS2C decode(FriendlyByteBuf buf) {
            boolean enabled = buf.readBoolean();
            double chance = buf.readDouble();
            int volume = buf.readVarInt();
            int minRadius = buf.readVarInt();
            int maxRadius = buf.readVarInt();
            int minDistance = buf.readVarInt();
            return new SnapshotS2C(enabled, chance, volume, minRadius, maxRadius, minDistance,
                    readIds(buf), readIds(buf));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientEggHandler.snapshot(enabled, chance, volume, minRadius, maxRadius,
                            minDistance, allIds, selectedIds)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ResultS2C {
        private final boolean success;
        private final String messageKey;

        public ResultS2C(boolean success, String messageKey) {
            this.success = success;
            this.messageKey = messageKey;
        }

        public static void encode(ResultS2C msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.success);
            buf.writeUtf(msg.messageKey);
        }

        public static ResultS2C decode(FriendlyByteBuf buf) {
            return new ResultS2C(buf.readBoolean(), buf.readUtf());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientEggHandler.result(success, messageKey)));
            ctx.get().setPacketHandled(true);
        }
    }

    private static void writeIds(FriendlyByteBuf buf, List<String> ids) {
        buf.writeVarInt(ids.size());
        for (String id : ids) {
            buf.writeUtf(id == null ? "" : id);
        }
    }

    private static List<String> readIds(FriendlyByteBuf buf) {
        int size = Math.max(0, Math.min(buf.readVarInt(), MAX_IDS));
        List<String> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readUtf());
        }
        return ids;
    }
}
