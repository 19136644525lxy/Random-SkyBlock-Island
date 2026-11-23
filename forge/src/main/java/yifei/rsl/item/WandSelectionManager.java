package yifei.rsl.item;

import net.minecraft.server.level.ServerPlayer;
import yifei.rsl.common.wand.WandSelection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side per-player wand selection. The map is only touched from the
 * server thread.
 */
public final class WandSelectionManager {
    private static final Map<UUID, WandSelection> SELECTIONS = new HashMap<>();

    private WandSelectionManager() {
    }

    public static WandSelection getOrCreate(ServerPlayer player) {
        return SELECTIONS.computeIfAbsent(player.getUUID(), uuid -> new WandSelection());
    }

    public static void remove(UUID uuid) {
        SELECTIONS.remove(uuid);
    }

    public static void clear() {
        SELECTIONS.clear();
    }
}
