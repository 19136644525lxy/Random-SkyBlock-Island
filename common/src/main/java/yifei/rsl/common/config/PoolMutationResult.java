package yifei.rsl.common.config;

import java.util.List;

/**
 * Result of an atomic block-pool mutation. On failure the pool is unchanged and
 * {@link #failedIds} lists the rejected raw ids.
 */
public final class PoolMutationResult {
    public final boolean success;
    public final BlockMutationResult code;
    public final String messageKey;
    public final List<String> failedIds;

    public PoolMutationResult(boolean success, BlockMutationResult code, String messageKey, List<String> failedIds) {
        this.success = success;
        this.code = code;
        this.messageKey = messageKey;
        this.failedIds = failedIds == null ? List.of() : List.copyOf(failedIds);
    }

    public static PoolMutationResult ok() {
        return new PoolMutationResult(true, BlockMutationResult.OK, "", List.of());
    }

    public static PoolMutationResult fail(BlockMutationResult code, String messageKey, List<String> failedIds) {
        return new PoolMutationResult(false, code, messageKey, failedIds);
    }
}
