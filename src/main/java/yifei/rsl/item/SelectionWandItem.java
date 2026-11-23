package yifei.rsl.item;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
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
    public SelectionWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        int permissionLevel = ModConfig.getGamePermissionLevel();
        if (!player.hasPermissionLevel(permissionLevel)) {
            player.sendMessage(Text.translatable("rsl.message.wand_no_permission", permissionLevel), true);
            return ActionResult.FAIL;
        }
        if (ModConfig.isDimensionLocked() && !ModConfig.isPlayerInActiveDimension(player)) {
            player.sendMessage(Text.translatable("rsl.message.dimension_locked",
                    ModConfig.getActiveDimension()), true);
            return ActionResult.FAIL;
        }

        String dimension = player.getServerWorld().getRegistryKey().getValue().toString();
        BlockPos pos = context.getBlockPos();
        WandSelection selection = WandSelectionManager.getOrCreate(player);

        if (player.isSneaking()) {
            selection.clear();
            player.sendMessage(Text.translatable("rsl.message.wand_cleared"), true);
            return ActionResult.SUCCESS;
        }

        // Re-select flow: the player chose "reselect corners" in the edit screen.
        if (selection.isEditing()) {
            if (!selection.hasFirst() || !dimension.equals(selection.dimension())) {
                selection.setFirst(dimension, pos.getX(), pos.getY(), pos.getZ());
                player.sendMessage(Text.translatable("rsl.message.wand_pos1",
                        pos.getX(), pos.getY(), pos.getZ()), true);
                return ActionResult.SUCCESS;
            }
            if (!selection.hasSecond()) {
                if (!selection.setSecond(dimension, pos.getX(), pos.getY(), pos.getZ())) {
                    selection.setFirst(dimension, pos.getX(), pos.getY(), pos.getZ());
                    player.sendMessage(Text.translatable("rsl.message.wand_pos1",
                            pos.getX(), pos.getY(), pos.getZ()), true);
                    return ActionResult.SUCCESS;
                }
                long volume = selection.volumeSaturated();
                if (volume > ModConfig.getMaxAreaVolume()) {
                    player.sendMessage(Text.translatable("rsl.message.wand_volume_too_large",
                            volume, ModConfig.getMaxAreaVolume()), true);
                } else {
                    player.sendMessage(Text.translatable("rsl.message.wand_pos2",
                            pos.getX(), pos.getY(), pos.getZ(), volume), true);
                }
                return ActionResult.SUCCESS;
            }
            AreaSpec edited = selection.toArea(selection.editTarget());
            if (edited == null || !edited.hasValidName() || !ModConfig.isValidArea(edited)) {
                player.sendMessage(Text.translatable("rsl.message.wand_invalid_selection",
                        ModConfig.getMaxAreaVolume()), true);
                return ActionResult.SUCCESS;
            }
            WandNetwork.sendOpen(player, WandScreenData.confirmEdit(
                    selection.editTarget(), selection.editTarget(), edited.volumeSaturated(),
                    edited.minX, edited.minY, edited.minZ, edited.maxX, edited.maxY, edited.maxZ));
            return ActionResult.SUCCESS;
        }

        if (!selection.hasFirst() || !dimension.equals(selection.dimension())) {
            selection.setFirst(dimension, pos.getX(), pos.getY(), pos.getZ());
            player.sendMessage(Text.translatable("rsl.message.wand_pos1",
                    pos.getX(), pos.getY(), pos.getZ()), true);
            return ActionResult.SUCCESS;
        }

        if (!selection.hasSecond()) {
            if (!selection.setSecond(dimension, pos.getX(), pos.getY(), pos.getZ())) {
                selection.setFirst(dimension, pos.getX(), pos.getY(), pos.getZ());
                player.sendMessage(Text.translatable("rsl.message.wand_pos1",
                        pos.getX(), pos.getY(), pos.getZ()), true);
                return ActionResult.SUCCESS;
            }
            long volume = selection.volumeSaturated();
            if (volume > ModConfig.getMaxAreaVolume()) {
                player.sendMessage(Text.translatable("rsl.message.wand_volume_too_large",
                        volume, ModConfig.getMaxAreaVolume()), true);
            } else {
                player.sendMessage(Text.translatable("rsl.message.wand_pos2",
                        pos.getX(), pos.getY(), pos.getZ(), volume), true);
            }
            return ActionResult.SUCCESS;
        }

        AreaSpec target = findTarget(pos);
        if (target != null) {
            WandNetwork.sendOpen(player, WandScreenData.edit(target.name, target.volumeSaturated(),
                    target.minX, target.minY, target.minZ, target.maxX, target.maxY, target.maxZ));
            return ActionResult.SUCCESS;
        }

        int number = ModConfig.getSaveAreas().size() + 1;
        String defaultName = "area_" + number;
        while (ModConfig.getAreaByName(defaultName) != null) {
            defaultName = "area_" + (++number);
        }
        WandNetwork.sendOpen(player, WandScreenData.create(defaultName, selection.volumeSaturated(),
                selection.x1(), selection.y1(), selection.z1(),
                selection.x2(), selection.y2(), selection.z2()));
        return ActionResult.SUCCESS;
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
