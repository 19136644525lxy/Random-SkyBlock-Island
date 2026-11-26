package yifei.rsl.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yifei.rsl.network.EggNetwork;

import java.util.List;
import java.util.function.Consumer;

/**
 * Client-side egg-mode state. Handlers are swapped by the open screen and
 * cleared when it closes.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientEggHandler {
    public record Snapshot(boolean enabled, double chance, int volume, int minRadius, int maxRadius,
                           int minDistance, List<String> allIds, List<String> selectedIds) {
    }

    public record Result(boolean success, String messageKey) {
    }

    private static Consumer<Snapshot> snapshotHandler = snapshot -> {
    };
    private static Consumer<Result> resultHandler = result -> {
    };

    private ClientEggHandler() {
    }

    public static void setHandlers(Consumer<Snapshot> snapshot, Consumer<Result> result) {
        snapshotHandler = snapshot == null ? value -> {
        } : snapshot;
        resultHandler = result == null ? value -> {
        } : result;
    }

    public static void snapshot(boolean enabled, double chance, int volume, int minRadius, int maxRadius,
                                int minDistance, List<String> allIds, List<String> selectedIds) {
        snapshotHandler.accept(new Snapshot(enabled, chance, volume, minRadius, maxRadius,
                minDistance, allIds, selectedIds));
    }

    public static void result(boolean success, String messageKey) {
        resultHandler.accept(new Result(success, messageKey));
    }

    public static void request() {
        EggNetwork.sendRequest();
    }

    public static void toggle() {
        EggNetwork.sendToggle();
    }

    public static void apply(boolean enabled, double chance, int volume, List<String> selected) {
        EggNetwork.sendApply(enabled, chance, volume, selected);
    }
}
