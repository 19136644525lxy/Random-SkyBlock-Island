package yifei.rsl.common.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pure role/ACL logic for the block-pool editor. This deliberately does not look
 * at vanilla permission levels: being a level-4 operator is not enough to open
 * the editor unless this list grants access (the server console and the
 * single-player host are handled by the platform layer).
 */
public final class PoolAccess {
	private static final int MAX_NAME_LENGTH = 32;

	private PoolAccess() {
	}

	public static PoolAccessEntry find(List<PoolAccessEntry> entries, UUID uuid) {
		if (entries == null || uuid == null) {
			return null;
		}
		String wanted = uuid.toString();
		for (PoolAccessEntry entry : entries) {
			if (entry != null && wanted.equalsIgnoreCase(entry.uuid)) {
				return entry;
			}
		}
		return null;
	}

	public static boolean isOwner(List<PoolAccessEntry> entries, UUID uuid) {
		PoolAccessEntry entry = find(entries, uuid);
		return entry != null && entry.role == PoolAccessEntry.Role.OWNER;
	}

	public static boolean isEditor(List<PoolAccessEntry> entries, UUID uuid) {
		PoolAccessEntry entry = find(entries, uuid);
		return entry != null && entry.role == PoolAccessEntry.Role.EDITOR;
	}

	/**
	 * @return true for OWNER and EDITOR; this is the permission to open the editor
	 *         and to use the {@code blocks} commands
	 */
	public static boolean canUse(List<PoolAccessEntry> entries, UUID uuid) {
		return isOwner(entries, uuid) || isEditor(entries, uuid);
	}

	/**
	 * Adds the player or upgrades an editor to owner.
	 *
	 * @return true when the stored list changed; false for a no-op (already at the
	 *         requested role or invalid input)
	 */
	public static boolean grant(List<PoolAccessEntry> entries, UUID uuid, String name, boolean owner) {
		if (entries == null || uuid == null) {
			return false;
		}
		String normalizedName = normalizeName(name);
		PoolAccessEntry existing = find(entries, uuid);
		if (existing == null) {
			PoolAccessEntry entry = new PoolAccessEntry(uuid.toString(), normalizedName,
					owner ? PoolAccessEntry.Role.OWNER : PoolAccessEntry.Role.EDITOR);
			entries.add(entry);
			return true;
		}
		if (owner && existing.role != PoolAccessEntry.Role.OWNER) {
			existing.role = PoolAccessEntry.Role.OWNER;
			existing.name = normalizedName;
			return true;
		}
		return false;
	}

	public static boolean revoke(List<PoolAccessEntry> entries, UUID uuid) {
		if (entries == null || uuid == null) {
			return false;
		}
		String wanted = uuid.toString();
		return entries.removeIf(entry -> entry == null
				|| entry.uuid == null
				|| entry.uuid.equalsIgnoreCase(wanted));
	}

	/**
	 * Normalizes UUIDs/names, drops invalid entries and de-duplicates by UUID,
	 * keeping the highest role (OWNER beats EDITOR).
	 */
	public static void sanitize(List<PoolAccessEntry> entries) {
		if (entries == null) {
			return;
		}
		List<PoolAccessEntry> cleaned = new ArrayList<>(entries.size());
		Set<String> seen = new HashSet<>();
		for (PoolAccessEntry entry : entries) {
			if (entry == null || entry.uuid == null) {
				continue;
			}
			UUID uuid;
			try {
				uuid = UUID.fromString(entry.uuid.trim());
			} catch (IllegalArgumentException e) {
				continue;
			}
			if (entry.role == null) {
				continue;
			}
			String key = uuid.toString();
			String name = normalizeName(entry.name);
			PoolAccessEntry existing = null;
			for (PoolAccessEntry candidate : cleaned) {
				if (key.equals(candidate.uuid)) {
					existing = candidate;
					break;
				}
			}
			if (existing == null) {
				cleaned.add(new PoolAccessEntry(key, name, entry.role));
				seen.add(key);
			} else if (entry.role == PoolAccessEntry.Role.OWNER) {
				existing.role = PoolAccessEntry.Role.OWNER;
				if (!name.isEmpty()) {
					existing.name = name;
				}
			} else if (existing.name == null || existing.name.isEmpty()) {
				existing.name = name;
			}
		}
		entries.clear();
		entries.addAll(cleaned);
	}

	private static String normalizeName(String name) {
		if (name == null) {
			return "";
		}
		String value = name.trim();
		if (value.length() > MAX_NAME_LENGTH) {
			value = value.substring(0, MAX_NAME_LENGTH);
		}
		return value;
	}
}
