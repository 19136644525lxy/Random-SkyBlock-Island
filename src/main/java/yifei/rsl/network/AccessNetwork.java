package yifei.rsl.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import yifei.rsl.common.config.PoolAccess;
import yifei.rsl.common.config.PoolAccessEntry;
import yifei.rsl.common.config.PoolAccessPlayer;
import yifei.rsl.common.config.PoolAccessPlayers;
import yifei.rsl.common.config.PoolAccessResult;
import yifei.rsl.config.ModConfig;
import yifei.rsl.permission.PoolPermissions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative access-management channel. The client asks for the online
 * player list together with their current roles, then sends bulk grant/revoke
 * actions. Every change goes through the same {@link ModConfig} ACL methods the
 * access command uses, so UI and command stay in sync.
 */
public final class AccessNetwork {
	public static final Identifier REQUEST = new Identifier("rsl", "access_request");
	public static final Identifier PLAYERS = new Identifier("rsl", "access_players");
	public static final Identifier ACTION = new Identifier("rsl", "access_action");
	public static final Identifier RESULT = new Identifier("rsl", "access_result");

	public static final String GRANT_EDITOR = "grant";
	public static final String GRANT_OWNER = "grant_owner";
	public static final String REVOKE = "revoke";

	private static final int MAX_PLAYERS = 256;
	private static final int MAX_BATCH = 256;

	private AccessNetwork() {
	}

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) ->
				server.execute(() -> sendPlayers(player)));
		ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, sender) -> {
			String action = buf.readString(32);
			List<String> uuids = readUuids(buf);
			server.execute(() -> handleAction(player, action, uuids));
		});
	}

	public static void sendPlayers(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		boolean canManage = PoolPermissions.canManage(player);
		List<PoolAccessPlayer> rows = new ArrayList<>();
		MinecraftServer server = player.getServer();
		if (canManage && server != null) {
			List<PoolAccessEntry> entries = ModConfig.getPoolAccess();
			for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
				PoolAccessEntry entry = PoolAccess.find(entries, online.getUuid());
				rows.add(new PoolAccessPlayer(online.getUuid().toString(),
						online.getGameProfile().getName(),
						entry == null ? "" : entry.role.name()));
			}
			rows.sort(Comparator.comparing(row -> row.name, String.CASE_INSENSITIVE_ORDER));
		}
		PacketByteBuf buf = PacketByteBufs.create();
		writePlayers(buf, new PoolAccessPlayers(canManage, rows));
		ServerPlayNetworking.send(player, PLAYERS, buf);
	}

	public static void sendResult(ServerPlayerEntity player, PoolAccessResult result) {
		if (player == null) {
			return;
		}
		if (result.success) {
			yifei.rsl.game.SoundFx.play(player, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, 0.4F, 1.4F);
		} else {
			yifei.rsl.game.SoundFx.play(player, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
		}
		PacketByteBuf buf = PacketByteBufs.create();
		writeResult(buf, result);
		ServerPlayNetworking.send(player, RESULT, buf);
	}

	private static void handleAction(ServerPlayerEntity player, String action, List<String> uuids) {
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
			ServerPlayerEntity target = server.getPlayerManager().getPlayer(uuid);
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

	public static void writePlayers(PacketByteBuf buf, PoolAccessPlayers payload) {
		buf.writeBoolean(payload.canManage);
		buf.writeVarInt(Math.min(payload.players.size(), MAX_PLAYERS));
		for (PoolAccessPlayer row : payload.players) {
			buf.writeString(row.uuid, 64);
			buf.writeString(row.name, 64);
			buf.writeString(row.role, 16);
		}
	}

	public static PoolAccessPlayers readPlayers(PacketByteBuf buf) {
		boolean canManage = buf.readBoolean();
		int count = Math.max(0, Math.min(buf.readVarInt(), MAX_PLAYERS));
		List<PoolAccessPlayer> rows = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			rows.add(new PoolAccessPlayer(buf.readString(64), buf.readString(64), buf.readString(16)));
		}
		return new PoolAccessPlayers(canManage, rows);
	}

	public static void writeAction(PacketByteBuf buf, String action, List<String> uuids) {
		buf.writeString(action == null ? "" : action, 32);
		writeUuids(buf, uuids);
	}

	public static void writeResult(PacketByteBuf buf, PoolAccessResult result) {
		buf.writeBoolean(result.success);
		buf.writeString(result.messageKey == null ? "" : result.messageKey, 64);
		buf.writeVarInt(result.changed);
		buf.writeVarInt(result.skipped);
		writeUuids(buf, result.failedNames);
	}

	public static PoolAccessResult readResult(PacketByteBuf buf) {
		boolean success = buf.readBoolean();
		String key = buf.readString(64);
		int changed = buf.readVarInt();
		int skipped = buf.readVarInt();
		return new PoolAccessResult(success, key, changed, skipped, readUuids(buf));
	}

	private static void writeUuids(PacketByteBuf buf, List<String> uuids) {
		buf.writeVarInt(uuids.size());
		for (String uuid : uuids) {
			buf.writeString(uuid == null ? "" : uuid, 64);
		}
	}

	private static List<String> readUuids(PacketByteBuf buf) {
		int size = Math.max(0, Math.min(buf.readVarInt(), MAX_BATCH));
		List<String> uuids = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			uuids.add(buf.readString(64));
		}
		return uuids;
	}
}
