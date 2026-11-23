package yifei.rsl.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yifei.rsl.client.ClientAccessHandler;
import yifei.rsl.common.config.PoolAccessPlayer;
import yifei.rsl.common.config.PoolAccessPlayers;
import yifei.rsl.common.config.PoolAccessResult;
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

    private final List<Button> rowButtons = new ArrayList<>();
    private Button prevButton;
    private Button nextButton;
    private Button grantEditorButton;
    private Button grantOwnerButton;
    private Button revokeButton;
    private Button backButton;
    private Button confirmButton;
    private Button cancelButton;

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
        super(Component.translatable("rsl.access.title"));
        ClientAccessHandler.setHandlers(this::onPlayers, this::onResult);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(PANEL_W, width - 16);
        panelHeight = Math.min(PANEL_H, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        rowButtons.clear();
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int index = i;
            rowButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> toggle(index))
                    .bounds(panelX + 12, panelY + 66 + i * ROW_H, panelWidth - 24, BUTTON_H)
                    .build()));
        }

        int navY = panelY + panelHeight - 70;
        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(panelX + 12, navY, 24, BUTTON_H).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(panelX + panelWidth - 36, navY, 24, BUTTON_H).build());

        int actionY = panelY + panelHeight - 42;
        int actionWidth = Math.max(52, (panelWidth - 24 - GAP * 3) / 4);
        int actionStartX = panelX + (panelWidth - (actionWidth * 4 + GAP * 3)) / 2;
        grantEditorButton = addRenderableWidget(Button.builder(Component.translatable("rsl.access.grant"),
                b -> askConfirm(AccessNetwork.GRANT_EDITOR))
                .bounds(actionStartX, actionY, actionWidth, BUTTON_H).build());
        grantOwnerButton = addRenderableWidget(Button.builder(Component.translatable("rsl.access.grant_owner"),
                b -> askConfirm(AccessNetwork.GRANT_OWNER))
                .bounds(actionStartX + actionWidth + GAP, actionY, actionWidth, BUTTON_H).build());
        revokeButton = addRenderableWidget(Button.builder(Component.translatable("rsl.access.revoke"),
                b -> askConfirm(AccessNetwork.REVOKE))
                .bounds(actionStartX + (actionWidth + GAP) * 2, actionY, actionWidth, BUTTON_H).build());
        backButton = addRenderableWidget(Button.builder(Component.translatable("rsl.access.back"), b -> onClose())
                .bounds(actionStartX + (actionWidth + GAP) * 3, actionY, actionWidth, BUTTON_H).build());

        int confirmWidth = Math.min(132, (panelWidth - 24 - GAP) / 2);
        int confirmStartX = panelX + (panelWidth - (confirmWidth * 2 + GAP)) / 2;
        confirmButton = addRenderableWidget(Button.builder(Component.translatable("rsl.access.confirm"), b -> confirm())
                .bounds(confirmStartX, actionY, confirmWidth, BUTTON_H).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("rsl.access.cancel"), b -> cancelConfirm())
                .bounds(confirmStartX + confirmWidth + GAP, actionY, confirmWidth, BUTTON_H).build());

        refreshRows();
        updateControls();
        ClientAccessHandler.requestPlayers();
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
            setStatus(Component.translatable("rsl.access.no_manage").getString(), 0xFF5555);
        }
    }

    private void onResult(PoolAccessResult result) {
        pending = false;
        confirmAction = null;
        if (result.success) {
            setStatus(Component.translatable(result.messageKey, result.changed, result.skipped).getString(),
                    0x55FF55);
        } else {
            String message = Component.translatable(result.messageKey).getString();
            if (!result.failedNames.isEmpty()) {
                message += " " + String.join(", ", result.failedNames);
            }
            setStatus(message, 0xFF5555);
        }
        updateControls();
        ClientAccessHandler.requestPlayers();
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
            setStatus(Component.translatable("rsl.access.none_selected").getString(), 0xFFAA55);
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
        ClientAccessHandler.sendAction(confirmAction, new ArrayList<>(selected));
        setStatus(Component.translatable("rsl.access.pending").getString(), 0xFFAA55);
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
            Button button = rowButtons.get(i);
            if (index >= players.size()) {
                button.visible = false;
                continue;
            }
            PoolAccessPlayer row = players.get(index);
            button.visible = true;
            button.setMessage(Component.literal(rowLabel(row)));
        }
    }

    private String rowLabel(PoolAccessPlayer row) {
        String role = switch (row.role) {
            case "OWNER" -> " " + Component.translatable("rsl.access.owner").getString();
            case "EDITOR" -> " " + Component.translatable("rsl.access.editor").getString();
            default -> "";
        };
        String mark = selected.contains(row.uuid) ? "✓ " : "";
        return mark + row.name + role;
    }

    private void updateControls() {
        boolean base = canManage && loaded && !pending;
        boolean choosing = base && confirmAction != null;
        for (Button button : rowButtons) {
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD101018);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0x33FFFFFF);
        graphics.drawCenteredString(this.font, this.title,
                panelX + panelWidth / 2, panelY + 14, 0xFFFFD166);
        graphics.drawCenteredString(this.font, Component.translatable("rsl.access.subtitle"),
                panelX + panelWidth / 2, panelY + 34, 0xA0A0A0);

        Component statusLine = null;
        int color = statusColor;
        if (confirmAction != null) {
            String key = switch (confirmAction) {
                case AccessNetwork.GRANT_EDITOR -> "rsl.access.confirm_grant";
                case AccessNetwork.GRANT_OWNER -> "rsl.access.confirm_grant_owner";
                default -> "rsl.access.confirm_revoke";
            };
            statusLine = Component.translatable(key, selected.size());
            color = 0xFFFFD166;
        } else if (!statusMessage.isEmpty()) {
            statusLine = Component.literal(statusMessage);
        } else if (!selected.isEmpty()) {
            statusLine = Component.translatable("rsl.access.selected", selected.size());
        }
        if (statusLine != null) {
            graphics.drawCenteredString(this.font, trim(statusLine, panelWidth - 24),
                    panelX + panelWidth / 2, panelY + 50, color);
        }

        if (!loaded) {
            graphics.drawCenteredString(this.font, Component.translatable("rsl.access.loading"),
                    panelX + panelWidth / 2, panelY + 90, 0xAAAAAA);
        } else if (players.isEmpty() && canManage) {
            graphics.drawCenteredString(this.font, Component.translatable("rsl.access.empty"),
                    panelX + panelWidth / 2, panelY + 90, 0xAAAAAA);
        } else {
            graphics.drawCenteredString(this.font, Component.translatable("rsl.access.page", page + 1, pageCount()),
                    panelX + panelWidth / 2, panelY + panelHeight - 65, 0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component trim(Component text, int maxWidth) {
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), maxWidth));
    }

    @Override
    public void onClose() {
        ClientAccessHandler.setHandlers(null, null);
        super.onClose();
    }
}
