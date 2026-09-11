package yifei.rsl.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块替换工具类
 *
 * 移植说明：
 * - ServerWorld → ServerLevel
 * - World → Level
 * - BlockState 保持不变
 * - BlockPos 保持不变
 * - setBlockState → setBlock（参数 flags 保持一致，3 = Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS）
 */
public class BlockReplacer {
    public static void fillArea(Level world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
        if (!(world instanceof ServerLevel)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) world;

        int startX = Math.min(minX, maxX);
        int startY = Math.min(minY, maxY);
        int startZ = Math.min(minZ, maxZ);
        int endX = Math.max(minX, maxX);
        int endY = Math.max(minY, maxY);
        int endZ = Math.max(minZ, maxZ);

        // 使用 MutableBlockPos 减少对象创建开销，避免内存压力
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    pos.set(x, y, z);
                    serverLevel.setBlock(pos, state, 3);
                }
            }
        }
    }
}
