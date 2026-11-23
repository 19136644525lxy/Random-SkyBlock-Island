package yifei.rsl.common.config;

import java.util.List;

/**
 * Server to client payload for the block-pool access screen: whether the sender
 * may manage the ACL and the online players with their current roles.
 */
public final class PoolAccessPlayers {
	public final boolean canManage;
	public final List<PoolAccessPlayer> players;

	public PoolAccessPlayers(boolean canManage, List<PoolAccessPlayer> players) {
		this.canManage = canManage;
		this.players = List.copyOf(players);
	}
}
