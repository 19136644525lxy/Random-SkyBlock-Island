package yifei.rsl.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.common.config.BlockMutationResult;
import yifei.rsl.common.config.PoolEntry;
import yifei.rsl.config.ModConfig;
import yifei.rsl.game.NbtPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side of the capture-stick flow. The gameplay screen can request a stick;
 * sneaking + right-clicking a filled container first asks the client to edit the
 * container's display name, then the confirmed name is written into the stored NBT.
 * Kept server-safe so dedicated servers never load client classes.
 */
public final class CaptureStickNetwork {
    public static final Identifier REQUEST = new Identifier("rsl", "capture_stick_request");
    public static final Identifier OPEN = new Identifier("rsl", "capture_stick_open");
    public static final Identifier CONFIRM = new Identifier("rsl", "capture_stick_confirm");
    public static final Identifier CANCEL = new Identifier("rsl", "capture_stick_cancel");

    public static final int MAX_NAME_LENGTH = 32;
    private static final long TIMEOUT_MS = 60_000L;
    private static final String CUSTOM_NAME = "CustomName";
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private record Pending(String id, String state, String payload, long createdAt) {
    }

    private CaptureStickNetwork() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST,
                (server, player, handler, buf, sender) -> server.execute(() -> give(player)));
        ServerPlayNetworking.registerGlobalReceiver(CONFIRM,
                (server, player, handler, buf, sender) -> {
                    String name = buf.readString(MAX_NAME_LENGTH + 8);
                    server.execute(() -> confirm(player, name));
                });
        ServerPlayNetworking.registerGlobalReceiver(CANCEL,
                (server, player, handler, buf, sender) -> server.execute(() -> PENDING.remove(player.getUuid())));
    }

    private static void give(ServerPlayerEntity player) {
        ItemStack stack = new ItemStack(RandomSkyBlockIsland.CAPTURE_STICK);
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
        player.sendMessage(Text.translatable("rsl.message.stick_given"), true);
    }

    /**
     * Stores a captured container on the server and asks the client to open the
     * rename screen. The NBT is kept server-side so a modified client cannot forge it.
     */
    public static void beginCapture(ServerPlayerEntity player, String id, String state, String payload, String defaultName) {
        PENDING.put(player.getUuid(), new Pending(id, state, payload, System.currentTimeMillis()));
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(state, 512);
        buf.writeString(defaultName == null ? "" : defaultName, 256);
        ServerPlayNetworking.send(player, OPEN, buf);
    }

    private static void confirm(ServerPlayerEntity player, String rawName) {
        Pending pending = PENDING.remove(player.getUuid());
        if (pending == null || System.currentTimeMillis() - pending.createdAt() > TIMEOUT_MS) {
            player.sendMessage(Text.translatable("rsl.message.capture_failed"), false);
            return;
        }
        NbtCompound nbt = NbtPayload.decode(pending.payload());
        if (nbt == null) {
            player.sendMessage(Text.translatable("rsl.message.capture_failed"), false);
            return;
        }
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        if (name.isEmpty()) {
            nbt.remove(CUSTOM_NAME);
        } else {
            nbt.putString(CUSTOM_NAME, Text.Serializer.toJson(Text.literal(name)));
        }
        String payload = NbtPayload.encode(nbt);
        if (payload.isEmpty() || payload.length() > PoolEntry.MAX_NBT_LENGTH) {
            player.sendMessage(Text.translatable("rsl.message.capture_too_large"), false);
            return;
        }
        BlockMutationResult result = ModConfig.addNbtPoolEntry(new PoolEntry(pending.id(), pending.state(), payload));
        Text message = switch (result) {
            case OK -> Text.translatable("rsl.message.capture_added");
            case DUPLICATE -> Text.translatable("rsl.message.capture_exists");
            case POOL_FULL -> Text.translatable("rsl.message.capture_pool_full");
            default -> Text.translatable("rsl.message.capture_failed");
        };
        if (result == BlockMutationResult.OK) {
            yifei.rsl.game.SoundFx.play(player, net.minecraft.sound.SoundEvents.ITEM_BOTTLE_FILL, 0.6F, 1.0F);
        } else {
            yifei.rsl.game.SoundFx.play(player, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
        }
        player.sendMessage(message, false);
        RandomSkyBlockIsland.LOGGER.info("[RSL] Captured container {} ({})", pending.id(), result);
    }

    /** Sends a packet used only as an empty trigger. */
    public static PacketByteBuf emptyRequest() {
        return PacketByteBufs.create();
    }

    /** Utility used by the capture handler to read a container's current display name. */
    public static String displayName(NbtCompound nbt) {
        if (nbt == null || !nbt.contains(CUSTOM_NAME)) {
            return "";
        }
        try {
            return Text.Serializer.fromJson(nbt.getString(CUSTOM_NAME)).getString();
        } catch (Exception e) {
            return "";
        }
    }
}
