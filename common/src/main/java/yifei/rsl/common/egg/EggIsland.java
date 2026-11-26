package yifei.rsl.common.egg;

import yifei.rsl.common.config.AreaSpec;

import java.util.List;

/**
 * Pure geometry helpers for the hidden "egg" island: the cube edge derived from a
 * configured volume, and the overlap/distance test against saved areas. World
 * access stays in the platform layer.
 */
public final class EggIsland {
    public static final int MAX_EDGE = 64;

    private EggIsland() {
    }

    /**
     * Smallest cube edge whose volume reaches {@code volume}; e.g. 27 -> 3,
     * 28 -> 4. Values below 1 yield 1.
     */
    public static int edgeLength(int volume) {
        if (volume <= 1) {
            return 1;
        }
        int edge = 1;
        while ((long) edge * edge * edge < volume && edge < MAX_EDGE) {
            edge++;
        }
        return edge;
    }

    public static long cubeVolume(int edge) {
        long clamped = Math.max(1, Math.min(MAX_EDGE, edge));
        return clamped * clamped * clamped;
    }

    /**
     * True when the block box keeps fewer than {@code minDistance} empty blocks
     * between itself and the area on every axis. Distance 0 still rejects only
     * actual overlaps (the +1 accounts for block boxes being inclusive).
     */
    public static boolean tooClose(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                   AreaSpec area, int minDistance) {
        if (area == null) {
            return false;
        }
        int distance = Math.max(0, minDistance);
        if (distance == 0) {
            return maxX >= area.minX
                    && area.maxX >= minX
                    && maxY >= area.minY
                    && area.maxY >= minY
                    && maxZ >= area.minZ
                    && area.maxZ >= minZ;
        }
        return maxX + distance + 1 >= area.minX
                && area.maxX + distance + 1 >= minX
                && maxY + distance + 1 >= area.minY
                && area.maxY + distance + 1 >= minY
                && maxZ + distance + 1 >= area.minZ
                && area.maxZ + distance + 1 >= minZ;
    }

    public static boolean anyAreaTooClose(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                          List<AreaSpec> areas, int minDistance) {
        if (areas == null) {
            return false;
        }
        for (AreaSpec area : areas) {
            if (tooClose(minX, minY, minZ, maxX, maxY, maxZ, area, minDistance)) {
                return true;
            }
        }
        return false;
    }

    /** Inclusive cube fits strictly inside the world height range. */
    public static boolean insideWorldHeight(int minY, int edge, int worldBottom, int worldTop) {
        return minY > worldBottom && minY + edge - 1 < worldTop;
    }
}
