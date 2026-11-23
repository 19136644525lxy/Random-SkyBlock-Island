package yifei.rsl.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative action channel for the gameplay menu. The client sends an
 * action id (start/stop/reset/wand/areas/status/delay); the server runs the same
 * command a player could type, captures every feedback line through a custom
 * {@link CommandOutput}, and sends the localized lines back so the menu can show
 * the result inline instead of hiding it in chat.
 */
public final class GameplayActionNetwork {
    public static final Identifier REQUEST = new Identifier("rsl", "gameplay_action_request");
    public static final Identifier RESULT = new Identifier("rsl", "gameplay_action_result");

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_RESET = "reset";
    public static final String ACTION_WAND = "wand";
    public static final String ACTION_AREAS = "areas";
    public static final String ACTION_STATUS = "status";
    public static final String ACTION_DELAY = "delay";

    private GameplayActionNetwork() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) -> {
            String action = buf.readString(32);
            int delay = buf.readVarInt();
            server.execute(() -> execute(server, player, action, delay));
        });
    }

    public static void sendResult(ServerPlayerEntity player, List<Text> lines) {
        if (player == null) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        writeLines(buf, lines);
        ServerPlayNetworking.send(player, RESULT, buf);
    }

    public static void writeLines(PacketByteBuf buf, List<Text> lines) {
        buf.writeVarInt(lines.size());
        for (Text line : lines) {
            buf.writeString(Text.Serializer.toJson(line), 262144);
        }
    }

    public static List<Text> readLines(PacketByteBuf buf) {
        int count = buf.readVarInt();
        List<Text> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(Text.Serializer.fromJson(buf.readString(262144)));
        }
        return lines;
    }

    private static void execute(MinecraftServer server, ServerPlayerEntity player, String action, int delay) {
        String command = commandFor(action, delay);
        if (command == null) {
            return;
        }
        List<Text> lines = new ArrayList<>();
        CommandOutput output = new CommandOutput() {
            @Override
            public void sendMessage(Text message) {
                lines.add(message);
            }

            @Override
            public boolean shouldReceiveFeedback() {
                return true;
            }

            @Override
            public boolean shouldTrackOutput() {
                return true;
            }

            @Override
            public boolean shouldBroadcastConsoleToOps() {
                return false;
            }
        };
        ServerCommandSource source = player.getCommandSource().withOutput(output);
        int result = server.getCommandManager().executeWithPrefix(source, command);
        if (lines.isEmpty()) {
            lines.add(Text.translatable(result > 0 ? "rsl.menu.action_done" : "rsl.menu.action_failed"));
        }
        sendResult(player, lines);
    }

    private static String commandFor(String action, int delay) {
        return switch (action) {
            case ACTION_START -> "randomskyblock start";
            case ACTION_STOP -> "randomskyblock stop";
            case ACTION_RESET -> "randomskyblock reset";
            case ACTION_WAND -> "randomskyblock wand";
            case ACTION_AREAS -> "randomskyblock areas";
            case ACTION_STATUS -> "randomskyblock status";
            case ACTION_DELAY -> delay >= 1 && delay <= 3600 ? "randomskyblock delay " + delay : null;
            default -> null;
        };
    }
}
