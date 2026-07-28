package yifei.rsl.game;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockReplacer {
	public static void fillArea(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		if (!(world instanceof ServerWorld)) {
			return;
		}
		
		ServerWorld serverWorld = (ServerWorld) world;
		
		int startX = Math.min(minX, maxX);
		int startY = Math.min(minY, maxY);
		int startZ = Math.min(minZ, maxZ);
		int endX = Math.max(minX, maxX);
		int endY = Math.max(minY, maxY);
		int endZ = Math.max(minZ, maxZ);
		
		for (int x = startX; x <= endX; x++) {
			for (int y = startY; y <= endY; y++) {
				for (int z = startZ; z <= endZ; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					serverWorld.setBlockState(pos, state, 3);
				}
			}
		}
	}
}
