package yifei.rsl.common.ui;

/**
 * Shared, platform-neutral layout for the block-pool editor.
 *
 * <p>The grid uses the available width and height instead of a fixed
 * 12x5 rectangle. Pages are used rather than a scroll row so every page fills
 * the screen and the page indicator stays meaningful.</p>
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
    public final int actionY;
    public final int actionStartX;
    public final int actionButtonWidth;

    private PoolLayout(int width, int height, int searchX, int searchWidth,
                       int columns, int rows, int cell, int gridLeft, int gridTop,
                       int gridWidth, int gridHeight, boolean twoFooterRows,
                       int pageY, int pagePrevX, int pageNextX, int pageLabelX,
                       int actionY, int actionStartX, int actionButtonWidth) {
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
        this.actionY = actionY;
        this.actionStartX = actionStartX;
        this.actionButtonWidth = actionButtonWidth;
    }

    public static PoolLayout compute(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);

        boolean twoRows = w < 700;
        int footerHeight = twoRows ? 56 : 34;
        int gridBottom = h - footerHeight;

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

        int searchWidth = Math.min(480, availableWidth);
        int searchX = (w - searchWidth) / 2;

        int actionGap = 6;
        int actionButtonWidth = Math.min(120,
                Math.max(60, (w - PADDING * 2 - actionGap * 2) / 3));
        int actionsWidth = actionButtonWidth * 3 + actionGap * 2;

        int pageY;
        int pagePrevX;
        int pageNextX;
        int pageLabelX;
        int actionY;
        int actionStartX;
        if (twoRows) {
            pageY = h - 46;
            pagePrevX = w / 2 - 70;
            pageNextX = w / 2 + 50;
            pageLabelX = w / 2;
            actionY = h - 24;
            actionStartX = Math.max(PADDING, (w - actionsWidth) / 2);
        } else {
            pageY = h - 24;
            pagePrevX = PADDING;
            pageNextX = PADDING + 126;
            pageLabelX = PADDING + 30;
            actionY = h - 24;
            actionStartX = Math.max(PADDING, w - PADDING - actionsWidth);
        }

        return new PoolLayout(w, h, searchX, searchWidth, columns, rows, cell,
                gridLeft, gridTop, gridWidth, gridHeight, twoRows,
                pageY, pagePrevX, pageNextX, pageLabelX,
                actionY, actionStartX, actionButtonWidth);
    }

    private static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }
}
