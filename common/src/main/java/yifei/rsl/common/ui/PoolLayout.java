package yifei.rsl.common.ui;

/**
 * Shared, platform-neutral layout for the block-pool editor.
 *
 * <p>The grid uses the available width and height instead of a fixed
 * 12x5 rectangle. Pages are used rather than a scroll row so every page fills
 * the screen and the page indicator stays meaningful. On wide screens the
 * seven block-pool actions share a single row so the footer stays short and
 * the grid keeps more room; very narrow windows fall back to two rows because
 * seven buttons would not fit.</p>
 */
public final class PoolLayout {
    public static final int PADDING = 12;
    public static final int GAP = 4;
    public static final int TITLE_Y = 8;
    public static final int SEARCH_Y = 24;
    public static final int SEARCH_HEIGHT = 20;
    public static final int STATUS_Y = 52;
    public static final int MESSAGE_Y = 64;
    public static final int GRID_TOP = 78;
    public static final int ACTION_HEIGHT = 20;
    public static final int ACTION_GAP = 6;
    public static final int ACTION_ROW_GAP = 4;
    public static final int PAGE_HEIGHT = 20;
    public static final int PAGE_BUTTON_WIDTH = 24;
    public static final int CLOSE_SIZE = 20;
    public static final int CLOSE_Y = 2;

    public final int width;
    public final int height;
    public final int searchX;
    public final int searchWidth;
    public final int columns;
    public final int rows;
    public final int cell;
    public final int gridLeft;
    public final int gridTop;
    public final int gridWidth;
    public final int gridHeight;
    public final int pageSize;
    public final boolean twoFooterRows;
    public final int pageY;
    public final int pagePrevX;
    public final int pageNextX;
    public final int pageLabelX;
    public final int closeX;
    public final int closeY;
    public final int closeSize;
    public final int actionY;
    public final int actionStartX;
    public final int actionButtonWidth;
    public final int action2Y;
    public final int action2StartX;
    public final int action2ButtonWidth;

    private PoolLayout(int width, int height, int searchX, int searchWidth,
                       int columns, int rows, int cell, int gridLeft, int gridTop,
                       int gridWidth, int gridHeight, boolean twoFooterRows,
                       int pageY, int pagePrevX, int pageNextX, int pageLabelX,
                       int closeX, int closeY, int closeSize,
                       int actionY, int actionStartX, int actionButtonWidth,
                       int action2Y, int action2StartX, int action2ButtonWidth) {
        this.width = width;
        this.height = height;
        this.searchX = searchX;
        this.searchWidth = searchWidth;
        this.columns = columns;
        this.rows = rows;
        this.cell = cell;
        this.gridLeft = gridLeft;
        this.gridTop = gridTop;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.pageSize = columns * rows;
        this.twoFooterRows = twoFooterRows;
        this.pageY = pageY;
        this.pagePrevX = pagePrevX;
        this.pageNextX = pageNextX;
        this.pageLabelX = pageLabelX;
        this.closeX = closeX;
        this.closeY = closeY;
        this.closeSize = closeSize;
        this.actionY = actionY;
        this.actionStartX = actionStartX;
        this.actionButtonWidth = actionButtonWidth;
        this.action2Y = action2Y;
        this.action2StartX = action2StartX;
        this.action2ButtonWidth = action2ButtonWidth;
    }

    public static PoolLayout compute(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);

        boolean twoRows = w < 700;
        int pageY = h - 24;
        int actionY;
        int action2Y;
        int gridBottom;
        if (twoRows) {
            action2Y = pageY - ACTION_ROW_GAP - ACTION_HEIGHT;
            actionY = action2Y - ACTION_ROW_GAP - ACTION_HEIGHT;
            gridBottom = actionY - 14;
        } else {
            // All seven actions share one centered row, so the footer shrinks
            // and the grid gains the freed vertical space.
            actionY = pageY - ACTION_ROW_GAP - ACTION_HEIGHT;
            action2Y = actionY;
            gridBottom = actionY - 14;
        }

        int availableWidth = Math.max(1, w - PADDING * 2);
        int availableHeight = Math.max(1, gridBottom - GRID_TOP);

        int cell = clamp(20, 30,
                Math.min(availableWidth / 20, availableHeight / 10));
        int columns = Math.max(1, (availableWidth + GAP) / (cell + GAP));
        int rows = Math.max(1, (availableHeight + GAP) / (cell + GAP));
        int gridWidth = columns * cell + (columns - 1) * GAP;
        int gridHeight = rows * cell + (rows - 1) * GAP;
        int gridLeft = Math.max(PADDING, (w - gridWidth) / 2);
        int gridTop = GRID_TOP + Math.max(0, (availableHeight - gridHeight) / 2);

        int searchWidth = Math.min(640, availableWidth);
        int searchX = (w - searchWidth) / 2;

        // Keep the arrows close to the centered page label instead of pinning
        // them to the far left. A 24px arrow is easier to hit than the old 20px
        // button and leaves room for the disabled state at page boundaries.
        int pageLabelX = w / 2;
        int pagePrevX = Math.max(PADDING, pageLabelX - 86);
        int pageNextX = Math.min(w - PADDING - PAGE_BUTTON_WIDTH, pageLabelX + 62);

        // Wide screens fit all seven actions in one centered row. Narrow
        // windows keep two rows because seven 56px buttons would not fit.
        int actionButtonWidth;
        int actionsWidth;
        int action2ButtonWidth;
        int actions2Width;
        int actionStartX;
        int action2StartX;
        if (twoRows) {
            actionButtonWidth = Math.min(110,
                    Math.max(56, (w - PADDING * 2 - ACTION_GAP * 3) / 4));
            actionsWidth = actionButtonWidth * 4 + ACTION_GAP * 3;
            action2ButtonWidth = Math.min(110,
                    Math.max(56, (w - PADDING * 2 - ACTION_GAP * 2) / 3));
            actions2Width = action2ButtonWidth * 3 + ACTION_GAP * 2;
            actionStartX = Math.max(PADDING, (w - actionsWidth) / 2);
            action2StartX = Math.max(PADDING, (w - actions2Width) / 2);
        } else {
            actionButtonWidth = Math.min(110,
                    Math.max(56, (w - PADDING * 2 - ACTION_GAP * 6) / 7));
            actionsWidth = actionButtonWidth * 7 + ACTION_GAP * 6;
            action2ButtonWidth = actionButtonWidth;
            actions2Width = actionsWidth;
            actionStartX = Math.max(PADDING, (w - actionsWidth) / 2);
            // The secondary actions are slots 5-7 of the same single row.
            action2StartX = actionStartX + (actionButtonWidth + ACTION_GAP) * 4;
        }
        int closeX = Math.max(PADDING, w - PADDING - CLOSE_SIZE);
        int closeY = CLOSE_Y;
        int closeSize = CLOSE_SIZE;

        return new PoolLayout(w, h, searchX, searchWidth, columns, rows, cell,
                gridLeft, gridTop, gridWidth, gridHeight, twoRows,
                pageY, pagePrevX, pageNextX, pageLabelX,
                closeX, closeY, closeSize,
                actionY, actionStartX, actionButtonWidth,
                action2Y, action2StartX, action2ButtonWidth);
    }

    private static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }
}
