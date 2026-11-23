package yifei.rsl.common.wand;

import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.common.config.DimensionId;

/**
 * Platform-neutral selection state for the selection wand.
 *
 * <p>The server keeps one instance per player. The selection must stay inside a
 * single dimension; setting a first point in another dimension resets it.</p>
 */
public final class WandSelection {
    private String dimension;
    private boolean hasFirst;
    private boolean hasSecond;
    private int x1;
    private int y1;
    private int z1;
    private int x2;
    private int y2;
    private int z2;
    private String editTarget;

    public void setFirst(String dimension, int x, int y, int z) {
        this.dimension = DimensionId.normalize(dimension);
        this.x1 = x;
        this.y1 = y;
        this.z1 = z;
        this.x2 = 0;
        this.y2 = 0;
        this.z2 = 0;
        this.hasFirst = this.dimension != null;
        this.hasSecond = false;
    }

    /**
     * @return true when the second point was accepted; false for a missing first
     *         point or a point from another dimension.
     */
    public boolean setSecond(String dimension, int x, int y, int z) {
        String normalized = DimensionId.normalize(dimension);
        if (!hasFirst || normalized == null || !normalized.equals(this.dimension)) {
            return false;
        }
        this.x2 = x;
        this.y2 = y;
        this.z2 = z;
        this.hasSecond = true;
        return true;
    }

    public boolean hasFirst() {
        return hasFirst;
    }

    public boolean hasSecond() {
        return hasSecond;
    }

    public boolean isReady() {
        return hasFirst && hasSecond;
    }

    public String dimension() {
        return dimension;
    }

    public int x1() {
        return x1;
    }

    public int y1() {
        return y1;
    }

    public int z1() {
        return z1;
    }

    public int x2() {
        return x2;
    }

    public int y2() {
        return y2;
    }

    public int z2() {
        return z2;
    }

    /**
     * Builds a normalized area from the two selected points. The caller supplies
     * the name because naming happens in the confirmation screen.
     */
    public AreaSpec toArea(String name) {
        if (!isReady()) {
            return null;
        }
        AreaSpec area = new AreaSpec(name, x1, y1, z1, x2, y2, z2);
        area.normalizeInPlace();
        return area;
    }

    public long volumeSaturated() {
        AreaSpec area = toArea("wand");
        return area == null ? 0L : area.volumeSaturated();
    }

    /**
     * Enters the "re-select two corners for an existing area" mode. The next
     * clicks set a fresh selection instead of opening another area for editing.
     */
    public void beginEdit(String targetName) {
        clear();
        editTarget = targetName == null ? null : targetName.trim();
        if (editTarget != null && editTarget.isEmpty()) {
            editTarget = null;
        }
    }

    public boolean isEditing() {
        return editTarget != null;
    }

    public String editTarget() {
        return editTarget;
    }

    public void clear() {
        dimension = null;
        hasFirst = false;
        hasSecond = false;
        x1 = y1 = z1 = x2 = y2 = z2 = 0;
        editTarget = null;
    }
}
