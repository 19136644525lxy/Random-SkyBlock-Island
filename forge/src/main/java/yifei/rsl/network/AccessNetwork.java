package yifei.rsl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yifei.rsl.client.ClientAccessHandler;
import yifei.rsl.common.config.PoolAccess;
import yifei.rsl.common.config.PoolAccessEntry;
import yifei.rsl.common.config.PoolAccessPlayer;
import yifei.rsl.common.config.PoolAccessResult;
import yifei.rsl.config.ModConfig;
import yifei.rsl.permission.PoolPermissions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Forge server-authoritative access-management channel. Mirrors the Fabric
 * AccessNetwork: send the online player list with roles, accept bulk grant/revoke
 * requests, and persist through the same ModConfig ACL methods as the command.
 */
public final class AccessNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("rsl", "access_settings"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static final String GRANT_EDITOR = "grant";
    public static final String GRANT_OWNER = "grant_owner";
    public static final String REVOKE = "revoke";

    private static final int MAX_PLAYERS = 256;
    private static final int MAX_BATCH = 256;

    private AccessNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, RequestC2S.class, RequestC2S::encode, RequestC2S::decode,
                (message, supplier) -> {
                    NetworkEvent.Context context = supplier.get();
                    context.enqueueWork(() -> sendPlayers(context.getSender()));
                    context.setPacketHandled(true);
                });
        CHANNEL.registerMessage(1, PlayersS2C.class, PlayersS2C::encode, PlayersS2C::decode,
                (message, supplier) -> {
                    NetworkEvent.Context context = supplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientAccessHandler.players(message.canManage, message.players)));
                    context.setPacketHandled(true);
                });
        CHANNEL.registerMessage(2, ActionC2S.class, ActionC2S::encode, ActionC2S::decode,
                (message, supplier) -> {
                    NetworkEvent.Context context = supplier.get();
                    context.enqueueWork(() -> handleAction(context.getSender(), message.action, message.uuids));
                    context.setPacketHandled(true);
                });
        CHANNEL.registerMessage(3, ResultS2C.class, ResultS2C::encode, ResultS2C::decode,
                (message, supplier) -> {
                    NetworkEvent.Context context = supplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientAccessHandler.result(message.result)));
                    context.setPacketHandled(true);
                });
    }

    public static void sendRequest() {
        CHANNEL.sendToServer(new RequestC2S());
    }

    public static void sendAction(String action, List<String> uuids) {
        CHANNEL.sendToServer(new ActionC2S(action, uuids));
    }

    private static void sendPlayers(ServerPlayer player) {
        if (player == null) {
            return;
        }
        boolean canManage = PoolPermissions.canManage(player);
        List<PoolAccessPlayer> rows = new ArrayList<>();
        MinecraftServer server = player.getServer();
        if (canManage && server != null) {
            List<PoolAccessEntry> entries = ModConfig.getPoolAccess();
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                PoolAccessEntry entry = PoolAccess.find(entries, online.getUUID());
                rows.add(new PoolAccessPlayer(online.getUUID().toString(),
                        online.getGameProfile().getName(),
                        entry == null ? "" : entry.role.name()));
            }
            rows.sort(Comparator.comparing(row -> row.name, String.CASE_INSENSITIVE_ORDER));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayersS2C(canManage, rows));
    }

    private static void sendResult(ServerPlayer player, PoolAccessResult result) {
        if (player == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ResultS2C(result));
    }

    private static void handleAction(ServerPlayer player, String action, List<String> uuids) {
        if (player == null) {
            return;
        }
        if (!PoolPermissions.canManage(player)) {
            sendResult(player, new PoolAccessResult(false, "rsl.access.no_manage", 0, 0, List.of()));
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            sendResult(player, new PoolAccessResult(false, "rsl.access.failed", 0, 0, List.of()));
            return;
        }
        String normalized = switch (action) {
            case GRANT_EDITOR, GRANT_OWNER, REVOKE -> action;
            default -> null;
        };
        if (normalized == null || uuids.size() > MAX_BATCH) {
            sendResult(player, new PoolAccessResult(false, "rsl.access.failed", 0, 0, List.of()));
            return;
        }
        int changed = 0;
        int skipped = 0;
        List<String> failed = new ArrayList<>();
        for (String uuidText : uuids) {
            UUID uuid = parseUuid(uuidText);
            if (uuid == null) {
                failed.add(uuidText);
                continue;
            }
            ServerPlayer target = server.getPlayerList().getPlayer(uuid);
            if (target == null) {
                failed.add(uuidText);
                continue;
            }
            String name = target.getGameProfile().getName();
            PoolAccessEntry existing = PoolAccess.find(ModConfig.getPoolAccess(), uuid);
            switch (normalized) {
                case GRANT_EDITOR, GRANT_OWNER -> {
                    boolean owner = GRANT_OWNER.equals(normalized);
                    if (existing != null && (owner ? existing.role == PoolAccessEntry.Role.OWNER : true)) {
                        skipped++;
                    } else if (ModConfig.grantPoolAccess(target, owner)) {
                        changed++;
                    } else {
                        failed.add(name);
                    }
                }
                case REVOKE -> {
                    if (existing == null) {
                        skipped++;
                    } else if (ModConfig.revokePoolAccess(uuid)) {
                        changed++;
                    } else {
                        failed.add(name);
                    }
                }
                default -> failed.add(name);
            }
        }
        boolean success = failed.isEmpty();
        sendResult(player, new PoolAccessResult(success,
                success ? "rsl.access.done" : "rsl.access.failed", changed, skipped, failed));
    }

    private static UUID parseUuid(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(text.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static final class RequestC2S {
        private RequestC2S() {
        }

        public static void encode(RequestC2S message, FriendlyByteBuf buf) {
        }

        public static RequestC2S decode(FriendlyByteBuf buf) {
            return new RequestC2S();
        }
    }

    public static final class PlayersS2C {
        private final boolean canManage;
        private final List<PoolAccessPlayer> players;

        public PlayersS2C(boolean canManage, List<PoolAccessPlayer> players) {
            this.canManage = canManage;
            this.players = List.copyOf(players);
        }

        public static void encode(PlayersS2C message, FriendlyByteBuf buf) {
            buf.writeBoolean(message.canManage);
            buf.writeVarInt(message.players.size());
            for (PoolAccessPlayer row : message.players) {
                buf.writeUtf(row.uuid == null ? "" : row.uuid, 64);
                buf.writeUtf(row.name == null ? "" : row.name, 64);
                buf.writeUtf(row.role == null ? "" : row.role, 16);
            }
        }

        public static PlayersS2C decode(FriendlyByteBuf buf) {
            boolean canManage = buf.readBoolean();
            int count = Math.max(0, Math.min(buf.readVarInt(), MAX_PLAYERS));
            List<PoolAccessPlayer> rows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                rows.add(new PoolAccessPlayer(buf.readUtf(64), buf.readUtf(64), buf.readUtf(16)));
            }
            return new PlayersS2C(canManage, rows);
        }
    }

    public static final class ActionC2S {
        private final String action;
        private final List<String> uuids;

        public ActionC2S(String action, List<String> uuids) {
            this.action = action == null ? "" : action;
            this.uuids = List.copyOf(uuids);
        }

        public static void encode(ActionC2S message, FriendlyByteBuf buf) {
            buf.writeUtf(message.action, 32);
            buf.writeVarInt(message.uuids.size());
            for (String uuid : message.uuids) {
                buf.writeUtf(uuid == null ? "" : uuid, 64);
            }
        }

        public static ActionC2S decode(FriendlyByteBuf buf) {
            String action = buf.readUtf(32);
            int count = Math.max(0, Math.min(buf.readVarInt(), MAX_BATCH));
            List<String> uuids = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                uuids.add(buf.readUtf(64));
            }
            return new ActionC2S(action, uuids);
        }
    }

    public static final class ResultS2C {
        private final PoolAccessResult result;

        public ResultS2C(PoolAccessResult result) {
            this.result = result;
        }

        public static void encode(ResultS2C message, FriendlyByteBuf buf) {
            buf.writeBoolean(message.result.success);
            buf.writeUtf(message.result.messageKey == null ? "" : message.result.messageKey, 64);
            buf.writeVarInt(message.result.changed);
            buf.writeVarInt(message.result.skipped);
            buf.writeVarInt(message.result.failedNames.size());
            for (String name : message.result.failedNames) {
                buf.writeUtf(name == null ? "" : name, 64);
            }
        }

        public static ResultS2C decode(FriendlyByteBuf buf) {
            boolean success = buf.readBoolean();
            String key = buf.readUtf(64);
            int changed = buf.readVarInt();
            int skipped = buf.readVarInt();
            int count = Math.max(0, Math.min(buf.readVarInt(), MAX_BATCH));
            List<String> failed = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                failed.add(buf.readUtf(64));
            }
            return new ResultS2C(new PoolAccessResult(success, key, changed, skipped, failed));
        }
    }
}
