package yifei.rsl.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import yifei.rsl.common.config.AreaSpec;
import yifei.rsl.common.wand.WandScreenData;
import yifei.rsl.common.wand.WandSelection;
import yifei.rsl.config.ModConfig;
import yifei.rsl.network.WandNetwork;

/**
 * Two right-clicks select an area; the third click opens the confirmation
 * screen. Right-clicking an existing named area opens its edit screen instead.
 */
public class SelectionWandItem extends Item {
    public SelectionWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        int permissionLevel = ModConfig.getGamePermissionLevel();
        if (!player.hasPermissions(permissionLevel)) {
            player.displayClientMessage(Component.translatable("rsl.message.wand_no_permission", permissionLevel), true);
            return InteractionResult.FAIL;
        }
        if (ModConfig.isDimensionLocked() && !ModConfig.isPlayerInActiveDimension(player)) {
            player.displayClientMessage(Component.translatable("rsl.message.dimension_locked",
                    ModConfig.getActiveDimension()), true);
            return InteractionResult.FAIL;
        }

        String dimension = player.serverLevel().dimension().location().toString();
        BlockPos pos = context.getClickedPos();
        WandSelection selection = WandSelectionManager.getOrCreate(player);

        if (player.isShiftKeyDown()) {
            selection.clear();
            player.displayClientMessage(Component.translatable("rsl.message.wand_cleared"), true);
            return InteractionResult.SUCCESS;
        }

        // Re-select flow: the player chose "reselect corners" in the edit screen.
        if (selection.isEditing()) {
            if (!selection.hasFirst() || !dimension.equals(selection.dimension())) {
                selection.setFirst(dimension, pos.getX(), pos.getY(), pos.getZ());
                player.displayClientMessage(Component.translatable("rsl.message.wand_pos1",
                        pos.getX(), pos.getY(), pos.getZ()), true);
                return InteractionResult.SUCCESS;
            }
            if (!selection.hasSecond()) {
                if (!selection.setSecond(dimension, pos.getX(), pos.getY(), pos.getZ())) {
                    selection.setFirst(dimension, pos.getX(), pos.getY(), pos.getZ());
                    player.displayClientMessage(Component.translatable("rsl.message.wand_pos1",
                            pos.getX(), pos.getY(), pos.getZ()), true);
                    return InteractionResult.SUCCESS;
                }
                long volume = selection.volumeSaturated();
                if (volume > ModConfig.getMaxAreaVolume()) {
                    player.displayClientMessage(Component.translatable("rsl.message.wand_volume_too_large",
                            volume, ModConfig.getMaxAreaVolume()), true);
                } else {
                    player.displayClientMessage(Component.translatable("rsl.message.wand_pos2",
                            pos.getX(), pos.getY(), pos.getZ(), volume), true);
                }
                return InteractionResult.SUCCESS;
            }
            AreaSpec edited = selection.toArea(selection.editTarget());
            if (edited == null || !edited.hasValidName() || !ModConfig.isValidArea(edited)) {
                player.displayClientMessage(Component.translatable("rsl.message.wand_invalid_selection",
                        ModConfig.getMaxAreaVolume()), true);
                return InteractionResult.SUCCESS;
            }
            WandNetwork.sendOpen(player, WandScreenData.confirmEdit(
                    selection.editTarget(), selection.editTarget(), edited.volumeSaturated(),
                    edited.minX, edited.minY, edited.minZ, edited.maxX, edited.maxY, edited.maxZ));
            return InteractionResult.SUCCESS;
        }

        if (!selection.hasFirst() || !dimension.equals(selection.dimension())) {
            selection.setFirst(dimension, pos.getX(), pos.getY(), pos.getZ());
            player.displayClientMessage(Component.translatable("rsl.message.wand_pos1",
                    pos.getX(), pos.getY(), pos.getZ()), true);
            return InteractionResult.SUCCESS;
        }

        if (!selection.hasSecond()) {
            if (!selection.setSecond(dimension, pos.getX(), pos.getY(), pos.getZ())) {
                selection.setFirst(dimension, pos.getX(), pos.getY(), pos.getZ());
                player.displayClientMessage(Component.translatable("rsl.message.wand_pos1",
                        pos.getX(), pos.getY(), pos.getZ()), true);
                return InteractionResult.SUCCESS;
            }
            long volume = selection.volumeSaturated();
            if (volume > ModConfig.getMaxAreaVolume()) {
                player.displayClientMessage(Component.translatable("rsl.message.wand_volume_too_large",
                        volume, ModConfig.getMaxAreaVolume()), true);
            } else {
                player.displayClientMessage(Component.translatable("rsl.message.wand_pos2",
                        pos.getX(), pos.getY(), pos.getZ(), volume), true);
            }
            return InteractionResult.SUCCESS;
        }

        AreaSpec target = findTarget(pos);
        if (target != null) {
            WandNetwork.sendOpen(player, WandScreenData.edit(target.name, target.volumeSaturated(),
                    target.minX, target.minY, target.minZ, target.maxX, target.maxY, target.maxZ));
            return InteractionResult.SUCCESS;
        }

        int number = ModConfig.getSaveAreas().size() + 1;
        String defaultName = "area_" + number;
        while (ModConfig.getAreaByName(defaultName) != null) {
            defaultName = "area_" + (++number);
        }
        WandNetwork.sendOpen(player, WandScreenData.create(defaultName, selection.volumeSaturated(),
                selection.x1(), selection.y1(), selection.z1(),
                selection.x2(), selection.y2(), selection.z2()));
        return InteractionResult.SUCCESS;
    }

    private AreaSpec findTarget(BlockPos pos) {
        AreaSpec target = null;
        for (AreaSpec area : ModConfig.getSaveAreas()) {
            if (area.contains(pos.getX(), pos.getY(), pos.getZ())
                    && (target == null || area.volumeSaturated() < target.volumeSaturated())) {
                target = area;
            }
        }
        return target;
    }
}
