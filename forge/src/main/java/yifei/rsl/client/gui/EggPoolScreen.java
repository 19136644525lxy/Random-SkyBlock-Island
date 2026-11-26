package yifei.rsl.client.gui;

import yifei.rsl.client.ClientSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import yifei.rsl.client.ClientEggHandler;
import yifei.rsl.common.config.HiddenBlocksFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Other pools" screen: egg-pool block picker plus the egg-mode settings.
 * Chance and volume are free-text fields (not preset cycles); the 3D volume
 * preview opens in its own screen, the grid honours the per-save hidden list,
 * and the whole panel can switch to a full-screen layout.
 */
public final class EggPoolScreen extends Screen {
    private boolean openedSound;
    private static final int GAP = 8;
    private static final int CELL = 18;
    private static final int GRID_TOP = 146;
    private static final int WINDOW_W = 460;
    private static final int WINDOW_H = 380;

    private final BlockPoolScreen parent;
    private final HiddenBlocksFile hiddenFile;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int columns;
    private int rowsPerPage;

    private List<String> allIds = List.of();
    private final Set<String> selected = new HashSet<>();
    private int page;

    private boolean enabled;
    private double chance = 0.20;
    private int volume = 27;

    private Button toggleButton;
    private EditBox chanceField;
    private EditBox volumeField;
    private Button prevButton;
    private Button nextButton;

    private String statusMessage = "";
    private int statusColor = 0xFFA0A0A0;

    public EggPoolScreen(BlockPoolScreen parent, HiddenBlocksFile hiddenFile) {
        super(Component.translatable("rsl.egg.other_title"));
        this.parent = parent;
        this.hiddenFile = hiddenFile;
    }

    @Override
    protected void init() {
        if (!openedSound) {
            openedSound = true;
            ClientSounds.play(SoundEvents.BOOK_PAGE_TURN, 0.4F, 1.0F);
        }
        this.panelW = this.width;
        this.panelH = this.height;
        this.panelX = 0;
        this.panelY = 0;
        ClientEggHandler.setHandlers(this::onSnapshot, this::onResult);

        int rowW = Math.min(560, this.panelW - 32);
        int rowLeft = this.panelX + (this.panelW - rowW) / 2;
        int buttonW = (rowW - GAP * 4) / 5;
        int rowY = this.panelY + 40;
        this.toggleButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("rsl.egg.toggle_off"), button -> {
                            this.enabled = !this.enabled;
                            this.updateControls();
                        })
                .bounds(rowLeft, rowY, buttonW, 20).build());

        this.chanceField = new EditBox(this.font, rowLeft + buttonW + GAP, rowY, buttonW, 20,
                Component.translatable("rsl.egg.chance_hint"));
        this.chanceField.setMaxLength(3);
        this.chanceField.setHint(Component.translatable("rsl.egg.chance_hint"));
        this.chanceField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,3}"));
        this.chanceField.setValue(String.valueOf((int) Math.round(this.chance * 100.0)));
        this.addRenderableWidget(this.chanceField);

        this.volumeField = new EditBox(this.font, rowLeft + (buttonW + GAP) * 2, rowY, buttonW, 20,
                Component.translatable("rsl.egg.volume_hint"));
        this.volumeField.setMaxLength(4);
        this.volumeField.setHint(Component.translatable("rsl.egg.volume_hint"));
        this.volumeField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,4}"));
        this.volumeField.setValue(String.valueOf(this.volume));
        this.addRenderableWidget(this.volumeField);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("rsl.egg.preview_button"),
                        button -> Minecraft.getInstance().setScreen(
                                new EggPreviewScreen(this, this.parsedVolume(), new ArrayList<>(this.selected))))
                .bounds(rowLeft + (buttonW + GAP) * 3, rowY, buttonW, 20).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("rsl.egg.apply"), button -> this.sendApply())
                .bounds(rowLeft + (buttonW + GAP) * 4, rowY, buttonW, 20).build());

        this.columns = Math.max(1, (this.panelW - 32) / (CELL + 4));
        int gridHeight = (this.panelH - 40) - GRID_TOP;
        this.rowsPerPage = Math.max(1, gridHeight / (CELL + 4));

        int footerY = this.panelY + this.panelH - 32;
        this.prevButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(-1))
                .bounds(this.panelX + 16, footerY, 24, 20).build());
        this.nextButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1))
                .bounds(this.panelX + 16 + 28, footerY, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("rsl.egg.back"), button -> this.onClose())
                .bounds(this.panelX + this.panelW - 16 - 100, footerY, 100, 20).build());

        this.page = Math.min(this.page, Math.max(0, this.pageCount() - 1));
        this.updateControls();
        ClientEggHandler.request();
    }

    private List<String> visibleIds() {
        if (this.hiddenFile == null) {
            return this.allIds;
        }
        List<String> visible = new ArrayList<>(this.allIds.size());
        for (String id : this.allIds) {
            if (!this.hiddenFile.blocks().isHidden(id)) {
                visible.add(id);
            }
        }
        return visible;
    }

    private int parsedVolume() {
        if (this.volumeField == null) {
            return this.volume;
        }
        String text = this.volumeField.getValue().trim();
        if (text.isEmpty()) {
            return this.volume;
        }
        try {
            return Math.max(1, Math.min(4096, Integer.parseInt(text)));
        } catch (NumberFormatException exception) {
            return this.volume;
        }
    }

    private double parsedChance() {
        if (this.chanceField == null) {
            return this.chance;
        }
        String text = this.chanceField.getValue().trim();
        if (text.isEmpty()) {
            return this.chance;
        }
        try {
            int percent = Math.max(1, Math.min(100, Integer.parseInt(text)));
            return percent / 100.0;
        } catch (NumberFormatException exception) {
            return this.chance;
        }
    }

    private void onSnapshot(ClientEggHandler.Snapshot snapshot) {
        this.allIds = snapshot.allIds();
        this.selected.clear();
        this.selected.addAll(snapshot.selectedIds());
        this.enabled = snapshot.enabled();
        this.chance = snapshot.chance();
        this.volume = snapshot.volume();
        if (this.chanceField != null) {
            this.chanceField.setValue(String.valueOf((int) Math.round(this.chance * 100.0)));
        }
        if (this.volumeField != null) {
            this.volumeField.setValue(String.valueOf(this.volume));
        }
        this.page = Math.min(this.page, Math.max(0, this.pageCount() - 1));
        this.updateControls();
    }

    private void onResult(ClientEggHandler.Result result) {
        this.statusMessage = Component.translatable(result.messageKey()).getString();
        this.statusColor = result.success() ? 0xFF66DD66 : 0xFFFF6666;
        this.updateControls();
    }

    private void sendApply() {
        this.statusMessage = "";
        ClientEggHandler.apply(this.enabled, this.parsedChance(), this.parsedVolume(),
                new ArrayList<>(this.selected));
    }

    private void updateControls() {
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(Component.translatable(this.enabled ? "rsl.egg.toggle_on" : "rsl.egg.toggle_off"));
            this.prevButton.active = this.page > 0;
            this.nextButton.active = this.page + 1 < this.pageCount();
        }
    }

    private int pageCount() {
        int pageSize = this.pageSize();
        return Math.max(1, (this.visibleIds().size() + pageSize - 1) / pageSize);
    }

    private int pageSize() {
        return this.columns * this.rowsPerPage;
    }

    private void changePage(int delta) {
        this.page = Math.max(0, Math.min(this.pageCount() - 1, this.page + delta));
        this.updateControls();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, 0xEE101418);
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 2, 0xFF66E0FF);
        graphics.drawCenteredString(this.font, this.title,
                this.panelX + this.panelW / 2, this.panelY + 12, 0xFFFFD166);
        graphics.drawCenteredString(this.font, Component.translatable("rsl.egg.subtitle"),
                this.panelX + this.panelW / 2, this.panelY + 26, 0xFFA0A0A0);

        this.renderGrid(graphics, mouseX, mouseY);

        if (!this.statusMessage.isEmpty()) {
            graphics.drawCenteredString(this.font, this.statusMessage,
                    this.panelX + this.panelW / 2, this.panelY + this.panelH - 46, this.statusColor);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> ids = this.visibleIds();
        int gridLeft = this.panelX + (this.panelW - this.columns * (CELL + 4)) / 2;
        int gridTop = this.panelY + GRID_TOP;
        graphics.fill(gridLeft - 4, gridTop - 4,
                gridLeft + this.columns * (CELL + 4) + 2, gridTop + this.rowsPerPage * (CELL + 4) + 2,
                0x44202020);
        for (int row = 0; row < this.rowsPerPage; row++) {
            for (int col = 0; col < this.columns; col++) {
                int index = this.page * this.pageSize() + row * this.columns + col;
                if (index >= ids.size()) {
                    break;
                }
                String id = ids.get(index);
                int x = gridLeft + col * (CELL + 4);
                int y = gridTop + row * (CELL + 4);
                boolean isSelected = this.selected.contains(id);
                graphics.fill(x, y, x + CELL, y + CELL, isSelected ? 0x553AA55A : 0x55202020);
                this.drawIcon(graphics, id, x + 1, y + 1);
                if (isSelected) {
                    graphics.fill(x, y, x + CELL, y + 1, 0xFF66FF66);
                    graphics.fill(x, y + CELL - 1, x + CELL, y + CELL, 0xFF66FF66);
                    graphics.fill(x, y, x + 1, y + CELL, 0xFF66FF66);
                    graphics.fill(x + CELL - 1, y, x + CELL, y + CELL, 0xFF66FF66);
                } else if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                    graphics.fill(x - 1, y - 1, x + CELL + 1, y + CELL + 1, 0x409FD8FF);
                }
            }
        }
        Component pageText = Component.translatable("rsl.egg.page", this.page + 1, this.pageCount());
        graphics.drawCenteredString(this.font, pageText,
                this.panelX + 16 + 52 + 60, this.panelY + this.panelH - 26, 0xFFA0A0A0);
    }

    private void drawIcon(GuiGraphics graphics, String id, int x, int y) {
        ResourceLocation identifier = ResourceLocation.tryParse(id);
        Block block = identifier == null ? null : ForgeRegistries.BLOCKS.getValue(identifier);
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
        List<String> ids = this.visibleIds();
        if (button == 0 && !ids.isEmpty()) {
            int gridLeft = this.panelX + (this.panelW - this.columns * (CELL + 4)) / 2;
            int gridTop = this.panelY + GRID_TOP;
            int step = CELL + 4;
            int col = (int) ((mouseX - gridLeft) / step);
            int row = (int) ((mouseY - gridTop) / step);
            if (mouseX >= gridLeft && mouseY >= gridTop
                    && col >= 0 && col < this.columns && row >= 0 && row < this.rowsPerPage) {
                int index = this.page * this.pageSize() + row * this.columns + col;
                if (index < ids.size()) {
                    String id = ids.get(index);
                    if (!this.selected.remove(id)) {
                        this.selected.add(id);
                    }
                    this.updateControls();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.pageCount() > 1) {
            this.changePage(-(int) Math.signum(amount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void onClose() {
        ClientSounds.play(SoundEvents.BOOK_PAGE_TURN, 0.4F, 0.85F);
        ClientEggHandler.setHandlers(null, null);
        Minecraft.getInstance().setScreen(this.parent);
    }
}
