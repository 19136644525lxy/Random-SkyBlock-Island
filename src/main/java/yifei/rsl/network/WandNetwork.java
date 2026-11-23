package yifei.rsl.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.common.wand.WandAction;
import yifei.rsl.common.wand.WandResult;
import yifei.rsl.common.wand.WandScreenData;
import yifei.rsl.common.wand.WandSelection;
import yifei.rsl.config.ModConfig;
import yifei.rsl.item.WandSelectionManager;

/**
 * Server-safe wand channel. Client-only receivers live in WandClientNetwork.
 */
public final class WandNetwork {
    public static final Identifier OPEN = new Identifier("rsl", "wand_open");
    public static final Identifier ACTION = new Identifier("rsl", "wand_action");
    public static final Identifier RESULT = new Identifier("rsl", "wand_result");

    private WandNetwork() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, sender) -> {
            WandAction action = readAction(buf);
            server.execute(() -> handleAction(server, player, action));
        });
    }

    public static void sendOpen(ServerPlayerEntity player, WandScreenData data) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        writeData(buf, data);
        ServerPlayNetworking.send(player, OPEN, buf);
    }

    private static void sendResult(ServerPlayerEntity player, WandResult result) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        writeResult(buf, result);
        ServerPlayNetworking.send(player, RESULT, buf);
    }

    private static void handleAction(MinecraftServer server, ServerPlayerEntity player, WandAction action) {
        if (!player.hasPermissionLevel(ModConfig.getGamePermissionLevel())) {
            sendResult(player, new WandResult(false, "rsl.message.wand_no_permission",
                    String.valueOf(ModConfig.getGamePermissionLevel())));
            return;
        }
        if (ModConfig.isDimensionLocked() && !ModConfig.isPlayerInActiveDimension(player)) {
            sendResult(player, new WandResult(false, "rsl.message.dimension_locked",
                    ModConfig.getActiveDimension()));
            return;
        }

        WandSelection selection = WandSelectionManager.getOrCreate(player);

        if (action.type == WandAction.Type.CANCEL) {
            selection.clear();
            sendResult(player, new WandResult(true, "rsl.message.wand_cancelled", ""));
            return;
        }
        if (action.type == WandAction.Type.RESELECT) {
            AreaSpec target = targetOrNull(player, action.targetName);
            if (target == null) {
                return;
            }
            selection.beginEdit(target.name);
            sendResult(player, new WandResult(true, "rsl.message.wand_reselect", target.name));
            return;
        }
        if (action.type == WandAction.Type.DELETE) {
            AreaSpec target = targetOrNull(player, action.targetName);
            if (target == null) {
                return;
            }
            AreaSpec backup = target.copy();
            if (!ModConfig.removeArea(target.name) || !ModConfig.saveSaveAreas()) {
                if (ModConfig.getAreaByName(target.name) == null) {
                    ModConfig.addArea(backup.name, backup.minX, backup.minY, backup.minZ,
                            backup.maxX, backup.maxY, backup.maxZ);
                }
                sendResult(player, new WandResult(false, "rsl.message.wand_save_failed", ""));
                return;
            }
            selection.clear();
            sendResult(player, new WandResult(true, "rsl.message.wand_deleted", target.name));
            return;
        }

        String name = action.name == null ? "" : action.name.trim();
        switch (action.type) {
            case ADD -> {
                if (!ensureSelectionReady(player, selection)) {
                    return;
                }
                addArea(player, selection, name);
            }
            case RENAME -> renameArea(player, selection, action.targetName, name);
            case SAVE_EDIT -> {
                if (selection.isEditing() && !ensureSelectionReady(player, selection)) {
                    return;
                }
                saveEdit(player, selection, action.targetName, name);
            }
            default -> sendResult(player, new WandResult(false, "rsl.message.wand_not_ready", ""));
        }
    }

    private static boolean ensureSelectionReady(ServerPlayerEntity player, WandSelection selection) {
        if (!selection.isReady()) {
            sendResult(player, new WandResult(false, "rsl.message.wand_not_ready", ""));
            return false;
        }
        String currentDimension = player.getServerWorld().getRegistryKey().getValue().toString();
        if (!currentDimension.equals(selection.dimension())) {
            selection.clear();
            sendResult(player, new WandResult(false, "rsl.message.wand_not_ready", ""));
            return false;
        }
        return true;
    }

    private static AreaSpec targetOrNull(ServerPlayerEntity player, String targetName) {
        AreaSpec target = targetName == null ? null : ModConfig.getAreaByName(targetName);
        if (target == null) {
            sendResult(player, new WandResult(false, "rsl.message.wand_target_missing", ""));
            return null;
        }
        AreaSpec defaultArea = ModConfig.getDefaultArea();
        if (defaultArea != null && target.name.equals(defaultArea.name)) {
            sendResult(player, new WandResult(false, "rsl.message.wand_default_protected", ""));
            return null;
        }
        return target;
    }

    private static void addArea(ServerPlayerEntity player, WandSelection selection, String name) {
        AreaSpec area = selection.toArea(name);
        if (area == null || !area.hasValidName() || !ModConfig.isValidArea(area)) {
            sendResult(player, new WandResult(false, "rsl.message.wand_invalid_name", ""));
            return;
        }
        if (!ModConfig.addArea(area.name, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)) {
            sendResult(player, new WandResult(false, "rsl.message.wand_duplicate", name));
            return;
        }
        if (!ModConfig.saveSaveAreas()) {
            ModConfig.removeArea(name);
            sendResult(player, new WandResult(false, "rsl.message.wand_save_failed", ""));
            return;
        }
        selection.clear();
        sendResult(player, new WandResult(true, "rsl.message.wand_added", name));
    }

    private static void renameArea(ServerPlayerEntity player, WandSelection selection,
            String targetName, String name) {
        AreaSpec target = targetOrNull(player, targetName);
        if (target == null) {
            return;
        }
        if (!ModConfig.replaceArea(target.name, name, target.minX, target.minY, target.minZ,
                target.maxX, target.maxY, target.maxZ)) {
            sendResult(player, new WandResult(false, "rsl.message.wand_rename_failed", name));
            return;
        }
        selection.clear();
        sendResult(player, new WandResult(true, "rsl.message.wand_renamed", name));
    }

    private static void saveEdit(ServerPlayerEntity player, WandSelection selection,
            String targetName, String name) {
        AreaSpec target = targetOrNull(player, targetName);
        if (target == null) {
            return;
        }
        if (selection.isEditing()) {
            AreaSpec area = selection.toArea(name);
            if (area == null || !area.hasValidName() || !ModConfig.isValidArea(area)) {
                sendResult(player, new WandResult(false, "rsl.message.wand_invalid_name", ""));
                return;
            }
            if (!ModConfig.replaceArea(target.name, name, area.minX, area.minY, area.minZ,
                    area.maxX, area.maxY, area.maxZ)) {
                sendResult(player, new WandResult(false, "rsl.message.wand_rename_failed", name));
                return;
            }
        } else if (!ModConfig.replaceArea(target.name, name, target.minX, target.minY, target.minZ,
                target.maxX, target.maxY, target.maxZ)) {
            sendResult(player, new WandResult(false, "rsl.message.wand_rename_failed", name));
            return;
        }
        selection.clear();
        sendResult(player, new WandResult(true, "rsl.message.wand_saved", name));
    }

    public static void writeData(PacketByteBuf buf, WandScreenData data) {
        buf.writeString(data.mode.name());
        buf.writeBoolean(data.originalName != null);
        if (data.originalName != null) {
            buf.writeString(data.originalName);
        }
        buf.writeString(data.proposedName);
        buf.writeLong(data.volume);
        buf.writeInt(data.x1);
        buf.writeInt(data.y1);
        buf.writeInt(data.z1);
        buf.writeInt(data.x2);
        buf.writeInt(data.y2);
        buf.writeInt(data.z2);
    }

    public static WandScreenData readData(PacketByteBuf buf) {
        WandScreenData.Mode mode;
        try {
            mode = WandScreenData.Mode.valueOf(buf.readString());
        } catch (IllegalArgumentException ex) {
            mode = WandScreenData.Mode.CREATE;
        }
        String originalName = buf.readBoolean() ? buf.readString() : null;
        String proposedName = buf.readString();
        long volume = buf.readLong();
        return new WandScreenData(mode, originalName, proposedName, volume,
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void writeAction(PacketByteBuf buf, WandAction action) {
        buf.writeString(action.type.name());
        buf.writeString(action.name == null ? "" : action.name);
        buf.writeBoolean(action.targetName != null);
        if (action.targetName != null) {
            buf.writeString(action.targetName);
        }
    }

    public static WandAction readAction(PacketByteBuf buf) {
        WandAction.Type type;
        try {
            type = WandAction.Type.valueOf(buf.readString());
        } catch (IllegalArgumentException ex) {
            type = WandAction.Type.CANCEL;
        }
        String name = buf.readString();
        String target = buf.readBoolean() ? buf.readString() : null;
        return new WandAction(type, name, target);
    }

    public static void writeResult(PacketByteBuf buf, WandResult result) {
        buf.writeBoolean(result.success);
        buf.writeString(result.messageKey);
        buf.writeString(result.detail == null ? "" : result.detail);
    }

    public static WandResult readResult(PacketByteBuf buf) {
        return new WandResult(buf.readBoolean(), buf.readString(), buf.readString());
    }
}
