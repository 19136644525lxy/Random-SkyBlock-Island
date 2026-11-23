package yifei.rsl.common.wand;

/**
 * Client request from the area screen. Coordinates are never trusted from the
 * client; the server uses its own WandSelection for ADD and SAVE_EDIT.
 */
public final class WandAction {
    public enum Type {
        ADD,
        RENAME,
        SAVE_EDIT,
        DELETE,
        RESELECT,
        CANCEL
    }

    public final Type type;
    public final String name;
    public final String targetName;

    public WandAction(Type type, String name, String targetName) {
        this.type = type == null ? Type.CANCEL : type;
        this.name = name == null ? "" : name;
        this.targetName = targetName;
    }

    public static WandAction add(String name) {
        return new WandAction(Type.ADD, name, null);
    }

    public static WandAction rename(String targetName, String name) {
        return new WandAction(Type.RENAME, name, targetName);
    }

    public static WandAction saveEdit(String targetName, String name) {
        return new WandAction(Type.SAVE_EDIT, name, targetName);
    }

    public static WandAction delete(String targetName) {
        return new WandAction(Type.DELETE, "", targetName);
    }

    public static WandAction reselect(String targetName, String name) {
        return new WandAction(Type.RESELECT, name, targetName);
    }

    public static WandAction cancel() {
        return new WandAction(Type.CANCEL, "", null);
    }
}
