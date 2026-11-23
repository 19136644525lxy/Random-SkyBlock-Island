package yifei.rsl.common.config;

/**
 * Runtime state stored in {@code randomskyblockisland.state.json}.
 *
 * <p>Kept separate from settings so the countdown can be flushed frequently without
 * rewriting the (much larger) block list.</p>
 */
public final class RslState {
	public boolean isRunning;
	public int secondsRemaining;

	public RslState() {
	}

	public RslState(boolean isRunning, int secondsRemaining) {
		this.isRunning = isRunning;
		this.secondsRemaining = Math.max(0, secondsRemaining);
	}

	public RslState copy() {
		return new RslState(isRunning, secondsRemaining);
	}

	public void sanitize() {
		secondsRemaining = Math.max(0, secondsRemaining);
		if (!isRunning) {
			secondsRemaining = 0;
		}
	}
}
