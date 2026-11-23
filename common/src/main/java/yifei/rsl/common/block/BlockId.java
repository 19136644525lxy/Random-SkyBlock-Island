package yifei.rsl.common.block;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resource-location helpers that do not depend on Minecraft's {@code Identifier}.
 */
public final class BlockId {
	private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
	private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");
	private static final int MAX_LENGTH = 128;

	private BlockId() {
	}

	/**
	 * Normalizes a user supplied block id.
	 *
	 * <ul>
	 *   <li>trims surrounding whitespace and lowercases;</li>
	 *   <li>adds the {@code minecraft:} namespace when omitted;</li>
	 *   <li>rejects empty, malformed or over-long ids.</li>
	 * </ul>
	 *
	 * @return the normalized id, or {@code null} when it is not a legal resource location
	 */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String value = raw.trim().toLowerCase(Locale.ROOT);
		if (value.isEmpty()) {
			return null;
		}

		String namespace = "minecraft";
		String path = value;
		int colon = value.indexOf(':');
		if (colon >= 0) {
			if (value.indexOf(':', colon + 1) >= 0) {
				return null;
			}
			namespace = value.substring(0, colon);
			path = value.substring(colon + 1);
		}

		if (namespace.isEmpty() || path.isEmpty()) {
			return null;
		}
		if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
			return null;
		}

		String full = namespace + ":" + path;
		return full.length() <= MAX_LENGTH ? full : null;
	}

	public static boolean isValidSyntax(String raw) {
		return normalize(raw) != null;
	}

	public static String path(String normalizedId) {
		if (normalizedId == null) {
			return "";
		}
		int colon = normalizedId.indexOf(':');
		return colon < 0 ? normalizedId : normalizedId.substring(colon + 1);
	}

	public static String namespace(String normalizedId) {
		if (normalizedId == null) {
			return "";
		}
		int colon = normalizedId.indexOf(':');
		return colon < 0 ? "minecraft" : normalizedId.substring(0, colon);
	}
}
