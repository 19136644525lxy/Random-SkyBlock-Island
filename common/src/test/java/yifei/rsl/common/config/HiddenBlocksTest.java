package yifei.rsl.common.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiddenBlocksTest {
    @Test
    void hidesAndShowsWithDeduplication() {
        HiddenBlocks blocks = new HiddenBlocks();

        assertEquals(2, blocks.hideAll(Arrays.asList("minecraft:stone", " modid:block ", "minecraft:stone")));
        assertTrue(blocks.isHidden("minecraft:stone"));
        assertTrue(blocks.isHidden("modid:block"));
        assertEquals(2, blocks.count());

        assertEquals(1, blocks.showAll(List.of("minecraft:stone")));
        assertFalse(blocks.isHidden("minecraft:stone"));
        assertEquals(1, blocks.count());
    }

    @Test
    void ignoresNullAndBlankIds() {
        HiddenBlocks blocks = new HiddenBlocks();

        assertEquals(0, blocks.hideAll(Arrays.asList(null, "", "   ")));
        assertEquals(0, blocks.count());
        assertFalse(blocks.isHidden(null));
        assertFalse(blocks.show(null));
    }

    @Test
    void filterVisiblePreservesInputOrder() {
        HiddenBlocks blocks = new HiddenBlocks();
        blocks.hideAll(List.of("minecraft:dirt"));

        assertEquals(List.of("minecraft:stone", "minecraft:grass_block"),
                blocks.filterVisible(List.of("minecraft:stone", "minecraft:dirt", "minecraft:grass_block")));
    }

    @Test
    void keepsInsertionOrder() {
        HiddenBlocks blocks = new HiddenBlocks();
        blocks.hideAll(List.of("modid:b", "minecraft:a", "modid:c"));

        assertEquals(List.of("modid:b", "minecraft:a", "modid:c"), blocks.list());
    }

    @Test
    void clearRemovesEverything() {
        HiddenBlocks blocks = new HiddenBlocks();
        blocks.hideAll(List.of("minecraft:stone", "minecraft:dirt"));

        blocks.clear();

        assertTrue(blocks.isEmpty());
        assertEquals(0, blocks.count());
    }
}
