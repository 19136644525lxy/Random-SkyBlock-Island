package yifei.rsl.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yifei.rsl.client.ClientGameplayActionHandler;
import yifei.rsl.client.ClientGameplayHandler;
import yifei.rsl.config.ModConfig;
import yifei.rsl.network.GameplayActionNetwork;

import java.util.List;

/**
 * Client-side launcher for the common gameplay commands. The server remains
 * authoritative: every button sends an action id, the server runs the matching
 * command, and the captured feedback is rendered inside this panel.
 */
public class GameplayScreen extends Screen {
    private static final int PANEL_W = 460;
    private static final int PANEL_H = 320;
    private static final int BUTTON_W = 132;
    private static final int BUTTON_H = 20;
    private static final int GAP = 8;
    private static final int RESULT_TOP = 196;
    private static final int RESULT_LINE_HEIGHT = 10;
    private static final int RESULT_MAX_LINES = 10;

    private EditBox delayField;
    private Component statusText = Component.translatable("rsl.menu.status_hint");
    private List<Component> resultLines = List.of();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int buttonWidth;
    private int startX;
    private int row1;
    private int row2;
    private int row3;
    private int delayWidth;
    private int setDelayX;
    private int setDelayWidth;
    private int statusX;
    private int statusWidth;
    private int closeX;
    private int closeWidth;

    public GameplayScreen() {
        super(Component.translatable("rsl.menu.title"));
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(PANEL_W, this.width - 16);
        this.panelHeight = Math.min(PANEL_H, this.height - 16);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        int inner = this.panelWidth - 48;
        this.buttonWidth = Math.max(64, (inner - GAP * 2) / 3);
        int gridWidth = this.buttonWidth * 3 + GAP * 2;
        this.startX = this.panelX + (this.panelWidth - gridWidth) / 2;
        this.row1 = this.panelY + 92;
        this.row2 = this.panelY + 120;
        this.row3 = this.panelY + 164;

        this.addRenderableWidget(primary("rsl.menu.start", ChatFormatting.GREEN,
                startX, row1, () -> sendAction(GameplayActionNetwork.ACTION_START)));
        this.addRenderableWidget(primary("rsl.menu.stop", ChatFormatting.YELLOW,
                startX + buttonWidth + GAP, row1, () -> sendAction(GameplayActionNetwork.ACTION_STOP)));
        this.addRenderableWidget(primary("rsl.menu.reset", ChatFormatting.RED,
                startX + (buttonWidth + GAP) * 2, row1, () -> sendAction(GameplayActionNetwork.ACTION_RESET)));

        int row2Width = this.buttonWidth * 3 + GAP * 2;
        int row2StartX = this.panelX + (this.panelWidth - row2Width) / 2;
        this.addRenderableWidget(primary("rsl.menu.wand", ChatFormatting.AQUA,
                row2StartX, row2, () -> sendAction(GameplayActionNetwork.ACTION_WAND)));
        this.addRenderableWidget(primary("rsl.menu.areas", ChatFormatting.WHITE,
                row2StartX + buttonWidth + GAP, row2, () -> sendAction(GameplayActionNetwork.ACTION_AREAS)));
        this.addRenderableWidget(primary("rsl.menu.access", ChatFormatting.GOLD,
                row2StartX + (buttonWidth + GAP) * 2, row2,
                () -> this.minecraft.setScreen(new PoolAccessScreen())));

        this.delayWidth = Math.min(70, Math.max(48, inner / 6));
        this.setDelayWidth = Math.min(90, Math.max(64, inner / 4));
        this.statusWidth = Math.min(110, Math.max(70, inner / 4));
        this.setDelayX = startX + delayWidth + GAP;
        this.statusX = setDelayX + setDelayWidth + GAP;
        int remaining = inner - delayWidth - setDelayWidth - statusWidth - GAP * 3;
        this.closeWidth = Math.max(40, Math.min(118, remaining));
        this.closeX = statusX + statusWidth + GAP;

        this.delayField = new EditBox(this.font, startX, row3, delayWidth, BUTTON_H,
                Component.translatable("rsl.menu.delay"));
        this.delayField.setMaxLength(4);
        int syncedDelay = ClientGameplayHandler.getSyncedDelaySeconds();
        this.delayField.setValue(Integer.toString(
                syncedDelay > 0 ? syncedDelay : ModConfig.getTransformDelaySeconds()));
        this.addRenderableWidget(this.delayField);
        ClientGameplayHandler.setHandler(seconds -> {
            if (this.delayField != null) {
                this.delayField.setValue(Integer.toString(seconds));
            }
        });
        ClientGameplayHandler.requestSnapshot();

        this.addRenderableWidget(Button.builder(
                        Component.translatable("rsl.menu.set_delay"), button -> applyDelay())
                .bounds(setDelayX, row3, setDelayWidth, BUTTON_H).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("rsl.menu.status"), button -> sendAction(GameplayActionNetwork.ACTION_STATUS))
                .bounds(statusX, row3, statusWidth, BUTTON_H).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("rsl.menu.close"), button -> this.onClose())
                .bounds(closeX, row3, closeWidth, BUTTON_H).build());

        ClientGameplayActionHandler.setHandler(lines -> this.resultLines = lines);
        this.resultLines = ClientGameplayActionHandler.getLastLines();
        ClientGameplayActionHandler.request(GameplayActionNetwork.ACTION_STATUS, 0);
    }

    private Button primary(String key, ChatFormatting color, int x, int y, Runnable action) {
        return Button.builder(Component.translatable(key).withStyle(color), button -> action.run())
                .bounds(x, y, buttonWidth, BUTTON_H).build();
    }

    private void sendAction(String action) {
        this.statusText = Component.translatable("rsl.menu.status_hint");
        ClientGameplayActionHandler.request(action, 0);
    }

    private void applyDelay() {
        try {
            int seconds = Integer.parseInt(this.delayField.getValue().trim());
            if (seconds < 1 || seconds > 3600) {
                throw new NumberFormatException("range");
            }
            this.statusText = Component.translatable("rsl.menu.status_hint");
            ClientGameplayActionHandler.request(GameplayActionNetwork.ACTION_DELAY, seconds);
            ClientGameplayHandler.requestSnapshot();
        } catch (NumberFormatException exception) {
            this.statusText = Component.translatable("rsl.menu.invalid_delay").withStyle(ChatFormatting.RED);
        }
    }

    @Override
    public void onClose() {
        ClientGameplayHandler.setHandler(null);
        ClientGameplayActionHandler.setHandler(null);
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE6101018);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 3, 0xFF4A90D9);
        int border = 0xFF3A3A4A;
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, border);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, border);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, border);
        graphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, border);

        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, panelY + 16, 0xFFFFD166);
        graphics.drawCenteredString(this.font,
                Component.translatable("rsl.menu.subtitle"), this.width / 2, panelY + 36, 0xFFA0A0B0);
        graphics.drawCenteredString(this.font,
                trim(this.statusText, panelWidth - 36), this.width / 2, panelY + 62, 0xFFD0D0E0);
        graphics.drawCenteredString(this.font,
                Component.translatable("rsl.menu.hint"), this.width / 2, panelY + panelHeight - 18, 0xFF808090);

        int maxLines = Math.max(1, Math.min(RESULT_MAX_LINES,
                (this.panelHeight - RESULT_TOP - 14) / RESULT_LINE_HEIGHT));
        int count = Math.min(this.resultLines.size(), maxLines);
        for (int i = 0; i < count; i++) {
            graphics.drawCenteredString(this.font, trim(this.resultLines.get(i), panelWidth - 32),
                    this.width / 2, panelY + RESULT_TOP + i * RESULT_LINE_HEIGHT, 0xFFD0D0E0);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private Component trim(Component text, int maxWidth) {
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), maxWidth)).withStyle(text.getStyle());
    }
}
