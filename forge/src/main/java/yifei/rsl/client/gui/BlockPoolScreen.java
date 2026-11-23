package yifei.rsl.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import yifei.rsl.client.BlockPoolClientNetwork;
import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.common.ui.PoolLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Searchable block-pool editor. Icons are rendered with the vanilla item
 * renderer, so no JEI/REI dependency is needed and every registered mod block is
 * shown.
 */
public class BlockPoolScreen extends Screen {
    private final Set<String> selected = new LinkedHashSet<>();
    private final List<String> filtered = new ArrayList<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private BlockPoolSnapshot snapshot;
    private PoolLayout layout;
    private EditBox searchField;
    private Button applyButton;
    private Button selectAllButton;
    private Button clearButton;
    private int page;
    private String query = "";
    private String statusMessage = "";
    private int statusColor = 0xFFAA55;
    private long statusUntil;
    private boolean pending;

    public BlockPoolScreen() {
        super(Component.translatable("rsl.pool.title"));
        BlockPoolClientNetwork.setHandlers(this::onSnapshot, this::onResult);
    }

    @Override
    protected void init() {
        layout = PoolLayout.compute(this.width, this.height);

        searchField = new EditBox(this.font, layout.searchX, PoolLayout.SEARCH_Y,
                layout.searchWidth, PoolLayout.SEARCH_HEIGHT,
                Component.translatable("rsl.pool.search"));
        searchField.setMaxLength(128);
        searchField.setHint(Component.translatable("rsl.pool.search"));
        searchField.setValue(query);
        searchField.setResponder(value -> {
            query = value;
            page = 0;
            rebuildFiltered();
        });
        addRenderableWidget(searchField);

        addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(layout.pagePrevX, layout.pageY, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(layout.pageNextX, layout.pageY, 20, 20).build());

        int gap = 6;
        selectAllButton = addRenderableWidget(Button.builder(Component.translatable("rsl.pool.select_all"), b -> selectAllFiltered())
                .bounds(layout.actionStartX, layout.actionY, layout.actionButtonWidth, 20).build());
        clearButton = addRenderableWidget(Button.builder(Component.translatable("rsl.pool.clear"), b -> selected.clear())
                .bounds(layout.actionStartX + layout.actionButtonWidth + gap, layout.actionY,
                        layout.actionButtonWidth, 20).build());
        applyButton = addRenderableWidget(Button.builder(Component.translatable("rsl.pool.apply"), b -> apply())
                .bounds(layout.actionStartX + (layout.actionButtonWidth + gap) * 2, layout.actionY,
                        layout.actionButtonWidth, 20).build());

        page = clampPage(page);
        updateControls();
        BlockPoolClientNetwork.requestSnapshot();
    }

    private void onSnapshot(BlockPoolSnapshot snapshot) {
        this.snapshot = snapshot;
        this.selected.retainAll(snapshot.allBlockIds);
        this.displayNames.clear();
        for (String id : snapshot.allBlockIds) {
            this.displayNames.put(id, nameFor(id));
        }
        if (!snapshot.canEdit) {
            setStatus(Component.translatable("rsl.pool.readonly").getString(), 0xFFAA55);
        }
        if (snapshot.poolIds.isEmpty()) {
            setStatus(Component.translatable("rsl.pool.empty").getString(), 0xFFAA55);
        }
        rebuildFiltered();
        updateControls();
    }

    private void updateControls() {
        boolean canEdit = snapshot != null && snapshot.canEdit;
        if (applyButton != null) {
            applyButton.active = canEdit && !pending;
        }
        if (selectAllButton != null) {
            selectAllButton.active = canEdit;
        }
        if (clearButton != null) {
            clearButton.active = canEdit;
        }
    }

    private String nameFor(String id) {
        ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier == null || !ForgeRegistries.BLOCKS.containsKey(identifier)) {
            return id;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(identifier);
        return block == null ? id : block.getName().getString();
    }

    private void rebuildFiltered() {
        filtered.clear();
        if (snapshot == null) {
            return;
        }
        String search = query.trim().toLowerCase(Locale.ROOT);
        for (String id : snapshot.allBlockIds) {
            if (search.isEmpty()) {
                filtered.add(id);
                continue;
            }
            String name = displayNames.getOrDefault(id, id).toLowerCase(Locale.ROOT);
            if (id.toLowerCase(Locale.ROOT).contains(search) || name.contains(search)) {
                filtered.add(id);
            }
        }
        page = clampPage(page);
    }

    private int pageCount() {
        return Math.max(1, (filtered.size() + layout.pageSize - 1) / layout.pageSize);
    }

    private int clampPage(int value) {
        return Math.max(0, Math.min(value, pageCount() - 1));
    }

    private void changePage(int delta) {
        int next = clampPage(page + delta);
        if (next != page) {
            page = next;
        }
    }

    private void selectAllFiltered() {
        if (snapshot != null && snapshot.canEdit) {
            selected.addAll(filtered);
        }
    }

    private void apply() {
        if (snapshot == null || !snapshot.canEdit || pending) {
            return;
        }
        List<String> add = new ArrayList<>();
        List<String> remove = new ArrayList<>();
        for (String id : selected) {
            if (snapshot.poolIds.contains(id)) {
                remove.add(id);
            } else {
                add.add(id);
            }
        }
        if (add.isEmpty() && remove.isEmpty()) {
            setStatus(Component.translatable("rsl.pool.no_changes").getString(), 0xFFAA55);
            return;
        }
        pending = true;
        updateControls();
        setStatus(Component.translatable("rsl.pool.pending", add.size(), remove.size()).getString(), 0xFFAA55);
        BlockPoolClientNetwork.sendMutation(add, remove);
    }

    private void onResult(PoolMutationResult result) {
        pending = false;
        if (result.success) {
            selected.clear();
            setStatus(Component.translatable("rsl.pool.saved").getString(), 0x55FF55);
        } else {
            String base = Component.translatable(result.messageKey).getString();
            if (!result.failedIds.isEmpty()) {
                base += " " + String.join(", ", result.failedIds);
            }
            setStatus(base, 0xFF5555);
        }
        updateControls();
    }

    private void setStatus(String message, int color) {
        statusMessage = message == null ? "" : message;
        statusColor = color;
        statusUntil = System.currentTimeMillis() + 5000L;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(4, 4, this.width - 4, this.height - 4, 0x55101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, PoolLayout.TITLE_Y, 0xFFFFFF);

        if (snapshot != null) {
            Component count = Component.translatable("rsl.pool.count", filtered.size(),
                    snapshot.allBlockIds.size(), selected.size());
            graphics.drawCenteredString(this.font, trim(count, this.width - 24),
                    this.width / 2, PoolLayout.STATUS_Y, 0xA0A0A0);
        }
        if (!statusMessage.isEmpty() && System.currentTimeMillis() < statusUntil) {
            graphics.drawCenteredString(this.font,
                    trim(Component.literal(statusMessage), this.width - 24),
                    this.width / 2, PoolLayout.MESSAGE_Y, statusColor);
        }

        renderGrid(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (layout != null) {
            Component pageText = Component.translatable("rsl.pool.page", page + 1, pageCount());
            if (layout.twoFooterRows) {
                graphics.drawCenteredString(this.font, pageText, layout.pageLabelX,
                        layout.pageY + 6, 0xAAAAAA);
            } else {
                graphics.drawString(this.font, pageText, layout.pageLabelX,
                        layout.pageY + 6, 0xAAAAAA);
            }
        }
    }

    private Component trim(Component text, int maxWidth) {
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), maxWidth));
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        if (snapshot == null || layout == null) {
            return;
        }
        graphics.fill(layout.gridLeft - 4, layout.gridTop - 4,
                layout.gridLeft + layout.gridWidth + 4, layout.gridTop + layout.gridHeight + 4,
                0x44202020);
        String hoveredId = null;
        int hoveredX = 0;
        int hoveredY = 0;
        for (int row = 0; row < layout.rows; row++) {
            for (int col = 0; col < layout.columns; col++) {
                int index = page * layout.pageSize + row * layout.columns + col;
                if (index >= filtered.size()) {
                    break;
                }
                String id = filtered.get(index);
                int x = layout.gridLeft + col * (layout.cell + PoolLayout.GAP);
                int y = layout.gridTop + row * (layout.cell + PoolLayout.GAP);
                int cellSize = layout.cell;
                boolean isSelected = selected.contains(id);
                boolean isHovered = mouseX >= x && mouseX < x + cellSize
                        && mouseY >= y && mouseY < y + cellSize;
                boolean inPool = snapshot.poolIds.contains(id);
                graphics.fill(x, y, x + cellSize, y + cellSize,
                        isSelected ? (inPool ? 0x55A53A3A : 0x553AA55A) : 0x55202020);
                drawIcon(graphics, id, x + (cellSize - 16) / 2, y + (cellSize - 16) / 2);
                if (isSelected || isHovered) {
                    int border = isSelected ? (inPool ? 0xFFFF6666 : 0xFF66FF66) : 0xFFB0B0B0;
                    graphics.fill(x, y, x + cellSize, y + 1, border);
                    graphics.fill(x, y + cellSize - 1, x + cellSize, y + cellSize, border);
                    graphics.fill(x, y, x + 1, y + cellSize, border);
                    graphics.fill(x + cellSize - 1, y, x + cellSize, y + cellSize, border);
                }
                if (isHovered) {
                    hoveredId = id;
                    hoveredX = mouseX;
                    hoveredY = mouseY;
                }
            }
        }
        if (hoveredId != null) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.literal(displayNames.getOrDefault(hoveredId, hoveredId)),
                            Component.literal(hoveredId), Component.translatable("rsl.pool.tooltip")),
                    hoveredX, hoveredY);
        }
    }

    private void drawIcon(GuiGraphics graphics, String id, int x, int y) {
        ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier == null || !ForgeRegistries.BLOCKS.containsKey(identifier)) {
            return;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(identifier);
        if (block == null) {
            return;
        }
        ItemStack stack = new ItemStack(block.asItem());
        if (stack.isEmpty()) {
            int color = 0xFF000000 | (id.hashCode() & 0x7F7F7F);
            graphics.fill(x + 3, y + 3, x + 15, y + 15, color);
            return;
        }
        graphics.renderItem(stack, x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && snapshot != null && snapshot.canEdit && layout != null) {
            int step = layout.cell + PoolLayout.GAP;
            int col = (int) ((mouseX - layout.gridLeft) / step);
            int row = (int) ((mouseY - layout.gridTop) / step);
            if (mouseX >= layout.gridLeft && mouseY >= layout.gridTop
                    && col >= 0 && col < layout.columns && row >= 0 && row < layout.rows) {
                int index = page * layout.pageSize + row * layout.columns + col;
                if (index < filtered.size()) {
                    String id = filtered.get(index);
                    if (!selected.remove(id)) {
                        selected.add(id);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (layout == null || pageCount() <= 1) {
            return super.mouseScrolled(mouseX, mouseY, amount);
        }
        changePage(-(int) Math.signum(amount));
        return true;
    }

    @Override
    public void onClose() {
        BlockPoolClientNetwork.setHandlers(null, null);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
