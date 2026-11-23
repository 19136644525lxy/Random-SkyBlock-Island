package yifei.rsl.client;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import yifei.rsl.network.GameplayActionNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Forge client half of the gameplay action channel. Keeps the last result lines
 * so a freshly opened menu can show them immediately, and forwards new results
 * to the listener installed by {@code GameplayScreen}.
 */
public final class ClientGameplayActionHandler {
    private static volatile Consumer<List<Component>> listener;
    private static volatile List<Component> lastLines = List.of();

    private ClientGameplayActionHandler() {
    }

    public static void setHandler(Consumer<List<Component>> handler) {
        listener = handler;
    }

    public static List<Component> getLastLines() {
        return lastLines;
    }

    public static void result(List<String> jsonLines) {
        List<Component> lines = new ArrayList<>(jsonLines.size());
        for (String json : jsonLines) {
            lines.add(Component.Serializer.fromJson(JsonParser.parseString(json)));
        }
        List<Component> copy = List.copyOf(lines);
        lastLines = copy;
        Consumer<List<Component>> current = listener;
        if (current != null) {
            current.accept(copy);
        }
    }

    public static void request(String action, int delay) {
        GameplayActionNetwork.sendRequest(action, delay);
    }
}
