package yifei.rsl.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import yifei.rsl.network.GameplayActionNetwork;

import java.util.List;
import java.util.function.Consumer;

/**
 * Client half of the gameplay action channel. Keeps the last result lines so a
 * freshly opened menu can show them immediately, and forwards new results to
 * the listener installed by {@code GameplayScreen}.
 */
public final class GameplayActionClientNetwork {
    private static volatile Consumer<List<Text>> listener;
    private static volatile List<Text> lastLines = List.of();

    private GameplayActionClientNetwork() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(GameplayActionNetwork.RESULT,
                (client, handler, buf, sender) -> {
                    List<Text> lines = List.copyOf(GameplayActionNetwork.readLines(buf));
                    client.execute(() -> {
                        lastLines = lines;
                        Consumer<List<Text>> current = listener;
                        if (current != null) {
                            current.accept(lines);
                        }
                    });
                });
    }

    public static void setHandler(Consumer<List<Text>> handler) {
        listener = handler;
    }

    public static List<Text> getLastLines() {
        return lastLines;
    }

    public static void request(String action, int delay) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(action, 32);
        buf.writeVarInt(delay);
        ClientPlayNetworking.send(GameplayActionNetwork.REQUEST, buf);
    }
}
