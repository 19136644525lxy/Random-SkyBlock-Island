package yifei.rsl.common.config;

import java.util.List;

/**
 * Full block-pool state sent to the client when the pool screen opens or the
 * pool changes.
 */
public final class BlockPoolSnapshot {
    public final List<String> allBlockIds;
    public final List<String> poolIds;
    public final List<String> invalidIds;
    public final int maxPool;
    public final int permissionLevel;
    public final boolean canEdit;

    public BlockPoolSnapshot(List<String> allBlockIds, List<String> poolIds, List<String> invalidIds,
                             int maxPool, int permissionLevel, boolean canEdit) {
        this.allBlockIds = List.copyOf(allBlockIds);
        this.poolIds = List.copyOf(poolIds);
        this.invalidIds = List.copyOf(invalidIds);
        this.maxPool = maxPool;
        this.permissionLevel = permissionLevel;
        this.canEdit = canEdit;
    }
}
