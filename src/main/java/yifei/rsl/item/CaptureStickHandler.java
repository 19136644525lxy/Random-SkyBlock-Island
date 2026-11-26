package yifei.rsl.item;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import yifei.rsl.common.config.PoolEntry;
import yifei.rsl.game.BlockStateCodec;
import yifei.rsl.game.NbtPayload;
import yifei.rsl.network.CaptureStickNetwork;

/**
 * 服务端抓取逻辑：手持收纳木棍、潜行右键一个「有内容」的容器方块（箱子/木桶等
 * 实现了 {@link Inventory} 的方块实体），先请客户端修改容器名字，确认后再把
 * 方块状态 + 方块实体 NBT 加入正常池。
 */
public final class CaptureStickHandler {
	private CaptureStickHandler() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register(CaptureStickHandler::onUseBlock);
	}

	private static ActionResult onUseBlock(PlayerEntity player, net.minecraft.world.World world, Hand hand,
										   BlockHitResult hitResult) {
		if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}
		if (!player.isSneaking()) {
			return ActionResult.PASS;
		}
		ItemStack stack = player.getStackInHand(hand);
		if (!(stack.getItem() instanceof CaptureStickItem)) {
			return ActionResult.PASS;
		}
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.PASS;
		}
		BlockPos pos = hitResult.getBlockPos();
		BlockEntity blockEntity = serverWorld.getBlockEntity(pos);
		if (!(blockEntity instanceof Inventory inventory)) {
			serverPlayer.sendMessage(Text.translatable("rsl.message.capture_not_container"), false);
			yifei.rsl.game.SoundFx.play(serverPlayer, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
			return ActionResult.SUCCESS;
		}
		boolean hasItems = false;
		for (int slot = 0; slot < inventory.size(); slot++) {
			if (!inventory.getStack(slot).isEmpty()) {
				hasItems = true;
				break;
			}
		}
		if (!hasItems) {
			serverPlayer.sendMessage(Text.translatable("rsl.message.capture_empty"), false);
			yifei.rsl.game.SoundFx.play(serverPlayer, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
			return ActionResult.SUCCESS;
		}
		BlockState blockState = serverWorld.getBlockState(pos);
		NbtCompound nbt = blockEntity.createNbt();
		String payload = NbtPayload.encode(nbt);
		if (payload.isEmpty()) {
			serverPlayer.sendMessage(Text.translatable("rsl.message.capture_failed"), false);
			yifei.rsl.game.SoundFx.play(serverPlayer, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
			return ActionResult.SUCCESS;
		}
		if (payload.length() > PoolEntry.MAX_NBT_LENGTH) {
			serverPlayer.sendMessage(Text.translatable("rsl.message.capture_too_large"), false);
			yifei.rsl.game.SoundFx.play(serverPlayer, net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, 0.6F, 1.0F);
			return ActionResult.SUCCESS;
		}
		String id = Registries.BLOCK.getId(blockState.getBlock()).toString();
		String state = BlockStateCodec.encode(blockState);
		CaptureStickNetwork.beginCapture(serverPlayer, id, state, payload, CaptureStickNetwork.displayName(nbt));
		return ActionResult.SUCCESS;
	}
}
