package yifei.rsl.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yifei.rsl.common.wand.WandAction;
import yifei.rsl.common.wand.WandResult;
import yifei.rsl.common.wand.WandScreenData;
import yifei.rsl.network.WandNetwork;

/**
 * Create / edit / confirm screen opened by the selection wand.
 */
public class AreaWandScreen extends Screen {
    private final WandScreenData data;
    private EditBox nameField;
    private Button primaryButton;
    private Button reselectButton;
    private Button deleteButton;
    private String error = "";
    private boolean pending;
    private boolean completed;

    public AreaWandScreen(WandScreenData data) {
        super(titleFor(data));
        this.data = data;
    }

    private static Component titleFor(WandScreenData data) {
        return switch (data.mode) {
            case EDIT -> Component.translatable("rsl.wand.title.edit");
            case CONFIRM_EDIT -> Component.translatable("rsl.wand.title.confirm");
            default -> Component.translatable("rsl.wand.title.create");
        };
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 110;
        int top = this.height / 2 - 64;

        nameField = new EditBox(this.font, left, top + 40, 220, 20, Component.translatable("rsl.wand.name"));
        nameField.setMaxLength(32);
        nameField.setValue(data.proposedName);
        addRenderableWidget(nameField);

        if (data.mode == WandScreenData.Mode.EDIT) {
            primaryButton = addRenderableWidget(Button.builder(Component.translatable("rsl.wand.save_name"),
                    b -> submitRename()).bounds(left, top + 76, 220, 20).build());
            reselectButton = addRenderableWidget(Button.builder(Component.translatable("rsl.wand.reselect"),
                    b -> submitReselect()).bounds(left, top + 102, 106, 20).build());
            deleteButton = addRenderableWidget(Button.builder(Component.translatable("rsl.wand.delete"),
                    b -> submitDelete()).bounds(left + 114, top + 102, 106, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("rsl.wand.cancel"), b -> onClose())
                    .bounds(left, top + 128, 220, 20).build());
        } else if (data.mode == WandScreenData.Mode.CONFIRM_EDIT) {
            primaryButton = addRenderableWidget(Button.builder(Component.translatable("rsl.wand.save"),
                    b -> submitSaveEdit()).bounds(left, top + 76, 220, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("rsl.wand.cancel"), b -> onClose())
                    .bounds(left, top + 102, 220, 20).build());
        } else {
            primaryButton = addRenderableWidget(Button.builder(Component.translatable("rsl.wand.add"),
                    b -> submitAdd()).bounds(left, top + 76, 220, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("rsl.wand.cancel"), b -> onClose())
                    .bounds(left, top + 102, 220, 20).build());
        }
    }

    private String readName() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) {
            error = Component.translatable("rsl.message.wand_invalid_name").getString();
            return null;
        }
        return name;
    }

    private void submitAdd() {
        String name = readName();
        if (name != null) {
            send(WandAction.add(name));
        }
    }

    private void submitRename() {
        String name = readName();
        if (name != null) {
            send(WandAction.rename(data.originalName, name));
        }
    }

    private void submitSaveEdit() {
        String name = readName();
        if (name != null) {
            send(WandAction.saveEdit(data.originalName, name));
        }
    }

    private void submitReselect() {
        send(WandAction.reselect(data.originalName, data.proposedName));
    }

    private void submitDelete() {
        send(WandAction.delete(data.originalName));
    }

    private void send(WandAction action) {
        if (pending) {
            return;
        }
        pending = true;
        setButtons(false);
        WandNetwork.sendActionToServer(action);
    }

    public void handleResult(WandResult result) {
        if (result.success) {
            completed = true;
            onClose();
            return;
        }
        pending = false;
        error = Component.translatable(result.messageKey, result.detail).getString();
        setButtons(true);
    }

    private void setButtons(boolean active) {
        if (primaryButton != null) {
            primaryButton.active = active;
        }
        if (reselectButton != null) {
            reselectButton.active = active;
        }
        if (deleteButton != null) {
            deleteButton.active = active;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        if (data.originalName != null) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("rsl.wand.target", data.originalName),
                    this.width / 2, 46, 0xFFC060);
        }
        graphics.drawCenteredString(this.font,
                Component.translatable("rsl.wand.selection",
                        data.x1, data.y1, data.z1, data.x2, data.y2, data.z2),
                this.width / 2, 60, 0xA0A0A0);
        graphics.drawCenteredString(this.font,
                Component.translatable("rsl.wand.volume", data.volume), this.width / 2, 72, 0xA0A0A0);
        if (data.mode == WandScreenData.Mode.EDIT) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("rsl.wand.edit_hint"), this.width / 2, 86, 0x808080);
        } else if (data.mode == WandScreenData.Mode.CONFIRM_EDIT) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("rsl.wand.confirm_hint"), this.width / 2, 86, 0x808080);
        }
        if (!error.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal(error),
                    this.width / 2, this.height / 2 + 58, 0xFF5555);
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (!completed && !pending) {
            WandNetwork.sendActionToServer(WandAction.cancel());
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
