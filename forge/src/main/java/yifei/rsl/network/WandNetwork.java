package yifei.rsl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yifei.rsl.client.ClientWandHandler;
import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.common.wand.WandAction;
import yifei.rsl.common.wand.WandResult;
import yifei.rsl.common.wand.WandScreenData;
import yifei.rsl.common.wand.WandSelection;
import yifei.rsl.config.ModConfig;
import yifei.rsl.item.WandSelectionManager;

import java.util.function.Supplier;

/**
 * Forge wand channel. Client-only classes are touched exclusively inside a
 * DistExecutor client lambda.
 */
public final class WandNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("rsl", "wand"), () -> "1", "1"::equals, "1"::equals);

    private WandNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, OpenS2C.class, OpenS2C::encode, OpenS2C::decode, OpenS2C::handle);
        CHANNEL.registerMessage(1, ActionC2S.class, ActionC2S::encode, ActionC2S::decode, ActionC2S::handle);
        CHANNEL.registerMessage(2, ResultS2C.class, ResultS2C::encode, ResultS2C::decode, ResultS2C::handle);
    }

    public static void sendOpen(ServerPlayer player, WandScreenData data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenS2C(data));
    }

    public static void sendActionToServer(WandAction action) {
        CHANNEL.sendToServer(new ActionC2S(action));
    }

    private static void sendResult(ServerPlayer player, WandResult result) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ResultS2C(result));
    }

    private static void handleAction(ServerPlayer player, WandAction action) {
        if (!player.hasPermissions(ModConfig.getGamePermissionLevel())) {
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

    private static boolean ensureSelectionReady(ServerPlayer player, WandSelection selection) {
        if (!selection.isReady()) {
            sendResult(player, new WandResult(false, "rsl.message.wand_not_ready", ""));
            return false;
        }
        String currentDimension = player.serverLevel().dimension().location().toString();
        if (!currentDimension.equals(selection.dimension())) {
            selection.clear();
            sendResult(player, new WandResult(false, "rsl.message.wand_not_ready", ""));
            return false;
        }
        return true;
    }

    private static AreaSpec targetOrNull(ServerPlayer player, String targetName) {
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

    private static void addArea(ServerPlayer player, WandSelection selection, String name) {
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

    private static void renameArea(ServerPlayer player, WandSelection selection,
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

    private static void saveEdit(ServerPlayer player, WandSelection selection,
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

    public static void writeData(FriendlyByteBuf buf, WandScreenData data) {
        buf.writeUtf(data.mode.name());
        buf.writeBoolean(data.originalName != null);
        if (data.originalName != null) {
            buf.writeUtf(data.originalName);
        }
        buf.writeUtf(data.proposedName);
        buf.writeLong(data.volume);
        buf.writeInt(data.x1);
        buf.writeInt(data.y1);
        buf.writeInt(data.z1);
        buf.writeInt(data.x2);
        buf.writeInt(data.y2);
        buf.writeInt(data.z2);
    }

    public static WandScreenData readData(FriendlyByteBuf buf) {
        WandScreenData.Mode mode;
        try {
            mode = WandScreenData.Mode.valueOf(buf.readUtf());
        } catch (IllegalArgumentException ex) {
            mode = WandScreenData.Mode.CREATE;
        }
        String originalName = buf.readBoolean() ? buf.readUtf() : null;
        String proposedName = buf.readUtf();
        long volume = buf.readLong();
        return new WandScreenData(mode, originalName, proposedName, volume,
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void writeAction(FriendlyByteBuf buf, WandAction action) {
        buf.writeUtf(action.type.name());
        buf.writeUtf(action.name == null ? "" : action.name);
        buf.writeBoolean(action.targetName != null);
        if (action.targetName != null) {
            buf.writeUtf(action.targetName);
        }
    }

    public static WandAction readAction(FriendlyByteBuf buf) {
        WandAction.Type type;
        try {
            type = WandAction.Type.valueOf(buf.readUtf());
        } catch (IllegalArgumentException ex) {
            type = WandAction.Type.CANCEL;
        }
        String name = buf.readUtf();
        String target = buf.readBoolean() ? buf.readUtf() : null;
        return new WandAction(type, name, target);
    }

    public static class OpenS2C {
        private final WandScreenData data;

        public OpenS2C(WandScreenData data) {
            this.data = data;
        }

        private OpenS2C(FriendlyByteBuf buf) {
            this.data = readData(buf);
        }

        public static void encode(OpenS2C msg, FriendlyByteBuf buf) {
            writeData(buf, msg.data);
        }

        public static OpenS2C decode(FriendlyByteBuf buf) {
            return new OpenS2C(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> ClientWandHandler.open(data)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ActionC2S {
        private final WandAction action;

        public ActionC2S(WandAction action) {
            this.action = action;
        }

        private ActionC2S(FriendlyByteBuf buf) {
            this.action = readAction(buf);
        }

        public static void encode(ActionC2S msg, FriendlyByteBuf buf) {
            writeAction(buf, msg.action);
        }

        public static ActionC2S decode(FriendlyByteBuf buf) {
            return new ActionC2S(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    handleAction(player, action);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ResultS2C {
        private final WandResult result;

        public ResultS2C(WandResult result) {
            this.result = result;
        }

        private ResultS2C(FriendlyByteBuf buf) {
            this.result = new WandResult(buf.readBoolean(), buf.readUtf(), buf.readUtf());
        }

        public static void encode(ResultS2C msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.result.success);
            buf.writeUtf(msg.result.messageKey);
            buf.writeUtf(msg.result.detail == null ? "" : msg.result.detail);
        }

        public static ResultS2C decode(FriendlyByteBuf buf) {
            return new ResultS2C(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> ClientWandHandler.result(result)));
            ctx.get().setPacketHandled(true);
        }
    }
}
