package yifei.rsl.common.config;

/**
 * One online player row for the block-pool access screen. The role is
 * {@code "OWNER"}, {@code "EDITOR"} or empty when the player has no access.
 */
public final class PoolAccessPlayer {
	public final String uuid;
	public final String name;
	public final String role;

	public PoolAccessPlayer(String uuid, String name, String role) {
		this.uuid = uuid;
		this.name = name;
		this.role = role == null ? "" : role;
	}
}
