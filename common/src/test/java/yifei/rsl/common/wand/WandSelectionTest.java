package yifei.rsl.common.wand;

import org.junit.jupiter.api.Test;
import yifei.rsl.common.config.AreaSpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WandSelectionTest {
    @Test
    void firstAndSecondPointBuildNormalizedArea() {
        WandSelection selection = new WandSelection();
        selection.setFirst("minecraft:overworld", 5, 70, 9);
        assertTrue(selection.hasFirst());
        assertFalse(selection.hasSecond());

        assertTrue(selection.setSecond("minecraft:overworld", 1, 66, 3));
        assertTrue(selection.isReady());

        AreaSpec area = selection.toArea("test");
        assertEquals(1, area.minX);
        assertEquals(66, area.minY);
        assertEquals(3, area.minZ);
        assertEquals(5, area.maxX);
        assertEquals(70, area.maxY);
        assertEquals(9, area.maxZ);
        assertEquals(175, area.volumeSaturated());
    }

    @Test
    void secondPointFromAnotherDimensionIsRejected() {
        WandSelection selection = new WandSelection();
        selection.setFirst("minecraft:overworld", 0, 64, 0);

        assertFalse(selection.setSecond("minecraft:the_nether", 1, 64, 1));
        assertFalse(selection.hasSecond());
    }

    @Test
    void newFirstPointResetsPreviousSecondPoint() {
        WandSelection selection = new WandSelection();
        selection.setFirst("minecraft:overworld", 0, 64, 0);
        selection.setSecond("minecraft:overworld", 1, 64, 1);
        assertTrue(selection.isReady());

        selection.setFirst("minecraft:overworld", 10, 64, 10);
        assertTrue(selection.hasFirst());
        assertFalse(selection.hasSecond());
        assertNull(selection.toArea("test"));
    }

    @Test
    void clearAndInvalidDimension() {
        WandSelection selection = new WandSelection();
        selection.setFirst("NOT A DIMENSION", 0, 64, 0);
        assertFalse(selection.hasFirst());

        selection.setFirst("minecraft:overworld", 0, 64, 0);
        selection.clear();
        assertFalse(selection.hasFirst());
        assertFalse(selection.hasSecond());
        assertNull(selection.dimension());
    }

    @Test
    void editingTargetSurvivesPointSelectionAndClearsExplicitly() {
        WandSelection selection = new WandSelection();
        selection.beginEdit("base");
        assertTrue(selection.isEditing());
        assertEquals("base", selection.editTarget());

        selection.setFirst("minecraft:overworld", 0, 64, 0);
        selection.setSecond("minecraft:overworld", 4, 68, 4);
        assertTrue(selection.isReady());
        assertTrue(selection.isEditing());

        selection.clear();
        assertFalse(selection.isEditing());
        assertNull(selection.editTarget());
    }

    @Test
    void beginEditResetsPreviousSelection() {
        WandSelection selection = new WandSelection();
        selection.setFirst("minecraft:overworld", 0, 64, 0);
        selection.setSecond("minecraft:overworld", 1, 65, 1);

        selection.beginEdit("base");
        assertFalse(selection.hasFirst());
        assertFalse(selection.hasSecond());
        assertNull(selection.dimension());
        assertEquals("base", selection.editTarget());
    }
}
