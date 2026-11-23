package yifei.rsl.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import yifei.rsl.common.config.PoolAccessPlayers;
import yifei.rsl.common.config.PoolAccessResult;

import java.util.List;
import java.util.function.Consumer;

/**
 * Client-only access-management channel. Kept out of AccessNetwork so dedicated
 * servers never load client classes.
 */
public final class AccessClientNetwork {
	private static Consumer<PoolAccessPlayers> playersHandler = payload -> {
	};
	private static Consumer<PoolAccessResult> resultHandler = result -> {
	};

	private AccessClientNetwork() {
	}

	public static void registerClient() {
		ClientPlayNetworking.registerGlobalReceiver(AccessNetwork.PLAYERS,
				(client, handler, buf, sender) -> {
					PoolAccessPlayers payload = AccessNetwork.readPlayers(buf);
					client.execute(() -> playersHandler.accept(payload));
				});
		ClientPlayNetworking.registerGlobalReceiver(AccessNetwork.RESULT,
				(client, handler, buf, sender) -> {
					PoolAccessResult result = AccessNetwork.readResult(buf);
					client.execute(() -> resultHandler.accept(result));
				});
	}

	public static void setHandlers(Consumer<PoolAccessPlayers> players,
			Consumer<PoolAccessResult> result) {
		playersHandler = players == null ? payload -> {
		} : players;
		resultHandler = result == null ? resultValue -> {
		} : result;
	}

	public static void requestPlayers() {
		ClientPlayNetworking.send(AccessNetwork.REQUEST, PacketByteBufs.create());
	}

	public static void sendAction(String action, List<String> uuids) {
		PacketByteBuf buf = PacketByteBufs.create();
		AccessNetwork.writeAction(buf, action, uuids);
		ClientPlayNetworking.send(AccessNetwork.ACTION, buf);
	}
}
