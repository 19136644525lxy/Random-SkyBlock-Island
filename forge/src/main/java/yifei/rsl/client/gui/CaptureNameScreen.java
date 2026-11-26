package yifei.rsl.client.gui;

import yifei.rsl.client.ClientSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import yifei.rsl.client.ClientCaptureStickNetwork;
import yifei.rsl.network.CaptureStickNetwork;

/**
 * Forge mirror of the Fabric CaptureNameScreen: name the captured container before
 * it is stored in the block pool.
 */
public final class CaptureNameScreen extends Screen {
    private boolean openedSound;
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 150;

    private final String defaultName;
    private EditBox nameField;
    private boolean submitted;
    private int panelX;
    private int panelY;

    public CaptureNameScreen(String state, String defaultName) {
        super(Component.translatable("rsl.capture.title"));
        this.defaultName = defaultName;
    }

    @Override
    protected void init() {
        if (!openedSound) {
            openedSound = true;
            ClientSounds.play(SoundEvents.BOOK_PAGE_TURN, 0.4F, 1.0F);
        }
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.nameField = new EditBox(this.font, this.panelX + 16, this.panelY + 52, PANEL_W - 32, 20,
                Component.translatable("rsl.capture.name_label"));
        this.nameField.setMaxLength(CaptureStickNetwork.MAX_NAME_LENGTH);
        this.nameField.setValue(this.defaultName == null ? "" : this.defaultName);
        this.nameField.setHint(Component.translatable("rsl.capture.name_hint"));
        this.addRenderableWidget(this.nameField);
        int buttonWidth = (PANEL_W - 32 - 8) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("rsl.capture.confirm"), button -> this.confirm())
                .bounds(this.panelX + 16, this.panelY + PANEL_H - 34, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("rsl.capture.cancel"), button -> this.cancel())
                .bounds(this.panelX + 24 + buttonWidth, this.panelY + PANEL_H - 34, buttonWidth, 20).build());
        this.setInitialFocus(this.nameField);
    }

    private void confirm() {
        if (this.submitted) {
            return;
        }
        this.submitted = true;
        ClientCaptureStickNetwork.confirm(this.nameField.getValue());
        this.close();
    }

    private void cancel() {
        if (this.submitted) {
            return;
        }
        this.submitted = true;
        ClientCaptureStickNetwork.cancel();
        this.close();
    }

    private void close() {
        this.onClose();
    }

    @Override
    public void onClose() {
        ClientSounds.play(SoundEvents.BOOK_PAGE_TURN, 0.4F, 0.85F);
        if (!this.submitted) {
            this.submitted = true;
            ClientCaptureStickNetwork.cancel();
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.confirm();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.cancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xE0101018);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + 1, 0xFF60C8FF);
        graphics.fill(this.panelX, this.panelY + PANEL_H - 1, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xFF60C8FF);
        graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + PANEL_H, 0xFF60C8FF);
        graphics.fill(this.panelX + PANEL_W - 1, this.panelY, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xFF60C8FF);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.panelY + 14, 0xFFD166);
        graphics.drawString(this.font, Component.translatable("rsl.capture.name_label"),
                this.panelX + 16, this.panelY + 38, 0xFFD0D0E0);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
