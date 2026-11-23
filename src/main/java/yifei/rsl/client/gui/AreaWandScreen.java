package yifei.rsl.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import yifei.rsl.client.WandClientNetwork;
import yifei.rsl.common.wand.WandAction;
import yifei.rsl.common.wand.WandResult;
import yifei.rsl.common.wand.WandScreenData;

/**
 * Create / edit / confirm screen opened by the selection wand.
 */
public class AreaWandScreen extends Screen {
    private final WandScreenData data;
    private TextFieldWidget nameField;
    private ButtonWidget primaryButton;
    private ButtonWidget reselectButton;
    private ButtonWidget deleteButton;
    private String error = "";
    private boolean pending;
    private boolean completed;

    public AreaWandScreen(WandScreenData data) {
        super(titleFor(data));
        this.data = data;
    }

    private static Text titleFor(WandScreenData data) {
        return switch (data.mode) {
            case EDIT -> Text.translatable("rsl.wand.title.edit");
            case CONFIRM_EDIT -> Text.translatable("rsl.wand.title.confirm");
            default -> Text.translatable("rsl.wand.title.create");
        };
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 110;
        int top = this.height / 2 - 64;

        nameField = new TextFieldWidget(this.textRenderer, left, top + 40, 220, 20,
                Text.translatable("rsl.wand.name"));
        nameField.setMaxLength(32);
        nameField.setText(data.proposedName);
        addDrawableChild(nameField);

        if (data.mode == WandScreenData.Mode.EDIT) {
            primaryButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.wand.save_name"),
                    b -> submitRename()).dimensions(left, top + 76, 220, 20).build());
            reselectButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.wand.reselect"),
                    b -> submitReselect()).dimensions(left, top + 102, 106, 20).build());
            deleteButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.wand.delete"),
                    b -> submitDelete()).dimensions(left + 114, top + 102, 106, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.wand.cancel"), b -> close())
                    .dimensions(left, top + 128, 220, 20).build());
        } else if (data.mode == WandScreenData.Mode.CONFIRM_EDIT) {
            primaryButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.wand.save"),
                    b -> submitSaveEdit()).dimensions(left, top + 76, 220, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.wand.cancel"), b -> close())
                    .dimensions(left, top + 102, 220, 20).build());
        } else {
            primaryButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.wand.add"),
                    b -> submitAdd()).dimensions(left, top + 76, 220, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.wand.cancel"), b -> close())
                    .dimensions(left, top + 102, 220, 20).build());
        }
    }

    private String readName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            error = Text.translatable("rsl.message.wand_invalid_name").getString();
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
        WandClientNetwork.sendAction(action);
    }

    public void handleResult(WandResult result) {
        if (result.success) {
            completed = true;
            close();
            return;
        }
        pending = false;
        error = Text.translatable(result.messageKey, result.detail).getString();
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 30, 0xFFFFFF);
        if (data.originalName != null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("rsl.wand.target", data.originalName),
                    this.width / 2, 46, 0xFFC060);
        }
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("rsl.wand.selection",
                        data.x1, data.y1, data.z1, data.x2, data.y2, data.z2),
                this.width / 2, 60, 0xA0A0A0);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("rsl.wand.volume", data.volume), this.width / 2, 72, 0xA0A0A0);
        if (data.mode == WandScreenData.Mode.EDIT) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("rsl.wand.edit_hint"), this.width / 2, 86, 0x808080);
        } else if (data.mode == WandScreenData.Mode.CONFIRM_EDIT) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("rsl.wand.confirm_hint"), this.width / 2, 86, 0x808080);
        }
        if (!error.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(error),
                    this.width / 2, this.height / 2 + 58, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (!completed && !pending) {
            WandClientNetwork.sendAction(WandAction.cancel());
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
