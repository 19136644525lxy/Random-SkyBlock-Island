package yifei.rsl.common.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolMutationResultTest {

    @Test
    void failedIdsNullBecomesEmpty() {
        PoolMutationResult result = PoolMutationResult.fail(BlockMutationResult.INVALID_ID, "rsl.message.blocks_invalid_id", null);
        assertFalse(result.success);
        assertEquals(BlockMutationResult.INVALID_ID, result.code);
        assertTrue(result.failedIds.isEmpty());
    }

    @Test
    void failedIdsAreDefensivelyCopied() {
        List<String> mutable = new ArrayList<>(List.of("bad:id", "missing:block"));
        PoolMutationResult result = PoolMutationResult.fail(BlockMutationResult.UNKNOWN_BLOCK, "rsl.message.blocks_unknown", mutable);
        mutable.add("later");
        assertEquals(List.of("bad:id", "missing:block"), result.failedIds);
    }

    @Test
    void okHasNoFailures() {
        PoolMutationResult result = PoolMutationResult.ok();
        assertTrue(result.success);
        assertEquals(BlockMutationResult.OK, result.code);
        assertTrue(result.failedIds.isEmpty());
    }
}
