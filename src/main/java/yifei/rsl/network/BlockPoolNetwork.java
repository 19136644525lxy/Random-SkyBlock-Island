package yifei.rsl.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-safe block-pool channel. Client-only receivers live in BlockPoolClientNetwork.
 */
public final class BlockPoolNetwork {
    public static final Identifier REQUEST = new Identifier("rsl", "pool_request");
    public static final Identifier SNAPSHOT = new Identifier("rsl", "pool_snapshot");
    public static final Identifier MUTATION = new Identifier("rsl", "pool_mutation");
    public static final Identifier RESULT = new Identifier("rsl", "pool_result");

    private static final int MAX_BATCH = 512;

    private BlockPoolNetwork() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) ->
                server.execute(() -> sendSnapshot(player)));
        ServerPlayNetworking.registerGlobalReceiver(MUTATION, (server, player, handler, buf, sender) -> {
            List<String> add = readIds(buf);
            List<String> remove = readIds(buf);
            server.execute(() -> handleMutation(player, add, remove));
        });
    }

    public static BlockPoolSnapshot snapshot(ServerPlayerEntity player) {
        List<String> all = new ArrayList<>();
        for (Identifier id : Registries.BLOCK.getIds()) {
            Block block = Registries.BLOCK.get(id);
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
                ModConfig.getBlocksPermissionLevel(),
                player.hasPermissionLevel(ModConfig.getBlocksPermissionLevel()));
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        writeSnapshot(buf, snapshot(player));
        ServerPlayNetworking.send(player, SNAPSHOT, buf);
    }

    private static void handleMutation(ServerPlayerEntity player, List<String> add, List<String> remove) {
        if (!player.hasPermissionLevel(ModConfig.getBlocksPermissionLevel())) {
            sendResult(player, PoolMutationResult.fail(
                    yifei.rsl.common.config.BlockMutationResult.INVALID_ID,
                    "rsl.message.blocks_no_permission", List.of()));
            return;
        }
        if (add.size() > MAX_BATCH || remove.size() > MAX_BATCH) {
            sendResult(player, PoolMutationResult.fail(
                    yifei.rsl.common.config.BlockMutationResult.INVALID_ID,
                    "rsl.message.blocks_invalid_id", List.of()));
            return;
        }
        PoolMutationResult result = ModConfig.applyBlockMutations(add, remove);
        sendResult(player, result);
        if (result.success) {
            sendSnapshot(player);
        }
    }

    private static void sendResult(ServerPlayerEntity player, PoolMutationResult result) {
        PacketByteBuf buf = PacketByteBufs.create();
        writeResult(buf, result);
        ServerPlayNetworking.send(player, RESULT, buf);
    }

    public static void writeMutation(PacketByteBuf buf, List<String> add, List<String> remove) {
        writeIds(buf, add);
        writeIds(buf, remove);
    }

    public static void writeSnapshot(PacketByteBuf buf, BlockPoolSnapshot snapshot) {
        writeIds(buf, snapshot.allBlockIds);
        writeIds(buf, snapshot.poolIds);
        writeIds(buf, snapshot.invalidIds);
        buf.writeInt(snapshot.maxPool);
        buf.writeInt(snapshot.permissionLevel);
        buf.writeBoolean(snapshot.canEdit);
    }

    public static BlockPoolSnapshot readSnapshot(PacketByteBuf buf) {
        List<String> all = readIds(buf);
        List<String> pool = readIds(buf);
        List<String> invalid = readIds(buf);
        return new BlockPoolSnapshot(all, pool, invalid,
                buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void writeResult(PacketByteBuf buf, PoolMutationResult result) {
        buf.writeBoolean(result.success);
        buf.writeString(result.messageKey == null ? "" : result.messageKey);
        writeIds(buf, result.failedIds);
    }

    public static PoolMutationResult readResult(PacketByteBuf buf) {
        boolean success = buf.readBoolean();
        String key = buf.readString();
        List<String> failed = readIds(buf);
        return success
                ? PoolMutationResult.ok()
                : PoolMutationResult.fail(yifei.rsl.common.config.BlockMutationResult.INVALID_ID, key, failed);
    }

    private static void writeIds(PacketByteBuf buf, List<String> ids) {
        buf.writeInt(ids.size());
        for (String id : ids) {
            buf.writeString(id == null ? "" : id);
        }
    }

    private static List<String> readIds(PacketByteBuf buf) {
        int size = Math.max(0, Math.min(buf.readInt(), ModConfig.getMaxPoolSize() * 4));
        List<String> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readString());
        }
        return ids;
    }
}
