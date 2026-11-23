package yifei.rsl.common.config;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validation and normalization for dimension ids such as
 * {@code minecraft:the_nether} or {@code examplemod:custom_dimension}.
 *
 * <p>This class deliberately has no Minecraft imports so it can be shared by the
 * Fabric and Forge builds and unit-tested without a game classpath.</p>
 */
public final class DimensionId {
	private static final int MAX_LENGTH = 128;
	private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
	private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

	private DimensionId() {
	}

	/**
	 * Normalizes a dimension id, adding the {@code minecraft:} namespace when it is
	 * omitted. Returns {@code null} when the value is empty or syntactically invalid.
	 */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String value = raw.trim().toLowerCase(Locale.ROOT);
		if (value.isEmpty()) {
			return null;
		}
		int colon = value.indexOf(':');
		if (colon < 0) {
			value = "minecraft:" + value;
		} else if (value.indexOf(':', colon + 1) >= 0) {
			return null;
		}
		if (value.length() > MAX_LENGTH) {
			return null;
		}
		int split = value.indexOf(':');
		String namespace = value.substring(0, split);
		String path = value.substring(split + 1);
		if (namespace.isEmpty() || path.isEmpty()) {
			return null;
		}
		if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
			return null;
		}
		return value;
	}

	public static boolean isValid(String raw) {
		return normalize(raw) != null;
	}

	/**
	 * Translation key for a dimension display name. Unknown ids fall back to a key
	 * that callers can translate to a generic "unknown dimension" label.
	 */
	public static String translationKey(String raw) {
		String id = normalize(raw);
		if (id == null) {
			return "dimension.unknown";
		}
		int split = id.indexOf(':');
		return "dimension." + id.substring(0, split) + "." + id.substring(split + 1);
	}
}
