package yifei.rsl.common.config;

import java.util.List;

/**
 * Outcome of a bulk grant/revoke from the block-pool access screen.
 */
public final class PoolAccessResult {
	public final boolean success;
	public final String messageKey;
	public final int changed;
	public final int skipped;
	public final List<String> failedNames;

	public PoolAccessResult(boolean success, String messageKey, int changed, int skipped,
			List<String> failedNames) {
		this.success = success;
		this.messageKey = messageKey;
		this.changed = changed;
		this.skipped = skipped;
		this.failedNames = List.copyOf(failedNames);
	}
}
