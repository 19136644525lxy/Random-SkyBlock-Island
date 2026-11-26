package yifei.rsl.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client receivers for the egg-mode channel. The handlers are swapped by the
 * open screen and cleared when it closes.
 */
@Environment(EnvType.CLIENT)
public final class EggClientNetwork {
    public record Snapshot(boolean enabled, double chance, int volume, int minRadius, int maxRadius,
                           int minDistance, List<String> allIds, List<String> selectedIds) {
    }

    public record Result(boolean success, String messageKey) {
    }

    private static Consumer<Snapshot> snapshotHandler = snapshot -> {
    };
    private static Consumer<Result> resultHandler = result -> {
    };

    private EggClientNetwork() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(EggNetwork.SNAPSHOT, (client, handler, buf, sender) -> {
            Snapshot snapshot = readSnapshot(buf);
            client.execute(() -> snapshotHandler.accept(snapshot));
        });
        ClientPlayNetworking.registerGlobalReceiver(EggNetwork.RESULT, (client, handler, buf, sender) -> {
            boolean success = buf.readBoolean();
            String key = buf.readString();
            client.execute(() -> resultHandler.accept(new Result(success, key)));
        });
    }

    public static void setHandlers(Consumer<Snapshot> snapshot, Consumer<Result> result) {
        snapshotHandler = snapshot == null ? value -> {
        } : snapshot;
        resultHandler = result == null ? value -> {
        } : result;
    }

    public static void request() {
        ClientPlayNetworking.send(EggNetwork.REQUEST, PacketByteBufs.create());
    }

    public static void toggle() {
        ClientPlayNetworking.send(EggNetwork.TOGGLE, PacketByteBufs.create());
    }

    public static void apply(boolean enabled, double chance, int volume, List<String> selected) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(enabled);
        buf.writeDouble(chance);
        buf.writeInt(volume);
        buf.writeInt(selected.size());
        for (String id : selected) {
            buf.writeString(id == null ? "" : id);
        }
        ClientPlayNetworking.send(EggNetwork.APPLY, buf);
    }

    private static Snapshot readSnapshot(PacketByteBuf buf) {
        boolean enabled = buf.readBoolean();
        double chance = buf.readDouble();
        int volume = buf.readInt();
        int minRadius = buf.readInt();
        int maxRadius = buf.readInt();
        int minDistance = buf.readInt();
        List<String> all = readIds(buf);
        List<String> selected = readIds(buf);
        return new Snapshot(enabled, chance, volume, minRadius, maxRadius, minDistance, all, selected);
    }

    private static List<String> readIds(PacketByteBuf buf) {
        int size = Math.max(0, Math.min(buf.readInt(), 4096));
        List<String> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readString());
        }
        return ids;
    }
}
