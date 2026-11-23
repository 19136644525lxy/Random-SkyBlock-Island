package yifei.rsl.common.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolLayoutTest {
    @Test
    void wideLayoutUsesMoreColumnsAndRows() {
        PoolLayout layout = PoolLayout.compute(1920, 1080);

        assertTrue(layout.columns > 12);
        assertTrue(layout.rows > 5);
        assertTrue(layout.pageSize > 60);
        assertFalse(layout.twoFooterRows);
        assertGridInsideScreen(layout);
    }

    @Test
    void standardLayoutFillsAvailableArea() {
        PoolLayout layout = PoolLayout.compute(854, 480);

        assertTrue(layout.pageSize >= 100);
        assertTrue(layout.gridWidth >= layout.width / 2);
        assertTrue(layout.gridHeight >= 100);
        assertGridInsideScreen(layout);
    }

    @Test
    void smallWindowUsesTwoFooterRows() {
        PoolLayout layout = PoolLayout.compute(320, 240);

        assertTrue(layout.twoFooterRows);
        assertTrue(layout.pageSize > 0);
        assertGridInsideScreen(layout);
        assertTrue(layout.pageY < layout.actionY);
    }

    @Test
    void verticalSectionsDoNotOverlap() {
        int[][] sizes = {{320, 240}, {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}};
        for (int[] size : sizes) {
            PoolLayout layout = PoolLayout.compute(size[0], size[1]);
            int searchBottom = PoolLayout.SEARCH_Y + PoolLayout.SEARCH_HEIGHT;
            assertTrue(searchBottom <= PoolLayout.STATUS_Y, "search/status " + size[0] + "x" + size[1]);
            assertTrue(PoolLayout.STATUS_Y + 9 <= PoolLayout.MESSAGE_Y, "status/message " + size[0] + "x" + size[1]);
            assertTrue(PoolLayout.MESSAGE_Y + 9 <= layout.gridTop, "message/grid " + size[0] + "x" + size[1]);
            assertTrue(layout.gridTop + layout.gridHeight <= layout.pageY, "grid/page " + size[0] + "x" + size[1]);
        }
    }

    @Test
    void pageSizeMatchesGridView() {
        PoolLayout layout = PoolLayout.compute(1280, 720);
        assertEquals(layout.columns * layout.rows, layout.pageSize);
    }

    private static void assertGridInsideScreen(PoolLayout layout) {
        assertTrue(layout.gridLeft >= 0);
        assertTrue(layout.gridTop >= 0);
        assertTrue(layout.gridLeft + layout.gridWidth <= layout.width);
        assertTrue(layout.gridTop + layout.gridHeight <= layout.height);
        assertTrue(layout.columns >= 1);
        assertTrue(layout.rows >= 1);
    }
}
