package yifei.rsl.common.wand;

/**
 * Server response for a wand action.
 */
public final class WandResult {
    public final boolean success;
    public final String messageKey;
    public final String detail;

    public WandResult(boolean success, String messageKey, String detail) {
        this.success = success;
        this.messageKey = messageKey;
        this.detail = detail;
    }
}
