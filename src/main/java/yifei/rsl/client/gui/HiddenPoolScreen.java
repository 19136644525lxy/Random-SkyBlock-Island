package yifei.rsl.client.gui;

import yifei.rsl.client.ClientSounds;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

import yifei.rsl.common.config.HiddenBlocksFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for the current save's hidden block list. It can rename the list and
 * copy another save's list into this save. Names are display-only; file names
 * stay the per-context SHA-256 hash.
 */
public final class HiddenPoolScreen extends Screen {
    private boolean openedSound;
    private static final int PANEL_W = 400;
    private static final int PANEL_H = 320;
    private static final int BUTTON_H = 20;
    private static final int GAP = 8;
    private static final int ROW_H = 20;
    private static final int PAGE_SIZE = 6;

    private final BlockPoolScreen parent;
    private final HiddenBlocksFile file;
    private final List<HiddenBlocksFile.PoolInfo> pools = new ArrayList<>();
    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private TextFieldWidget nameField;
    private ButtonWidget copyButton;
    private ButtonWidget cancelButton;
    private ButtonWidget prevPageButton;
    private ButtonWidget nextPageButton;
    private int selectedIndex = -1;
    private int page;
    private boolean confirming;
    private String statusMessage = "";
    private int statusColor = 0xFFAA55;
    private int panelX;
    private int panelY;

    public HiddenPoolScreen(BlockPoolScreen parent, HiddenBlocksFile file) {
        super(Text.translatable("rsl.pool.manage_title"));
        this.parent = parent;
        this.file = file;
    }

    @Override
    protected void init() {
        if (!openedSound) {
            openedSound = true;
            ClientSounds.play(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.4F, 1.0F);
        }
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;
        refreshingPools();

        int fieldWidth = PANEL_W - 32 - 88;
        nameField = new TextFieldWidget(this.textRenderer, panelX + 16, panelY + 50,
                fieldWidth, 20, Text.translatable("rsl.pool.manage_name_label"));
        nameField.setMaxLength(32);
        nameField.setText(file.name());
        nameField.setPlaceholder(Text.translatable("rsl.pool.rename_hint"));
        addDrawableChild(nameField);
        setInitialFocus(nameField);
        addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.pool.rename_save"),
                        b -> saveName())
                .dimensions(panelX + 16 + fieldWidth + GAP, panelY + 50, 80, 20).build());

        rowButtons.clear();
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = i;
            ButtonWidget button = ButtonWidget.builder(Text.empty(), b -> select(index))
                    .dimensions(panelX + 16, panelY + 96 + i * ROW_H,
                            PANEL_W - 32, ROW_H - 2).build();
            addDrawableChild(button);
            rowButtons.add(button);
        }

        prevPageButton = addDrawableChild(ButtonWidget.builder(Text.literal("<"),
                        b -> changePage(-1))
                .dimensions(panelX + 16, panelY + 220, 24, 18).build());
        prevPageButton.setTooltip(Tooltip.of(Text.translatable("rsl.pool.prev_page")));
        nextPageButton = addDrawableChild(ButtonWidget.builder(Text.literal(">"),
                        b -> changePage(1))
                .dimensions(panelX + PANEL_W - 16 - 24, panelY + 220, 24, 18).build());
        nextPageButton.setTooltip(Tooltip.of(Text.translatable("rsl.pool.next_page")));

        copyButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("rsl.pool.manage_copy"),
                        b -> copySelected())
                .dimensions(panelX + 16, panelY + PANEL_H - 34, 140, BUTTON_H).build());
        cancelButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("rsl.pool.rename_cancel"),
                        b -> cancel())
                .dimensions(panelX + PANEL_W - 16 - 80, panelY + PANEL_H - 34, 80, BUTTON_H).build());
        refreshRows();
    }

    private void refreshingPools() {
        pools.clear();
        if (file.path().getParent() == null || file.path().getParent().getParent() == null) {
            return;
        }
        for (HiddenBlocksFile.PoolInfo pool : HiddenBlocksFile.listAll(
                file.path().getParent().getParent())) {
            if (!pool.path().toAbsolutePath().equals(file.path().toAbsolutePath())) {
                pools.add(pool);
            }
        }
        if (selectedIndex >= pools.size()) {
            selectedIndex = pools.isEmpty() ? -1 : pools.size() - 1;
        }
    }

    private int pageCount() {
        return Math.max(1, (pools.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void refreshRows() {
        page = Math.max(0, Math.min(page, pageCount() - 1));
        for (int i = 0; i < rowButtons.size(); i++) {
            int index = page * PAGE_SIZE + i;
            ButtonWidget button = rowButtons.get(i);
            if (index >= pools.size()) {
                button.visible = false;
                continue;
            }
            HiddenBlocksFile.PoolInfo pool = pools.get(index);
            button.visible = true;
            String prefix = index == selectedIndex ? "▶ " : "";
            button.setMessage(Text.translatable("rsl.pool.manage_entry",
                    prefix + pool.displayName(), pool.count()));
        }
        if (prevPageButton != null) {
            prevPageButton.active = page > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.active = page < pageCount() - 1;
        }
        if (copyButton != null) {
            copyButton.active = selectedIndex >= 0;
        }
        if (pools.isEmpty()) {
            statusMessage = Text.translatable("rsl.pool.manage_empty").getString();
            statusColor = 0xFF888888;
        }
    }

    private void changePage(int delta) {
        page += delta;
        confirming = false;
        refreshRows();
    }

    private void select(int index) {
        if (index < 0 || index >= pools.size()) {
            return;
        }
        selectedIndex = index;
        confirming = false;
        statusMessage = "";
        copyButton.setMessage(Text.translatable("rsl.pool.manage_copy"));
        refreshRows();
    }

    private void saveName() {
        file.setName(nameField.getText());
        if (!file.save()) {
            statusMessage = Text.translatable("rsl.pool.rename_failed").getString();
            statusColor = 0xFF5555;
        }
    }

    private void copySelected() {
        if (selectedIndex < 0 || selectedIndex >= pools.size()) {
            return;
        }
        HiddenBlocksFile.PoolInfo selected = pools.get(selectedIndex);
        if (!confirming && file.blocks().count() > 0) {
            confirming = true;
            statusMessage = Text.translatable("rsl.pool.manage_confirm", selected.count())
                    .getString();
            statusColor = 0xFFAA55;
            copyButton.setMessage(Text.translatable("rsl.pool.manage_confirm_button"));
            return;
        }
        if (!file.importFrom(selected.path())) {
            confirming = false;
            statusMessage = Text.translatable("rsl.pool.manage_copy_failed").getString();
            statusColor = 0xFF5555;
            copyButton.setMessage(Text.translatable("rsl.pool.manage_copy"));
            return;
        }
        parent.reloadHiddenFile();
        this.client.setScreen(parent);
    }

    private void cancel() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveName();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(4, 4, this.width - 4, this.height - 4, 0xCC101010);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, panelY + 14, 0xFFFFD166);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("rsl.pool.manage_description"),
                this.width / 2, panelY + 30, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("rsl.pool.manage_name_label"),
                panelX + 16, panelY + 36, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("rsl.pool.manage_copy_label"),
                panelX + 16, panelY + 84, 0xA0A0A0);
        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage,
                    this.width / 2, panelY + 246, statusColor);
        }
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("rsl.pool.page", page + 1, pageCount()),
                this.width / 2, panelY + 226, 0xA0A0A0);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        ClientSounds.play(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.4F, 0.85F);
        cancel();
    }
}
