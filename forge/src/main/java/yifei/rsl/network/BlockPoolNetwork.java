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
import yifei.rsl.client.ClientBlockPoolHandler;
import yifei.rsl.common.config.BlockMutationResult;
import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.config.ModConfig;
import yifei.rsl.permission.PoolPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Forge block-pool channel. Client-only classes are touched exclusively inside a
 * DistExecutor client lambda.
 */
public final class BlockPoolNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("rsl", "block_pool"), () -> "1", "1"::equals, "1"::equals);

    private static final int MAX_BATCH = 512;

    private BlockPoolNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, RequestC2S.class, RequestC2S::encode, RequestC2S::decode, RequestC2S::handle);
        CHANNEL.registerMessage(1, SnapshotS2C.class, SnapshotS2C::encode, SnapshotS2C::decode, SnapshotS2C::handle);
        CHANNEL.registerMessage(2, MutationC2S.class, MutationC2S::encode, MutationC2S::decode, MutationC2S::handle);
        CHANNEL.registerMessage(3, ResultS2C.class, ResultS2C::encode, ResultS2C::decode, ResultS2C::handle);
    }

    public static BlockPoolSnapshot snapshot(ServerPlayer player) {
        List<String> all = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null && block != Blocks.AIR) {
                all.add(id.toString());
            }
        }
        all.sort(String::compareTo);
        return new BlockPoolSnapshot(
                all,
                ModConfig.getAllowedBlockIds(),
                ModConfig.getInvalidBlockIds(),
                ModConfig.getMaxPoolSize(),
                0,
                PoolPermissions.canUse(player));
    }

    private static void sendSnapshot(ServerPlayer player) {
        if (!PoolPermissions.canUse(player)) {
            sendResult(player, PoolMutationResult.fail(BlockMutationResult.INVALID_ID,
                    "rsl.pool.no_permission", List.of()));
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SnapshotS2C(snapshot(player)));
    }

    private static void handleMutation(ServerPlayer player, List<String> add, List<String> remove) {
        if (!PoolPermissions.canUse(player)) {
            sendResult(player, PoolMutationResult.fail(BlockMutationResult.INVALID_ID,
                    "rsl.pool.no_permission", List.of()));
            return;
        }
        if (add.size() > MAX_BATCH || remove.size() > MAX_BATCH) {
            sendResult(player, PoolMutationResult.fail(BlockMutationResult.INVALID_ID,
                    "rsl.message.blocks_invalid_id", List.of()));
            return;
        }
        PoolMutationResult result = ModConfig.applyBlockMutations(add, remove);
        sendResult(player, result);
        if (result.success) {
            sendSnapshot(player);
        }
    }

    private static void sendResult(ServerPlayer player, PoolMutationResult result) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ResultS2C(result));
    }

    public static void sendMutationToServer(List<String> add, List<String> remove) {
        CHANNEL.sendToServer(new MutationC2S(add, remove));
    }

    private static void writeIds(FriendlyByteBuf buf, List<String> ids) {
        buf.writeInt(ids.size());
        for (String id : ids) {
            buf.writeUtf(id == null ? "" : id);
        }
    }

    private static List<String> readIds(FriendlyByteBuf buf) {
        int size = Math.max(0, Math.min(buf.readInt(), ModConfig.getMaxPoolSize() * 4));
        List<String> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readUtf());
        }
        return ids;
    }

    private static void writeSnapshot(FriendlyByteBuf buf, BlockPoolSnapshot snapshot) {
        writeIds(buf, snapshot.allBlockIds);
        writeIds(buf, snapshot.poolIds);
        writeIds(buf, snapshot.invalidIds);
        buf.writeInt(snapshot.maxPool);
        buf.writeInt(snapshot.permissionLevel);
        buf.writeBoolean(snapshot.canEdit);
    }

    private static BlockPoolSnapshot readSnapshot(FriendlyByteBuf buf) {
        return new BlockPoolSnapshot(readIds(buf), readIds(buf), readIds(buf),
                buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    private static void writeResult(FriendlyByteBuf buf, PoolMutationResult result) {
        buf.writeBoolean(result.success);
        buf.writeUtf(result.messageKey == null ? "" : result.messageKey);
        writeIds(buf, result.failedIds);
    }

    private static PoolMutationResult readResult(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        String key = buf.readUtf();
        List<String> failed = readIds(buf);
        return success ? PoolMutationResult.ok()
                : PoolMutationResult.fail(BlockMutationResult.INVALID_ID, key, failed);
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
        private final BlockPoolSnapshot snapshot;

        public SnapshotS2C(BlockPoolSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        public static void encode(SnapshotS2C msg, FriendlyByteBuf buf) {
            writeSnapshot(buf, msg.snapshot);
        }

        public static SnapshotS2C decode(FriendlyByteBuf buf) {
            return new SnapshotS2C(readSnapshot(buf));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientBlockPoolHandler.snapshot(snapshot)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class MutationC2S {
        private final List<String> add;
        private final List<String> remove;

        public MutationC2S(List<String> add, List<String> remove) {
            this.add = List.copyOf(add);
            this.remove = List.copyOf(remove);
        }

        public static void encode(MutationC2S msg, FriendlyByteBuf buf) {
            writeIds(buf, msg.add);
            writeIds(buf, msg.remove);
        }

        public static MutationC2S decode(FriendlyByteBuf buf) {
            return new MutationC2S(readIds(buf), readIds(buf));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    handleMutation(player, add, remove);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ResultS2C {
        private final PoolMutationResult result;

        public ResultS2C(PoolMutationResult result) {
            this.result = result;
        }

        public static void encode(ResultS2C msg, FriendlyByteBuf buf) {
            writeResult(buf, msg.result);
        }

        public static ResultS2C decode(FriendlyByteBuf buf) {
            return new ResultS2C(readResult(buf));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientBlockPoolHandler.result(result)));
            ctx.get().setPacketHandled(true);
        }
    }
}
