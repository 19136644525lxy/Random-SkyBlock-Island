package yifei.rsl.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import yifei.rsl.client.BlockPoolClientNetwork;
import yifei.rsl.common.config.BlockPoolSnapshot;
import yifei.rsl.common.config.HiddenBlocksFile;
import yifei.rsl.common.config.PoolMutationResult;
import yifei.rsl.common.ui.PoolLayout;
import yifei.rsl.common.ui.PoolSelection;

import java.nio.file.Path;
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
 * shown. Hiding is a client-side display filter; it never changes the server
 * pool.
 */
public class BlockPoolScreen extends Screen {
    private static final long REFRESH_TIMEOUT_MS = 5000L;

    private enum View {
        ALL, CURRENT_POOL, HIDDEN
    }

    private final Set<String> selected = new LinkedHashSet<>();
    private final Set<String> hiddenSelection = new LinkedHashSet<>();
    private final List<String> filtered = new ArrayList<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private BlockPoolSnapshot snapshot;
    private HiddenBlocksFile hiddenFile;
    private PoolLayout layout;
    private EditBox searchField;
    private Button applyButton;
    private Button selectAllButton;
    private Button clearButton;
    private Button viewPoolButton;
    private Button hideButton;
    private Button hiddenPoolButton;
    private Button refreshButton;
    private Button pagePrevButton;
    private Button pageNextButton;
    private View view = View.ALL;
    private boolean hidingMode;
    private boolean refreshPending;
    private boolean refreshLoadFailed;
    private long refreshDeadline;
    private int page;
    private String query = "";
    private String statusMessage = "";
    private int statusColor = 0xFFAA55;
    private long statusUntil;
    private boolean pending;
    private boolean confirmRemoveAll;

    public BlockPoolScreen() {
        super(Component.translatable("rsl.pool.title"));
        BlockPoolClientNetwork.setHandlers(this::onSnapshot, this::onResult);
    }

    @Override
    protected void init() {
        layout = PoolLayout.compute(this.width, this.height);
        if (hiddenFile == null) {
            hiddenFile = HiddenBlocksFile.load(FMLPaths.CONFIGDIR.get(), currentContextKey());
            if (hiddenFile.hadLoadError()) {
                setStatus(Component.translatable("rsl.pool.hidden_load_failed").getString(), 0xFFAA55);
            }
        }

        searchField = new EditBox(this.font, layout.searchX,
                PoolLayout.SEARCH_Y, layout.searchWidth, PoolLayout.SEARCH_HEIGHT,
                Component.translatable("rsl.pool.search"));
        searchField.setHint(Component.translatable("rsl.pool.search"));
        searchField.setMaxLength(128);
        searchField.setValue(query);
        searchField.setResponder(value -> {
            query = value;
            page = 0;
            rebuildFiltered();
        });
        addRenderableWidget(searchField);

        pagePrevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(layout.pagePrevX, layout.pageY,
                        PoolLayout.PAGE_BUTTON_WIDTH, PoolLayout.PAGE_HEIGHT).build());
        pagePrevButton.setTooltip(Tooltip.create(Component.translatable("rsl.pool.prev_page")));
        pageNextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(layout.pageNextX, layout.pageY,
                        PoolLayout.PAGE_BUTTON_WIDTH, PoolLayout.PAGE_HEIGHT).build());
        pageNextButton.setTooltip(Tooltip.create(Component.translatable("rsl.pool.next_page")));

        Button closeButton = addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(layout.closeX, layout.closeY, layout.closeSize, layout.closeSize).build());
        closeButton.setTooltip(Tooltip.create(Component.translatable("rsl.pool.close")));

        int gap = PoolLayout.ACTION_GAP;
        int height = PoolLayout.ACTION_HEIGHT;
        selectAllButton = addRenderableWidget(Button.builder(Component.translatable("rsl.pool.select_all"), b -> selectAllFiltered())
                .bounds(layout.actionStartX, layout.actionY, layout.actionButtonWidth, height).build());
        clearButton = addRenderableWidget(Button.builder(Component.translatable("rsl.pool.clear"), b -> clearSelection())
                .bounds(layout.actionStartX + layout.actionButtonWidth + gap, layout.actionY,
                        layout.actionButtonWidth, height).build());
        applyButton = addRenderableWidget(Button.builder(Component.translatable("rsl.pool.apply"), b -> apply())
                .bounds(layout.actionStartX + (layout.actionButtonWidth + gap) * 2, layout.actionY,
                        layout.actionButtonWidth, height).build());
        viewPoolButton = addRenderableWidget(Button.builder(viewPoolLabel(), b -> toggleViewPool())
                .bounds(layout.actionStartX + (layout.actionButtonWidth + gap) * 3, layout.actionY,
                        layout.actionButtonWidth, height).build());

        hideButton = addRenderableWidget(Button.builder(hideLabel(), b -> toggleHideMode())
                .bounds(layout.action2StartX, layout.action2Y,
                        layout.action2ButtonWidth, height).build());
        hiddenPoolButton = addRenderableWidget(Button.builder(hiddenPoolLabel(), b -> toggleHiddenView())
                .bounds(layout.action2StartX + layout.action2ButtonWidth + gap, layout.action2Y,
                        layout.action2ButtonWidth, height).build());
        refreshButton = addRenderableWidget(Button.builder(refreshLabel(), b -> refresh())
                .bounds(layout.action2StartX + (layout.action2ButtonWidth + gap) * 2, layout.action2Y,
                        layout.action2ButtonWidth, height).build());

        page = clampPage(page);
        updateControls();
        BlockPoolClientNetwork.requestSnapshot();
    }

    /**
     * Singleplayer uses the save folder, multiplayer uses the server address.
     * The old global file is used as a seed for contexts that have no file yet.
     */
    private static String currentContextKey() {
        Minecraft client = Minecraft.getInstance();
        IntegratedServer server = client.getSingleplayerServer();
        if (server != null) {
            Path root = server.getWorldPath(LevelResource.ROOT);
            Path name = root.getFileName();
            return "sp:" + (name == null ? root.toString() : name.toString());
        }
        ServerData data = client.getCurrentServer();
        if (data != null) {
            String address = data.ip;
            if (address == null || address.isBlank()) {
                address = data.name;
            }
            return "mp:" + address;
        }
        return "unknown";
    }

    private void onSnapshot(BlockPoolSnapshot snapshot) {
        boolean wasRefreshing = refreshPending;
        boolean hiddenLoadFailed = refreshLoadFailed;
        this.snapshot = snapshot;
        refreshPending = false;
        refreshLoadFailed = false;
        if (hidingMode) {
            hiddenSelection.clear();
        }
        PoolSelection.retainVisible(this.selected, currentSourceIds());
        this.confirmRemoveAll = false;
        this.displayNames.clear();
        for (String id : snapshot.allBlockIds) {
            this.displayNames.put(id, nameFor(id));
        }
        if (hiddenFile != null) {
            for (String id : hiddenFile.blocks().list()) {
                this.displayNames.putIfAbsent(id, nameFor(id));
            }
        }
        if (!snapshot.canEdit) {
            setStatus(Component.translatable("rsl.pool.readonly").getString(), 0xFFAA55);
        }
        if (snapshot.poolIds.isEmpty()) {
            setStatus(Component.translatable("rsl.pool.empty").getString(), 0xFFAA55);
        }
        rebuildFiltered();
        updateControls();
        if (wasRefreshing) {
            if (hiddenLoadFailed) {
                setStatus(Component.translatable("rsl.pool.hidden_load_failed").getString(),
                        0xFFAA55, 3000L);
            } else {
                setStatus(Component.translatable("rsl.pool.refresh_success").getString(),
                        0x55FF55, 3000L);
            }
        }
    }

    private Component viewPoolLabel() {
        return Component.translatable(view == View.CURRENT_POOL
                ? "rsl.pool.view_all" : "rsl.pool.view_pool");
    }

    private Component hideLabel() {
        return hidingMode
                ? Component.translatable("rsl.pool.hide_apply", hiddenSelection.size())
                : Component.translatable("rsl.pool.hide");
    }

    private Component hiddenPoolLabel() {
        return view == View.HIDDEN
                ? Component.translatable("rsl.pool.hidden_pool_back")
                : Component.translatable("rsl.pool.hidden_pool", hiddenCount());
    }

    private Component refreshLabel() {
        return Component.translatable(refreshPending
                ? "rsl.pool.refreshing" : "rsl.pool.refresh");
    }

    private void toggleViewPool() {
        if (hidingMode || refreshPending) {
            return;
        }
        view = view == View.CURRENT_POOL ? View.ALL : View.CURRENT_POOL;
        resetViewState();
        if (view == View.CURRENT_POOL) {
            setStatus(Component.translatable("rsl.pool.view_pool_hint").getString(), 0xFFAA55);
        } else {
            setStatus("", 0xFFAA55);
        }
        rebuildFiltered();
        updateControls();
    }

    private void toggleHiddenView() {
        if (hidingMode || refreshPending) {
            return;
        }
        view = view == View.HIDDEN ? View.ALL : View.HIDDEN;
        resetViewState();
        if (view == View.HIDDEN) {
            setStatus(Component.translatable("rsl.pool.hidden_hint").getString(), 0xFFAA55);
        } else {
            setStatus("", 0xFFAA55);
        }
        rebuildFiltered();
        updateControls();
    }

    private void toggleHideMode() {
        if (!canEditPool() || view == View.HIDDEN || pending || refreshPending) {
            return;
        }
        if (!hidingMode) {
            hidingMode = true;
            hiddenSelection.clear();
            selected.clear();
            confirmRemoveAll = false;
            setStatus(Component.translatable("rsl.pool.hide_hint").getString(), 0xFFAA55);
        } else {
            applyHide();
        }
        updateControls();
    }

    private void applyHide() {
        if (hiddenFile == null || !canEditPool()) {
            return;
        }
        if (hiddenSelection.isEmpty()) {
            hidingMode = false;
            setStatus(Component.translatable("rsl.pool.hide_empty").getString(), 0xFFAA55);
            return;
        }
        Set<String> before = new LinkedHashSet<>(hiddenFile.blocks().hidden());
        int added = hiddenFile.blocks().hideAll(hiddenSelection);
        if (added == 0) {
            hidingMode = false;
            hiddenSelection.clear();
            setStatus(Component.translatable("rsl.pool.hide_empty").getString(), 0xFFAA55);
            return;
        }
        if (!hiddenFile.save()) {
            Set<String> newlyHidden = new LinkedHashSet<>(hiddenFile.blocks().hidden());
            newlyHidden.removeAll(before);
            hiddenFile.blocks().showAll(newlyHidden);
            setStatus(Component.translatable("rsl.pool.hide_failed").getString(), 0xFF5555);
            return;
        }
        hidingMode = false;
        hiddenSelection.clear();
        selected.clear();
        confirmRemoveAll = false;
        setStatus(Component.translatable("rsl.pool.hide_saved", added).getString(), 0x55FF55);
        rebuildFiltered();
    }

    private void unhide(String id) {
        if (hiddenFile == null || !canEditPool()) {
            return;
        }
        if (!hiddenFile.blocks().show(id)) {
            return;
        }
        if (!hiddenFile.save()) {
            hiddenFile.blocks().hideAll(List.of(id));
            setStatus(Component.translatable("rsl.pool.unhide_failed").getString(), 0xFF5555);
            return;
        }
        setStatus(Component.translatable("rsl.pool.unhidden", id).getString(), 0x55FF55);
        rebuildFiltered();
        updateControls();
    }

    private void refresh() {
        if (hiddenFile == null || hidingMode || pending || refreshPending) {
            return;
        }
        refreshPending = true;
        refreshDeadline = System.currentTimeMillis() + REFRESH_TIMEOUT_MS;
        hiddenSelection.clear();
        confirmRemoveAll = false;
        refreshLoadFailed = !hiddenFile.reload();
        if (refreshLoadFailed) {
            setStatus(Component.translatable("rsl.pool.hidden_load_failed").getString(), 0xFFAA55);
        } else {
            setStatus(Component.translatable("rsl.pool.refreshing").getString(), 0xFFAA55);
        }
        rebuildFiltered();
        updateControls();
        BlockPoolClientNetwork.requestSnapshot();
    }

    private boolean canEditPool() {
        return snapshot != null && snapshot.canEdit;
    }

    private void updateControls() {
        boolean hasSnapshot = snapshot != null;
        boolean canEdit = canEditPool() && view != View.HIDDEN && !hidingMode;
        if (applyButton != null) {
            applyButton.active = canEdit && !pending;
        }
        if (selectAllButton != null) {
            selectAllButton.active = canEdit && !pending;
            selectAllButton.setMessage(selectAllLabel());
        }
        if (clearButton != null) {
            clearButton.active = hidingMode ? !hiddenSelection.isEmpty() : canEdit;
        }
        if (hideButton != null) {
            hideButton.active = canEditPool() && view != View.HIDDEN && !pending && !refreshPending;
            hideButton.setMessage(hideLabel());
        }
        if (hiddenPoolButton != null) {
            hiddenPoolButton.active = hasSnapshot && !hidingMode && !refreshPending;
            hiddenPoolButton.setMessage(hiddenPoolLabel());
        }
        if (viewPoolButton != null) {
            viewPoolButton.active = hasSnapshot && !hidingMode && !refreshPending && view != View.HIDDEN;
            viewPoolButton.setMessage(viewPoolLabel());
        }
        if (refreshButton != null) {
            refreshButton.active = hasSnapshot && !hidingMode && !pending && !refreshPending;
            refreshButton.setMessage(refreshLabel());
        }
        if (pagePrevButton != null) {
            pagePrevButton.active = page > 0;
        }
        if (pageNextButton != null) {
            pageNextButton.active = page < pageCount() - 1;
        }
    }

    private Component selectAllLabel() {
        return Component.translatable(view == View.CURRENT_POOL
                ? "rsl.pool.select_all_remove"
                : "rsl.pool.select_all");
    }

    private String nameFor(String id) {
        ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier == null || !ForgeRegistries.BLOCKS.containsKey(identifier)) {
            return id;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(identifier);
        return block == null ? id : block.getName().getString();
    }

    private List<String> currentSourceIds() {
        if (snapshot == null) {
            return List.of();
        }
        if (view == View.HIDDEN) {
            return hiddenFile == null ? List.of() : hiddenFile.blocks().list();
        }
        List<String> base = view == View.CURRENT_POOL ? snapshot.poolIds : snapshot.allBlockIds;
        return hiddenFile == null ? base : hiddenFile.blocks().filterVisible(base);
    }

    private void rebuildFiltered() {
        filtered.clear();
        if (snapshot == null) {
            return;
        }
        String search = query.trim().toLowerCase(Locale.ROOT);
        for (String id : currentSourceIds()) {
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

    private int hiddenCount() {
        return hiddenFile == null ? 0 : hiddenFile.blocks().count();
    }

    private int visibleCount(List<String> ids) {
        int count = 0;
        for (String id : ids) {
            if (hiddenFile == null || !hiddenFile.blocks().isHidden(id)) {
                count++;
            }
        }
        return count;
    }

    private Component countText() {
        if (view == View.HIDDEN) {
            return Component.translatable("rsl.pool.count_hidden", filtered.size(), hiddenCount());
        }
        int total = view == View.CURRENT_POOL
                ? visibleCount(snapshot.poolIds)
                : visibleCount(snapshot.allBlockIds);
        String key = view == View.CURRENT_POOL ? "rsl.pool.count_pool" : "rsl.pool.count";
        return Component.translatable(key, filtered.size(), total, selected.size());
    }

    private int pageCount() {
        return Math.max(1, (filtered.size() + layout.pageSize - 1) / layout.pageSize);
    }

    private int clampPage(int value) {
        return Math.max(0, Math.min(value, pageCount() - 1));
    }

    private void changePage(int delta) {
        page = clampPage(page + delta);
        updateControls();
    }

    private void selectAllFiltered() {
        if (canEditPool() && view != View.HIDDEN && !hidingMode) {
            selected.addAll(filtered);
            confirmRemoveAll = false;
            updateControls();
        }
    }

    private void clearSelection() {
        if (hidingMode) {
            hiddenSelection.clear();
        } else {
            selected.clear();
        }
        confirmRemoveAll = false;
        updateControls();
    }

    private void resetViewState() {
        selected.clear();
        hiddenSelection.clear();
        confirmRemoveAll = false;
        page = 0;
    }

    private void apply() {
        if (!canEditPool() || view == View.HIDDEN || hidingMode || pending) {
            return;
        }
        PoolSelection.Diff diff = PoolSelection.diff(selected, snapshot.poolIds);
        if (diff.isEmpty()) {
            confirmRemoveAll = false;
            setStatus(Component.translatable("rsl.pool.no_changes").getString(), 0xFFAA55);
            return;
        }
        if (PoolSelection.removesWholePool(diff, snapshot.poolIds) && !confirmRemoveAll) {
            confirmRemoveAll = true;
            setStatus(Component.translatable("rsl.pool.confirm_remove_all",
                    diff.remove().size()).getString(), 0xFFAA55);
            return;
        }
        confirmRemoveAll = false;
        pending = true;
        updateControls();
        setStatus(Component.translatable("rsl.pool.pending", diff.add().size(),
                diff.remove().size()).getString(), 0xFFAA55);
        BlockPoolClientNetwork.sendMutation(diff.add(), diff.remove());
    }

    private void onResult(PoolMutationResult result) {
        pending = false;
        refreshPending = false;
        refreshLoadFailed = false;
        confirmRemoveAll = false;
        if ("rsl.pool.no_permission".equals(result.messageKey)) {
            // Access was revoked after the screen opened: hide the editor again.
            onClose();
            return;
        }
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
        setStatus(message, color, 5000L);
    }

    private void setStatus(String message, int color, long durationMs) {
        statusMessage = message == null ? "" : message;
        statusColor = color;
        statusUntil = System.currentTimeMillis() + Math.max(0L, durationMs);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (refreshPending && System.currentTimeMillis() > refreshDeadline) {
            refreshPending = false;
            refreshLoadFailed = false;
            setStatus(Component.translatable("rsl.pool.refresh_timeout").getString(),
                    0xFFAA55, 3000L);
            updateControls();
        }
        renderBackground(graphics);
        graphics.fill(4, 4, this.width - 4, this.height - 4, 0x55101010);
        graphics.fill(4, 4, this.width - 4, 72, 0x66101010);
        if (layout != null) {
            graphics.fill(4, layout.action2Y - 8, this.width - 4, this.height - 4, 0x77101010);
            graphics.fill(4, layout.action2Y - 9, this.width - 4, layout.action2Y - 8, 0x33FFFFFF);
        }
        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, PoolLayout.TITLE_Y, 0xFFFFD166);

        if (snapshot != null && layout != null) {
            graphics.drawCenteredString(this.font, trim(countText(), this.width - 24),
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
            graphics.drawCenteredString(this.font, pageText, layout.pageLabelX,
                    layout.pageY + 6, 0xAAAAAA);
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
        long now = System.currentTimeMillis();
        float pulse = (float) ((Math.sin(now / 180.0) + 1.0) / 2.0);
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
                boolean isMarked = hiddenSelection.contains(id);
                boolean isHovered = mouseX >= x && mouseX < x + cellSize
                        && mouseY >= y && mouseY < y + cellSize;
                boolean inPool = snapshot.poolIds.contains(id);
                int fill = isMarked ? 0x66FF9900
                        : isSelected ? (inPool ? 0x55A53A3A : 0x553AA55A)
                        : 0x55202020;
                graphics.fill(x, y, x + cellSize, y + cellSize, fill);
                if (isHovered && !isMarked && !isSelected) {
                    int glowAlpha = 0x20 + (int) (0x38 * pulse);
                    graphics.fill(x - 1, y - 1, x + cellSize + 1, y + cellSize + 1,
                            (glowAlpha << 24) | 0x9FD8FF);
                }
                int lift = isHovered ? -1 - Math.round(pulse * 2.0f) : 0;
                drawIcon(graphics, id, x + (cellSize - 16) / 2,
                        y + (cellSize - 16) / 2 + lift);
                if (isMarked || isSelected || isHovered) {
                    int border = isMarked ? 0xFFFFAA00
                            : isSelected ? (inPool ? 0xFFFF6666 : 0xFF66FF66)
                            : ((0x80 + (int) (0x7F * pulse)) << 24) | 0xE8F4FF;
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
            String tooltipKey = view == View.HIDDEN
                    ? "rsl.pool.hidden_tooltip"
                    : hidingMode ? "rsl.pool.hide_tooltip" : "rsl.pool.tooltip";
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.literal(displayNames.getOrDefault(hoveredId, hoveredId)),
                            Component.literal(hoveredId), Component.translatable(tooltipKey)),
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
        if (button == 0 && layout != null && snapshot != null) {
            int step = layout.cell + PoolLayout.GAP;
            int col = (int) ((mouseX - layout.gridLeft) / step);
            int row = (int) ((mouseY - layout.gridTop) / step);
            if (mouseX >= layout.gridLeft && mouseY >= layout.gridTop
                    && col >= 0 && col < layout.columns && row >= 0 && row < layout.rows) {
                int index = page * layout.pageSize + row * layout.columns + col;
                if (index < filtered.size()) {
                    String id = filtered.get(index);
                    if (view == View.HIDDEN) {
                        if (canEditPool() && !pending) {
                            unhide(id);
                        }
                        return true;
                    }
                    if (hidingMode) {
                        if (canEditPool() && !pending) {
                            if (!hiddenSelection.remove(id)) {
                                hiddenSelection.add(id);
                            }
                            updateControls();
                        }
                        return true;
                    }
                    if (canEditPool() && !pending) {
                        if (!selected.remove(id)) {
                            selected.add(id);
                        }
                        confirmRemoveAll = false;
                        updateControls();
                        return true;
                    }
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
