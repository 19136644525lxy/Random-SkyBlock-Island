package yifei.rsl.client.gui;

import yifei.rsl.client.ClientSounds;
import net.minecraft.sound.SoundEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dedicated 3D preview for the egg island volume: the actual block models are
 * rendered with the game's block renderer, so the preview shows real textures.
 * Drag to rotate, scroll to zoom. The panel grows with the window so the cube
 * keeps a comfortable margin. The whole island is drawn with one block chosen
 * from the egg pool, and that choice changes every ten seconds, mirroring the
 * spawn logic.
 */
public final class EggPreviewScreen extends Screen {
    private boolean openedSound;
    private final EggPoolScreen parent;
    private final int volume;
    private final List<BlockState> states;
    private BlockState displayState;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private float yaw = -0.62f;
    private float pitch = 0.52f;
    private float zoom = 1.0f;
    private long lastDragAt;
    private long lastRotateAt = System.currentTimeMillis();

    public EggPreviewScreen(EggPoolScreen parent, int volume, List<String> poolIds) {
        super(Text.translatable("rsl.egg.preview_title"));
        this.parent = parent;
        this.volume = Math.max(1, volume);
        List<BlockState> resolved = new ArrayList<>();
        for (String id : poolIds) {
            Identifier identifier = Identifier.tryParse(id);
            Block block = identifier == null ? null : Registries.BLOCK.get(identifier);
            if (block != null && !block.getDefaultState().isAir()) {
                resolved.add(block.getDefaultState());
            }
        }
        if (resolved.isEmpty()) {
            resolved.add(Blocks.DIAMOND_BLOCK.getDefaultState());
        }
        this.states = List.copyOf(resolved);
        this.displayState = this.states.get(ThreadLocalRandom.current().nextInt(this.states.size()));
    }

    private int edge() {
        for (int value = 1; value <= 64; value++) {
            if (value * value * value >= this.volume) {
                return value;
            }
        }
        return 64;
    }

    private boolean capped() {
        return this.edge() > 4;
    }

    private int renderEdge() {
        return Math.min(this.edge(), 4);
    }

    private int remainingSeconds() {
        long elapsed = System.currentTimeMillis() - this.lastRotateAt;
        int seconds = (int) Math.ceil((10_000L - elapsed) / 1000.0);
        return Math.max(0, Math.min(10, seconds));
    }

    @Override
    protected void init() {
        if (!openedSound) {
            openedSound = true;
            ClientSounds.play(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.4F, 1.0F);
        }
        this.panelW = Math.max(360, this.width - 24);
        this.panelH = Math.max(300, this.height - 24);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("rsl.egg.preview_back"), button -> this.close())
                .dimensions(this.panelX + this.panelW / 2 - 50, this.panelY + this.panelH - 34, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (System.currentTimeMillis() - this.lastDragAt > 2500L) {
            this.yaw += delta * 0.12f;
        }
        if (this.states.size() > 1 && System.currentTimeMillis() - this.lastRotateAt >= 10_000L) {
            this.lastRotateAt = System.currentTimeMillis();
            this.displayState = this.states.get(ThreadLocalRandom.current().nextInt(this.states.size()));
        }
        this.renderBackground(context);
        context.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, 0xEE101418);
        context.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 2, 0xFF66E0FF);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.panelX + this.panelW / 2, this.panelY + 12, 0xFFFFD166);

        int edge = this.edge();
        int shown = this.renderEdge();
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("rsl.egg.preview_size", edge, edge, edge, edge * edge * edge),
                this.panelX + this.panelW / 2, this.panelY + 26, 0xFFA0A0A0);
        if (this.capped()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("rsl.egg.preview_limited", shown, shown, shown),
                    this.panelX + this.panelW / 2, this.panelY + 38, 0xFFB07A30);
        }

        this.renderCube(context, shown, this.panelX + this.panelW / 2, this.panelY + this.panelH / 2 + 6);

        if (this.states.size() > 1) {
            Text text = Text.translatable("rsl.egg.preview_rotate").append(Text.literal(" · "))
                    .append(Text.translatable("rsl.egg.preview_next", this.remainingSeconds()));
            context.drawCenteredTextWithShadow(this.textRenderer, text,
                    this.panelX + this.panelW / 2, this.panelY + this.panelH - 62, 0xFF9A9AC0);
        }
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("rsl.egg.preview_hint"),
                this.panelX + this.panelW / 2, this.panelY + this.panelH - 50, 0xFF8080A0);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCube(DrawContext context, int edge, int centerX, int centerY) {
        float scale = Math.min((this.panelW - 56) / (edge * 2.2f), (this.panelH - 150) / (edge * 1.55f))
                * this.zoom;
        float half = edge / 2.0f;
        float cy = (float) Math.cos(this.yaw);
        float sy = (float) Math.sin(this.yaw);
        float cp = (float) Math.cos(this.pitch);
        float sp = (float) Math.sin(this.pitch);

        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < edge; i++) {
            for (int j = 0; j < edge; j++) {
                for (int k = 0; k < edge; k++) {
                    float lx = i + 0.5f - half;
                    float ly = j + 0.5f - half;
                    float lz = k + 0.5f - half;
                    float z1 = lx * sy + lz * cy;
                    float z2 = ly * sp + z1 * cp;
                    cells.add(new Cell(i, j, k, z2));
                }
            }
        }
        cells.sort(Comparator.comparingDouble(Cell::depth));

        MatrixStack matrices = context.getMatrices();
        BlockRenderManager renderer = MinecraftClient.getInstance().getBlockRenderManager();
        VertexConsumerProvider.Immediate consumers =
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        for (Cell cell : cells) {
            BlockState state = this.displayState;
            matrices.push();
            matrices.translate(centerX, centerY, 0.0);
            matrices.scale(scale, -scale, scale);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(this.yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(this.pitch));
            matrices.translate(cell.i() - half - 0.02, cell.j() - half - 0.02, cell.k() - half - 0.02);
            matrices.scale(1.04f, 1.04f, 1.04f);
            renderer.renderBlockAsEntity(state, matrices, consumers,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
        consumers.draw();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            this.yaw += (float) deltaX * 0.006f;
            this.pitch = Math.max(-1.2f, Math.min(1.2f, this.pitch + (float) deltaY * 0.005f));
            this.lastDragAt = System.currentTimeMillis();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.zoom = Math.max(0.6f, Math.min(1.8f, this.zoom + (float) amount * 0.12f));
        return true;
    }

    @Override
    public void close() {
        ClientSounds.play(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.4F, 0.85F);
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Cell(int i, int j, int k, float depth) {
    }
}
