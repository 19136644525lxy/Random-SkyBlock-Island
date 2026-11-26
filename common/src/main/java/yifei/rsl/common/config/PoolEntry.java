package yifei.rsl.common.config;

import yifei.rsl.common.block.BlockId;

/**
 * One entry of the "container" part of the normal block pool: a block state
 * (encoded as text, including orientation/properties) plus an optional
 * Base64-compressed block-entity NBT payload captured from a chest/barrel/etc.
 *
 * <p>Pure data so it can live in the shared source set (no net.minecraft types).</p>
 */
public final class PoolEntry {
	/** Upper bound for the Base64 NBT string (about 48 KB of raw data). */
	public static final int MAX_NBT_LENGTH = 64 * 1024;

	public String id = "";
	public String state = "";
	public String nbt = "";

	public PoolEntry() {
	}

	public PoolEntry(String id, String state, String nbt) {
		this.id = id == null ? "" : id;
		this.state = state == null ? "" : state;
		this.nbt = nbt == null ? "" : nbt;
	}

	public PoolEntry copy() {
		return new PoolEntry(id, state, nbt);
	}

	public boolean hasNbt() {
		return nbt != null && !nbt.isEmpty();
	}

	/** Stable identity used for de-duplication and UI selection. */
	public String key() {
		return (id == null ? "" : id) + "|" + (state == null ? "" : state) + "|" + (nbt == null ? "" : nbt);
	}

	public String normalizedId() {
		return BlockId.normalize(id);
	}

	public static boolean isValidBase64(String value) {
		if (value == null || value.isEmpty() || value.length() > MAX_NBT_LENGTH) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
					|| (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=';
			if (!ok) {
				return false;
			}
		}
		return true;
	}
}
