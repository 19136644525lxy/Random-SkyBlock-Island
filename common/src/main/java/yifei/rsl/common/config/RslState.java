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
	/**
	 * Last spawned egg island, remembered so the next transform can clear it even
	 * after a server restart. {@code eggIslandSize} is the cube edge length.
	 */
	public boolean eggIslandPresent;
	public String eggIslandDimension = "";
	public int eggIslandMinX;
	public int eggIslandMinY;
	public int eggIslandMinZ;
	public int eggIslandSize;

	public RslState() {
	}

	public RslState(boolean isRunning, int secondsRemaining) {
		this.isRunning = isRunning;
		this.secondsRemaining = Math.max(0, secondsRemaining);
	}

	public RslState copy() {
		RslState copy = new RslState(isRunning, secondsRemaining);
		copy.eggIslandPresent = eggIslandPresent;
		copy.eggIslandDimension = eggIslandDimension;
		copy.eggIslandMinX = eggIslandMinX;
		copy.eggIslandMinY = eggIslandMinY;
		copy.eggIslandMinZ = eggIslandMinZ;
		copy.eggIslandSize = eggIslandSize;
		return copy;
	}

	public void sanitize() {
		secondsRemaining = Math.max(0, secondsRemaining);
		if (!isRunning) {
			secondsRemaining = 0;
		}
		eggIslandDimension = eggIslandDimension == null ? "" : eggIslandDimension.trim();
		eggIslandSize = Math.max(0, Math.min(64, eggIslandSize));
		if (!eggIslandPresent || eggIslandSize <= 0 || eggIslandDimension.isEmpty()) {
			clearEggIsland();
		}
	}

	public void clearEggIsland() {
		eggIslandPresent = false;
		eggIslandDimension = "";
		eggIslandMinX = 0;
		eggIslandMinY = 0;
		eggIslandMinZ = 0;
		eggIslandSize = 0;
	}

	public void setEggIsland(String dimension, int minX, int minY, int minZ, int size) {
		eggIslandPresent = true;
		eggIslandDimension = dimension == null ? "" : dimension;
		eggIslandMinX = minX;
		eggIslandMinY = minY;
		eggIslandMinZ = minZ;
		eggIslandSize = Math.max(1, Math.min(64, size));
	}
}
