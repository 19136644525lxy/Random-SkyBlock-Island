package yifei.rsl.common.block;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultBlocksTest {
	@Test
	void loadsTheSharedDefaultPool() {
		List<String> ids = DefaultBlocks.load();
		assertEquals(269, ids.size());
		assertEquals("minecraft:oak_log", ids.get(0));
		assertEquals("minecraft:infested_chiseled_stone_bricks", ids.get(ids.size() - 1));
		assertEquals(ids.size(), new HashSet<>(ids).size());
		for (String id : ids) {
			assertTrue(BlockId.isValidSyntax(id), id);
		}
	}

	@Test
	void rejectsInvalidEntry() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> DefaultBlocks.load(
				new ByteArrayInputStream("[\"minecraft:stone\", \"bad id\"]".getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	void rejectsDuplicateEntry() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> DefaultBlocks.load(
				new ByteArrayInputStream("[\"stone\", \"minecraft:stone\"]".getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	void rejectsEmptyResource() {
		assertThrows(IllegalArgumentException.class, () -> DefaultBlocks.load(
				new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	void normalizesEntries() throws Exception {
		List<String> ids = DefaultBlocks.load(
				new ByteArrayInputStream("[\"Stone\", \"mod:block\"]".getBytes(StandardCharsets.UTF_8)));
		assertEquals(List.of("minecraft:stone", "mod:block"), ids);
	}
}
