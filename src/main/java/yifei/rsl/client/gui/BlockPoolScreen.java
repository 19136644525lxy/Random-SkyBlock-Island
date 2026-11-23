package yifei.rsl.client.gui;

import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.common.ui.PoolLayout;
import yifei.rsl.network.BlockPoolClientNetwork;

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
    private TextFieldWidget searchField;
    private ButtonWidget applyButton;
    private ButtonWidget selectAllButton;
    private ButtonWidget clearButton;
    private int page;
    private String query = "";
    private String statusMessage = "";
    private int statusColor = 0xFFAA55;
    private long statusUntil;
    private boolean pending;

    public BlockPoolScreen() {
        super(Text.translatable("rsl.pool.title"));
        BlockPoolClientNetwork.setHandlers(this::onSnapshot, this::onResult);
    }

    @Override
    protected void init() {
        layout = PoolLayout.compute(this.width, this.height);

        searchField = new TextFieldWidget(this.textRenderer, layout.searchX,
                PoolLayout.SEARCH_Y, layout.searchWidth, PoolLayout.SEARCH_HEIGHT,
                Text.translatable("rsl.pool.search"));
        searchField.setPlaceholder(Text.translatable("rsl.pool.search"));
        searchField.setMaxLength(128);
        searchField.setText(query);
        searchField.setChangedListener(value -> {
            query = value;
            page = 0;
            rebuildFiltered();
        });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> changePage(-1))
                .dimensions(layout.pagePrevX, layout.pageY, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> changePage(1))
                .dimensions(layout.pageNextX, layout.pageY, 20, 20).build());

        int gap = 6;
        selectAllButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.pool.select_all"), b -> selectAllFiltered())
                .dimensions(layout.actionStartX, layout.actionY, layout.actionButtonWidth, 20).build());
        clearButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.pool.clear"), b -> selected.clear())
                .dimensions(layout.actionStartX + layout.actionButtonWidth + gap, layout.actionY,
                        layout.actionButtonWidth, 20).build());
        applyButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.pool.apply"), b -> apply())
                .dimensions(layout.actionStartX + (layout.actionButtonWidth + gap) * 2, layout.actionY,
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
            setStatus(Text.translatable("rsl.pool.readonly").getString(), 0xFFAA55);
        }
        if (snapshot.poolIds.isEmpty()) {
            setStatus(Text.translatable("rsl.pool.empty").getString(), 0xFFAA55);
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
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return id;
        }
        Block block = Registries.BLOCK.get(identifier);
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
            setStatus(Text.translatable("rsl.pool.no_changes").getString(), 0xFFAA55);
            return;
        }
        pending = true;
        updateControls();
        setStatus(Text.translatable("rsl.pool.pending", add.size(), remove.size()).getString(), 0xFFAA55);
        BlockPoolClientNetwork.sendMutation(add, remove);
    }

    private void onResult(PoolMutationResult result) {
        pending = false;
        if (result.success) {
            selected.clear();
            setStatus(Text.translatable("rsl.pool.saved").getString(), 0x55FF55);
        } else {
            String base = Text.translatable(result.messageKey).getString();
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.fill(4, 4, this.width - 4, this.height - 4, 0x55101010);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, PoolLayout.TITLE_Y, 0xFFFFFF);

        if (snapshot != null) {
            Text count = Text.translatable("rsl.pool.count", filtered.size(),
                    snapshot.allBlockIds.size(), selected.size());
            context.drawCenteredTextWithShadow(this.textRenderer, trim(count, this.width - 24),
                    this.width / 2, PoolLayout.STATUS_Y, 0xA0A0A0);
        }
        if (!statusMessage.isEmpty() && System.currentTimeMillis() < statusUntil) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    trim(Text.literal(statusMessage), this.width - 24),
                    this.width / 2, PoolLayout.MESSAGE_Y, statusColor);
        }

        renderGrid(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);

        if (layout != null) {
            Text pageText = Text.translatable("rsl.pool.page", page + 1, pageCount());
            int pageX = layout.twoFooterRows ? layout.pageLabelX : layout.pageLabelX;
            if (layout.twoFooterRows) {
                context.drawCenteredTextWithShadow(this.textRenderer, pageText,
                        pageX, layout.pageY + 6, 0xAAAAAA);
            } else {
                context.drawTextWithShadow(this.textRenderer, pageText,
                        pageX, layout.pageY + 6, 0xAAAAAA);
            }
        }
    }

    private Text trim(Text text, int maxWidth) {
        String value = this.textRenderer.trimToWidth(text.getString(), maxWidth);
        return Text.literal(value);
    }

    private void renderGrid(DrawContext context, int mouseX, int mouseY) {
        if (snapshot == null || layout == null) {
            return;
        }
        context.fill(layout.gridLeft - 4, layout.gridTop - 4,
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
                context.fill(x, y, x + cellSize, y + cellSize,
                        isSelected ? (inPool ? 0x55A53A3A : 0x553AA55A) : 0x55202020);
                drawIcon(context, id, x + (cellSize - 16) / 2, y + (cellSize - 16) / 2);
                if (isSelected || isHovered) {
                    int border = isSelected ? (inPool ? 0xFFFF6666 : 0xFF66FF66) : 0xFFB0B0B0;
                    context.fill(x, y, x + cellSize, y + 1, border);
                    context.fill(x, y + cellSize - 1, x + cellSize, y + cellSize, border);
                    context.fill(x, y, x + 1, y + cellSize, border);
                    context.fill(x + cellSize - 1, y, x + cellSize, y + cellSize, border);
                }
                if (isHovered) {
                    hoveredId = id;
                    hoveredX = mouseX;
                    hoveredY = mouseY;
                }
            }
        }
        if (hoveredId != null) {
            context.drawTooltip(this.textRenderer,
                    List.of(Text.literal(displayNames.getOrDefault(hoveredId, hoveredId)),
                            Text.literal(hoveredId), Text.translatable("rsl.pool.tooltip")),
                    hoveredX, hoveredY);
        }
    }

    private void drawIcon(DrawContext context, String id, int x, int y) {
        Identifier identifier = Identifier.tryParse(id);
        Block block = identifier == null ? null : Registries.BLOCK.get(identifier);
        if (block == null) {
            return;
        }
        ItemStack stack = new ItemStack(block.asItem());
        if (stack.isEmpty()) {
            int color = 0xFF000000 | (id.hashCode() & 0x7F7F7F);
            context.fill(x + 3, y + 3, x + 15, y + 15, color);
            return;
        }
        context.drawItem(stack, x, y);
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
    public void close() {
        BlockPoolClientNetwork.setHandlers(null, null);
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
