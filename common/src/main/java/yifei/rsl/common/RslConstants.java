package yifei.rsl.common;

/**
 * Shared, loader-independent constants.
 *
 * <p>This class must not reference any {@code net.minecraft} type: it is compiled
 * into both the Fabric and the Forge jar.</p>
 */
public final class RslConstants {
	public static final String MOD_ID = "rsl";

	public static final String SETTINGS_FILE = "randomskyblockisland.json";
	public static final String STATE_FILE = "randomskyblockisland.state.json";
	public static final String AREAS_FILE = "rsl_areas.json";
	public static final String BACKUP_FILE = "randomskyblockisland.v1.bak.json";

	public static final int CONFIG_VERSION = 2;

	private RslConstants() {
	}
}
