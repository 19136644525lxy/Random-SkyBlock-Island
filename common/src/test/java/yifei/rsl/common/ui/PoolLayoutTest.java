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
    void smallWindowKeepsTwoActionRowsAndPageControlsSeparate() {
        PoolLayout layout = PoolLayout.compute(320, 240);

        assertTrue(layout.twoFooterRows);
        assertTrue(layout.pageSize > 0);
        assertGridInsideScreen(layout);
        assertTrue(layout.actionY < layout.action2Y);
        assertTrue(layout.action2Y < layout.pageY);
    }

    @Test
    void verticalSectionsDoNotOverlap() {
        int[][] sizes = {{320, 240}, {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}};
        for (int[] size : sizes) {
            PoolLayout layout = PoolLayout.compute(size[0], size[1]);
            String name = size[0] + "x" + size[1];
            int searchBottom = PoolLayout.SEARCH_Y + PoolLayout.SEARCH_HEIGHT;
            assertTrue(searchBottom <= PoolLayout.STATUS_Y, "search/status " + name);
            assertTrue(PoolLayout.STATUS_Y + 9 <= PoolLayout.MESSAGE_Y, "status/message " + name);
            assertTrue(PoolLayout.MESSAGE_Y + 9 <= layout.gridTop, "message/grid " + name);
            assertTrue(layout.gridTop + layout.gridHeight <= layout.actionY,
                    "grid/action " + name);
            if (layout.twoFooterRows) {
                assertTrue(layout.actionY < layout.action2Y, "action rows " + name);
                assertTrue(layout.actionY + PoolLayout.ACTION_HEIGHT <= layout.action2Y,
                        "action row gap " + name);
                assertTrue(layout.action2Y + PoolLayout.ACTION_HEIGHT <= layout.pageY,
                        "action2/page " + name);
            } else {
                assertEquals(layout.actionY, layout.action2Y, "single action row " + name);
                assertTrue(layout.actionY + PoolLayout.ACTION_HEIGHT <= layout.pageY,
                        "action/page " + name);
            }
        }
    }

    @Test
    void pageSizeMatchesGridView() {
        PoolLayout layout = PoolLayout.compute(1280, 720);
        assertEquals(layout.columns * layout.rows, layout.pageSize);
    }

    @Test
    void closeButtonAndPageArrowsStayInsideScreen() {
        int[][] sizes = {{320, 240}, {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}};
        for (int[] size : sizes) {
            PoolLayout layout = PoolLayout.compute(size[0], size[1]);
            assertTrue(layout.closeX >= PoolLayout.PADDING,
                    "close left " + size[0] + "x" + size[1]);
            assertTrue(layout.closeX + layout.closeSize <= size[0] - PoolLayout.PADDING,
                    "close right " + size[0] + "x" + size[1]);
            assertTrue(layout.closeY + layout.closeSize <= PoolLayout.SEARCH_Y,
                    "close/search " + size[0] + "x" + size[1]);
            assertTrue(layout.pagePrevX >= PoolLayout.PADDING,
                    "prev left " + size[0] + "x" + size[1]);
            assertTrue(layout.pageNextX + PoolLayout.PAGE_BUTTON_WIDTH
                            <= size[0] - PoolLayout.PADDING,
                    "next right " + size[0] + "x" + size[1]);
            assertTrue(layout.pagePrevX < layout.pageLabelX
                            && layout.pageLabelX < layout.pageNextX,
                    "page order " + size[0] + "x" + size[1]);
        }
    }

    @Test
    void sevenActionButtonsFitInsidePadding() {
        int[][] sizes = {{320, 240}, {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}};
        for (int[] size : sizes) {
            PoolLayout layout = PoolLayout.compute(size[0], size[1]);
            String name = size[0] + "x" + size[1];
            if (layout.twoFooterRows) {
                int firstRow = layout.actionButtonWidth * 4 + PoolLayout.ACTION_GAP * 3;
                int secondRow = layout.action2ButtonWidth * 3 + PoolLayout.ACTION_GAP * 2;
                assertTrue(firstRow <= size[0] - PoolLayout.PADDING * 2,
                        "first actions " + name);
                assertTrue(secondRow <= size[0] - PoolLayout.PADDING * 2,
                        "second actions " + name);
            } else {
                int singleRow = layout.actionButtonWidth * 7 + PoolLayout.ACTION_GAP * 6;
                assertTrue(singleRow <= size[0] - PoolLayout.PADDING * 2,
                        "single action row " + name);
                assertEquals(layout.actionButtonWidth, layout.action2ButtonWidth,
                        "matching widths " + name);
                int lastRight = layout.action2StartX
                        + (layout.action2ButtonWidth + PoolLayout.ACTION_GAP) * 2
                        + layout.action2ButtonWidth;
                assertTrue(lastRight <= size[0] - PoolLayout.PADDING,
                        "last action right " + name);
            }
            assertTrue(layout.actionButtonWidth >= 56, "button width " + name);
            assertTrue(layout.action2ButtonWidth >= 56, "button2 width " + name);
        }
    }

    @Test
    void wideScreensUseSingleActionRow() {
        int[][] sizes = {{854, 480}, {1280, 720}, {1920, 1080}};
        for (int[] size : sizes) {
            PoolLayout layout = PoolLayout.compute(size[0], size[1]);
            String name = size[0] + "x" + size[1];
            assertFalse(layout.twoFooterRows, "single row " + name);
            assertEquals(layout.actionY, layout.action2Y, "shared row " + name);
            assertEquals(layout.actionStartX + (layout.actionButtonWidth + PoolLayout.ACTION_GAP) * 4,
                    layout.action2StartX, "secondary slot " + name);
            assertTrue(layout.actionStartX >= PoolLayout.PADDING, "start padding " + name);
            assertTrue(layout.actionStartX + layout.actionButtonWidth * 7
                            + PoolLayout.ACTION_GAP * 6 <= size[0] - PoolLayout.PADDING,
                    "row inside padding " + name);
            assertTrue(layout.gridTop + layout.gridHeight <= layout.actionY,
                    "grid above footer " + name);
        }
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
