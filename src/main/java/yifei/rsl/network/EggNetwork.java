package yifei.rsl.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import yifei.rsl.config.ModConfig;
import yifei.rsl.permission.PoolPermissions;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-safe egg-mode channel. Client-only receivers live in EggClientNetwork.
 * The snapshot carries the egg settings plus the full block id list and the
 * selected egg pool, so the client screen can reuse the block-grid picker.
 */
public final class EggNetwork {
    public static final Identifier REQUEST = new Identifier("rsl", "egg_request");
    public static final Identifier APPLY = new Identifier("rsl", "egg_apply");
    public static final Identifier TOGGLE = new Identifier("rsl", "egg_toggle");
    public static final Identifier SNAPSHOT = new Identifier("rsl", "egg_snapshot");
    public static final Identifier RESULT = new Identifier("rsl", "egg_result");

    private static final int MAX_IDS = 4096;

    private EggNetwork() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) ->
                server.execute(() -> sendSnapshot(player)));
        ServerPlayNetworking.registerGlobalReceiver(APPLY, (server, player, handler, buf, sender) -> {
            boolean enabled = buf.readBoolean();
            double chance = buf.readDouble();
            int volume = buf.readInt();
            List<String> ids = readIds(buf);
            server.execute(() -> handleApply(player, enabled, chance, volume, ids));
        });
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE, (server, player, handler, buf, sender) ->
                server.execute(() -> handleToggle(player)));
    }

    public static List<String> allBlockIds() {
        List<String> all = new ArrayList<>();
        for (Identifier id : Registries.BLOCK.getIds()) {
            Block block = Registries.BLOCK.get(id);
            if (block != null && block != Blocks.AIR) {
                all.add(id.toString());
            }
        }
        all.sort(String::compareTo);
        return all;
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(ModConfig.isEggModeEnabled());
        buf.writeDouble(ModConfig.getEggChance());
        buf.writeInt(ModConfig.getEggVolume());
        buf.writeInt(ModConfig.getEggMinRadius());
        buf.writeInt(ModConfig.getEggMaxRadius());
        buf.writeInt(ModConfig.getEggMinDistance());
        writeIds(buf, allBlockIds());
        writeIds(buf, ModConfig.getEggPoolIds());
        ServerPlayNetworking.send(player, SNAPSHOT, buf);
    }

    private static void handleApply(ServerPlayerEntity player, boolean enabled, double chance, int volume, List<String> ids) {
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

    private static void handleToggle(ServerPlayerEntity player) {
        if (!PoolPermissions.canManage(player)) {
            sendResult(player, false, "rsl.egg.no_permission");
            return;
        }
        boolean ok = ModConfig.toggleEggMode();
        sendResult(player, ok, ok ? "rsl.egg.applied" : "rsl.egg.save_failed");
        sendSnapshot(player);
    }

    private static void sendResult(ServerPlayerEntity player, boolean success, String key) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(success);
        buf.writeString(key == null ? "" : key);
        ServerPlayNetworking.send(player, RESULT, buf);
    }

    private static void writeIds(PacketByteBuf buf, List<String> ids) {
        buf.writeInt(ids.size());
        for (String id : ids) {
            buf.writeString(id == null ? "" : id);
        }
    }

    private static List<String> readIds(PacketByteBuf buf) {
        int size = Math.max(0, Math.min(buf.readInt(), MAX_IDS));
        List<String> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readString());
        }
        return ids;
    }
}
