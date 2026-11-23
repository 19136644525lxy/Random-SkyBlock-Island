package yifei.rsl.common.wand;

/**
 * Immutable payload used to open the area screen. The server computes all
 * values; the client only displays them and submits an action.
 */
public final class WandScreenData {
    public enum Mode {
        CREATE,
        EDIT,
        CONFIRM_EDIT
    }

    public final Mode mode;
    /** Existing area name for EDIT/CONFIRM_EDIT; null for CREATE. */
    public final String originalName;
    /** Name shown in the text field (editable by the client). */
    public final String proposedName;
    public final long volume;
    public final int x1;
    public final int y1;
    public final int z1;
    public final int x2;
    public final int y2;
    public final int z2;

    public WandScreenData(Mode mode, String originalName, String proposedName, long volume,
                          int x1, int y1, int z1, int x2, int y2, int z2) {
        this.mode = mode == null ? Mode.CREATE : mode;
        this.originalName = originalName;
        this.proposedName = proposedName == null ? "" : proposedName;
        this.volume = volume;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    public static WandScreenData create(String proposedName, long volume,
                                        int x1, int y1, int z1, int x2, int y2, int z2) {
        return new WandScreenData(Mode.CREATE, null, proposedName, volume,
                x1, y1, z1, x2, y2, z2);
    }

    public static WandScreenData edit(String name, long volume,
                                      int x1, int y1, int z1, int x2, int y2, int z2) {
        return new WandScreenData(Mode.EDIT, name, name, volume,
                x1, y1, z1, x2, y2, z2);
    }

    public static WandScreenData confirmEdit(String originalName, String proposedName, long volume,
                                             int x1, int y1, int z1, int x2, int y2, int z2) {
        return new WandScreenData(Mode.CONFIRM_EDIT, originalName, proposedName, volume,
                x1, y1, z1, x2, y2, z2);
    }

    public boolean canDelete() {
        return mode == Mode.EDIT && originalName != null;
    }

    public boolean isEditMode() {
        return mode == Mode.EDIT;
    }
}
