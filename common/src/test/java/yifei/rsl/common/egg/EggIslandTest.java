package yifei.rsl.common.egg;

import org.junit.jupiter.api.Test;
import yifei.rsl.common.config.AreaSpec;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EggIslandTest {

    @Test
    void edgeLengthRoundsUpToTheNextCube() {
        assertEquals(1, EggIsland.edgeLength(-5));
        assertEquals(1, EggIsland.edgeLength(0));
        assertEquals(1, EggIsland.edgeLength(1));
        assertEquals(2, EggIsland.edgeLength(8));
        assertEquals(3, EggIsland.edgeLength(27));
        assertEquals(4, EggIsland.edgeLength(28));
        assertEquals(5, EggIsland.edgeLength(65));
    }

    @Test
    void cubeVolumeIsEdgeCubed() {
        assertEquals(27, EggIsland.cubeVolume(3));
        assertEquals(1, EggIsland.cubeVolume(0));
    }

    @Test
    void overlapAndDistanceAreMeasuredOnAllAxes() {
        AreaSpec area = new AreaSpec();
        area.minX = 10;
        area.minY = 0;
        area.minZ = 0;
        area.maxX = 12;
        area.maxY = 2;
        area.maxZ = 2;

        assertFalse(EggIsland.tooClose(0, 0, 0, 2, 2, 2, area, 5));
        assertTrue(EggIsland.tooClose(0, 0, 0, 2, 2, 2, area, 7));
        assertTrue(EggIsland.tooClose(11, 1, 1, 13, 3, 3, area, 0));
        assertFalse(EggIsland.tooClose(0, 5, 0, 2, 7, 2, area, 0));
    }

    @Test
    void anyAreaTooCloseChecksEveryArea() {
        AreaSpec far = new AreaSpec();
        far.minX = 100;
        far.maxX = 110;
        far.minY = 0;
        far.maxY = 10;
        far.minZ = 0;
        far.maxZ = 10;

        AreaSpec near = new AreaSpec();
        near.minX = 8;
        near.maxX = 12;
        near.minY = 0;
        near.maxY = 10;
        near.minZ = 0;
        near.maxZ = 10;

        assertFalse(EggIsland.anyAreaTooClose(0, 0, 0, 3, 3, 3, List.of(far), 16));
        assertTrue(EggIsland.anyAreaTooClose(0, 0, 0, 3, 3, 3, List.of(far, near), 16));
        assertFalse(EggIsland.anyAreaTooClose(0, 0, 0, 3, 3, 3, List.of(), 16));
    }

    @Test
    void worldHeightUsesTheFullCube() {
        assertTrue(EggIsland.insideWorldHeight(-60, 3, -64, 320));
        assertFalse(EggIsland.insideWorldHeight(-64, 3, -64, 320));
        assertFalse(EggIsland.insideWorldHeight(318, 3, -64, 320));
        assertTrue(EggIsland.insideWorldHeight(317, 3, -64, 320));
    }
}
