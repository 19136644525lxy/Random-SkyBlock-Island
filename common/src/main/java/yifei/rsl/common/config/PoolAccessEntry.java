package yifei.rsl.common.config;

/**
 * One entry of the block-pool access list. The UUID is authoritative; the name is
 * only kept for display and may become stale after a rename.
 */
public final class PoolAccessEntry {
	public String uuid;
	public String name;
	public Role role;

	public PoolAccessEntry() {
	}

	public PoolAccessEntry(String uuid, String name, Role role) {
		this.uuid = uuid;
		this.name = name;
		this.role = role;
	}

	public PoolAccessEntry copy() {
		return new PoolAccessEntry(uuid, name, role);
	}

	public enum Role {
		OWNER,
		EDITOR
	}
}
