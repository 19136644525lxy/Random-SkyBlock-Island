package yifei.rsl.client;

import yifei.rsl.common.config.PoolAccessPlayer;
import yifei.rsl.common.config.PoolAccessPlayers;
import yifei.rsl.common.config.PoolAccessResult;
import yifei.rsl.network.AccessNetwork;

import java.util.List;
import java.util.function.Consumer;

/**
 * Forge client half of the access-management channel. Keeps the last player list
 * so a freshly opened screen can show it immediately and forwards new payloads to
 * the listener installed by {@code PoolAccessScreen}.
 */
public final class ClientAccessHandler {
    private static volatile Consumer<PoolAccessPlayers> playersListener;
    private static volatile Consumer<PoolAccessResult> resultListener;
    private static volatile PoolAccessPlayers lastPlayers;

    private ClientAccessHandler() {
    }

    public static void setHandlers(Consumer<PoolAccessPlayers> players,
            Consumer<PoolAccessResult> result) {
        playersListener = players;
        resultListener = result;
    }

    public static PoolAccessPlayers getLastPlayers() {
        return lastPlayers;
    }

    public static void players(boolean canManage, List<PoolAccessPlayer> rows) {
        PoolAccessPlayers payload = new PoolAccessPlayers(canManage, rows);
        lastPlayers = payload;
        Consumer<PoolAccessPlayers> current = playersListener;
        if (current != null) {
            current.accept(payload);
        }
    }

    public static void result(PoolAccessResult result) {
        Consumer<PoolAccessResult> current = resultListener;
        if (current != null) {
            current.accept(result);
        }
    }

    public static void requestPlayers() {
        AccessNetwork.sendRequest();
    }

    public static void sendAction(String action, List<String> uuids) {
        AccessNetwork.sendAction(action, uuids);
    }
}
