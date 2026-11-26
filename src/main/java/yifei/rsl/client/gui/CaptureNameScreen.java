package yifei.rsl.client.gui;

import yifei.rsl.client.ClientSounds;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import yifei.rsl.network.CaptureStickClientNetwork;
import yifei.rsl.network.CaptureStickNetwork;

/**
 * Small modal screen shown after sneak-right-clicking a filled container with the
 * capture stick. Lets the player name the container before it is stored in the pool.
 */
public final class CaptureNameScreen extends Screen {
    private boolean openedSound;
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 150;

    private final String defaultName;
    private TextFieldWidget nameField;
    private boolean submitted;
    private int panelX;
    private int panelY;

    public CaptureNameScreen(String blockState, String defaultName) {
        super(Text.translatable("rsl.capture.title"));
        this.defaultName = defaultName;
    }

    @Override
    protected void init() {
        if (!openedSound) {
            openedSound = true;
            ClientSounds.play(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.4F, 1.0F);
        }
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.nameField = new TextFieldWidget(this.textRenderer, this.panelX + 16, this.panelY + 52, PANEL_W - 32, 20,
                Text.translatable("rsl.capture.name_label"));
        this.nameField.setMaxLength(CaptureStickNetwork.MAX_NAME_LENGTH);
        this.nameField.setText(this.defaultName == null ? "" : this.defaultName);
        this.nameField.setPlaceholder(Text.translatable("rsl.capture.name_hint"));
        this.addDrawableChild(this.nameField);
        int buttonWidth = (PANEL_W - 32 - 8) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.capture.confirm"), button -> this.confirm())
                .dimensions(this.panelX + 16, this.panelY + PANEL_H - 34, buttonWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.capture.cancel"), button -> this.cancel())
                .dimensions(this.panelX + 24 + buttonWidth, this.panelY + PANEL_H - 34, buttonWidth, 20).build());
        this.setInitialFocus(this.nameField);
    }

    private void confirm() {
        if (this.submitted) {
            return;
        }
        this.submitted = true;
        CaptureStickClientNetwork.confirm(this.nameField.getText());
        this.close();
    }

    private void cancel() {
        if (this.submitted) {
            return;
        }
        this.submitted = true;
        CaptureStickClientNetwork.cancel();
        this.close();
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
    public void close() {
        ClientSounds.play(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.4F, 0.85F);
        if (!this.submitted) {
            this.submitted = true;
            CaptureStickClientNetwork.cancel();
        }
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xE0101018);
        context.drawBorder(this.panelX, this.panelY, PANEL_W, PANEL_H, 0xFF60C8FF);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.panelY + 14, 0xFFD166);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("rsl.capture.name_label"),
                this.panelX + 16, this.panelY + 38, 0xFFD0D0E0);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
