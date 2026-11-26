package yifei.rsl.client.gui;

import yifei.rsl.client.ClientSounds;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import yifei.rsl.common.config.PoolAccessPlayer;
import yifei.rsl.common.config.PoolAccessPlayers;
import yifei.rsl.common.config.PoolAccessResult;
import yifei.rsl.network.AccessClientNetwork;
import yifei.rsl.network.AccessNetwork;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Block-pool access management screen. Shows every online player with their
 * current role, lets the operator select several rows and then asks for a second
 * confirmation before the bulk grant/revoke reaches the server.
 */
public final class PoolAccessScreen extends Screen {
    private boolean openedSound;
    private static final int PANEL_W = 460;
    private static final int PANEL_H = 320;
    private static final int BUTTON_H = 20;
    private static final int GAP = 8;
    private static final int ROW_H = 22;
    private static final int PAGE_SIZE = 8;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private ButtonWidget grantEditorButton;
    private ButtonWidget grantOwnerButton;
    private ButtonWidget revokeButton;
    private ButtonWidget backButton;
    private ButtonWidget confirmButton;
    private ButtonWidget cancelButton;

    private List<PoolAccessPlayer> players = List.of();
    private boolean canManage;
    private boolean loaded;
    private final Set<String> selected = new HashSet<>();
    private int page;
    private String confirmAction;
    private boolean pending;
    private String statusMessage = "";
    private int statusColor = 0xFFD0D0E0;

    public PoolAccessScreen() {
        super(Text.translatable("rsl.access.title"));
        AccessClientNetwork.setHandlers(this::onPlayers, this::onResult);
    }

    @Override
    protected void init() {
        if (!openedSound) {
            openedSound = true;
            ClientSounds.play(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.4F, 1.0F);
        }
        panelWidth = Math.min(PANEL_W, width - 16);
        panelHeight = Math.min(PANEL_H, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        rowButtons.clear();
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int index = i;
            rowButtons.add(addDrawableChild(ButtonWidget.builder(Text.empty(), b -> toggle(index))
                    .dimensions(panelX + 12, panelY + 66 + i * ROW_H, panelWidth - 24, BUTTON_H)
                    .build()));
        }

        int navY = panelY + panelHeight - 70;
        prevButton = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> changePage(-1))
                .dimensions(panelX + 12, navY, 24, BUTTON_H).build());
        nextButton = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> changePage(1))
                .dimensions(panelX + panelWidth - 36, navY, 24, BUTTON_H).build());

        int actionY = panelY + panelHeight - 42;
        int actionWidth = Math.max(52, (panelWidth - 24 - GAP * 3) / 4);
        int actionStartX = panelX + (panelWidth - (actionWidth * 4 + GAP * 3)) / 2;
        grantEditorButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.access.grant"),
                b -> askConfirm(AccessNetwork.GRANT_EDITOR))
                .dimensions(actionStartX, actionY, actionWidth, BUTTON_H).build());
        grantOwnerButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.access.grant_owner"),
                b -> askConfirm(AccessNetwork.GRANT_OWNER))
                .dimensions(actionStartX + actionWidth + GAP, actionY, actionWidth, BUTTON_H).build());
        revokeButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.access.revoke"),
                b -> askConfirm(AccessNetwork.REVOKE))
                .dimensions(actionStartX + (actionWidth + GAP) * 2, actionY, actionWidth, BUTTON_H).build());
        backButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.access.back"), b -> close())
                .dimensions(actionStartX + (actionWidth + GAP) * 3, actionY, actionWidth, BUTTON_H).build());

        int confirmWidth = Math.min(132, (panelWidth - 24 - GAP) / 2);
        int confirmStartX = panelX + (panelWidth - (confirmWidth * 2 + GAP)) / 2;
        confirmButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.access.confirm"), b -> confirm())
                .dimensions(confirmStartX, actionY, confirmWidth, BUTTON_H).build());
        cancelButton = addDrawableChild(ButtonWidget.builder(Text.translatable("rsl.access.cancel"), b -> cancelConfirm())
                .dimensions(confirmStartX + confirmWidth + GAP, actionY, confirmWidth, BUTTON_H).build());

        refreshRows();
        updateControls();
        AccessClientNetwork.requestPlayers();
    }

    private void onPlayers(PoolAccessPlayers payload) {
        canManage = payload.canManage;
        players = payload.players;
        loaded = true;
        selected.removeIf(uuid -> players.stream().noneMatch(row -> row.uuid.equals(uuid)));
        if (page > pageCount() - 1) {
            page = Math.max(0, pageCount() - 1);
        }
        refreshRows();
        updateControls();
        if (!canManage && statusMessage.isEmpty()) {
            setStatus(Text.translatable("rsl.access.no_manage").getString(), 0xFF5555);
        }
    }

    private void onResult(PoolAccessResult result) {
        pending = false;
        confirmAction = null;
        if (result.success) {
            setStatus(Text.translatable(result.messageKey, result.changed, result.skipped).getString(),
                    0x55FF55);
        } else {
            String message = Text.translatable(result.messageKey).getString();
            if (!result.failedNames.isEmpty()) {
                message += " " + String.join(", ", result.failedNames);
            }
            setStatus(message, 0xFF5555);
        }
        updateControls();
        AccessClientNetwork.requestPlayers();
    }

    private void toggle(int index) {
        int absolute = page * PAGE_SIZE + index;
        if (absolute >= players.size() || !canManage || pending || confirmAction != null) {
            return;
        }
        String uuid = players.get(absolute).uuid;
        if (!selected.remove(uuid)) {
            selected.add(uuid);
        }
        refreshRows();
        updateControls();
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(pageCount() - 1, page + delta));
        refreshRows();
        updateControls();
    }

    private void askConfirm(String action) {
        if (!canManage || pending) {
            return;
        }
        if (selected.isEmpty()) {
            setStatus(Text.translatable("rsl.access.none_selected").getString(), 0xFFAA55);
            return;
        }
        confirmAction = action;
        statusMessage = "";
        updateControls();
    }

    private void confirm() {
        if (confirmAction == null || pending || selected.isEmpty()) {
            return;
        }
        pending = true;
        AccessClientNetwork.sendAction(confirmAction, new ArrayList<>(selected));
        setStatus(Text.translatable("rsl.access.pending").getString(), 0xFFAA55);
        updateControls();
    }

    private void cancelConfirm() {
        confirmAction = null;
        statusMessage = "";
        updateControls();
    }

    private void refreshRows() {
        int start = page * PAGE_SIZE;
        for (int i = 0; i < rowButtons.size(); i++) {
            int index = start + i;
            ButtonWidget button = rowButtons.get(i);
            if (index >= players.size()) {
                button.visible = false;
                continue;
            }
            PoolAccessPlayer row = players.get(index);
            button.visible = true;
            button.setMessage(Text.literal(rowLabel(row)));
        }
    }

    private String rowLabel(PoolAccessPlayer row) {
        String role = switch (row.role) {
            case "OWNER" -> " " + Text.translatable("rsl.access.owner").getString();
            case "EDITOR" -> " " + Text.translatable("rsl.access.editor").getString();
            default -> "";
        };
        String mark = selected.contains(row.uuid) ? "✓ " : "";
        return mark + row.name + role;
    }

    private void updateControls() {
        boolean base = canManage && loaded && !pending;
        boolean choosing = base && confirmAction != null;
        for (ButtonWidget button : rowButtons) {
            button.active = base && !choosing;
        }
        grantEditorButton.visible = !choosing;
        grantOwnerButton.visible = !choosing;
        revokeButton.visible = !choosing;
        backButton.visible = !choosing;
        confirmButton.visible = choosing;
        cancelButton.visible = choosing;
        grantEditorButton.active = base;
        grantOwnerButton.active = base;
        revokeButton.active = base;
        backButton.active = !pending;
        prevButton.active = base && page > 0;
        nextButton.active = base && page < pageCount() - 1;
    }

    private int pageCount() {
        return Math.max(1, (players.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void setStatus(String message, int color) {
        statusMessage = message == null ? "" : message;
        statusColor = color;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD101018);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0x33FFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                panelX + panelWidth / 2, panelY + 14, 0xFFFFD166);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("rsl.access.subtitle"),
                panelX + panelWidth / 2, panelY + 34, 0xA0A0A0);

        Text statusLine = null;
        int color = statusColor;
        if (confirmAction != null) {
            String key = switch (confirmAction) {
                case AccessNetwork.GRANT_EDITOR -> "rsl.access.confirm_grant";
                case AccessNetwork.GRANT_OWNER -> "rsl.access.confirm_grant_owner";
                default -> "rsl.access.confirm_revoke";
            };
            statusLine = Text.translatable(key, selected.size());
            color = 0xFFFFD166;
        } else if (!statusMessage.isEmpty()) {
            statusLine = Text.literal(statusMessage);
        } else if (!selected.isEmpty()) {
            statusLine = Text.translatable("rsl.access.selected", selected.size());
        }
        if (statusLine != null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    trim(statusLine, panelWidth - 24),
                    panelX + panelWidth / 2, panelY + 50, color);
        }

        if (!loaded) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("rsl.access.loading"),
                    panelX + panelWidth / 2, panelY + 90, 0xAAAAAA);
        } else if (players.isEmpty() && canManage) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("rsl.access.empty"),
                    panelX + panelWidth / 2, panelY + 90, 0xAAAAAA);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("rsl.access.page", page + 1, pageCount()),
                    panelX + panelWidth / 2, panelY + panelHeight - 65, 0xAAAAAA);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private Text trim(Text text, int maxWidth) {
        return Text.literal(this.textRenderer.trimToWidth(text.getString(), maxWidth));
    }

    @Override
    public void close() {
        ClientSounds.play(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.4F, 0.85F);
        AccessClientNetwork.setHandlers(null, null);
        super.close();
    }
}
