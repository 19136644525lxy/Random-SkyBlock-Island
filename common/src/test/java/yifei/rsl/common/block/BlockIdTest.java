package yifei.rsl.common.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockIdTest {
	@Test
	void addsMinecraftNamespace() {
		assertEquals("minecraft:stone", BlockId.normalize("stone"));
		assertEquals("minecraft:oak_log", BlockId.normalize("  OAK_LOG  "));
	}

	@Test
	void keepsExplicitNamespace() {
		assertEquals("mod:block", BlockId.normalize("mod:block"));
		assertEquals("minecraft:stone", BlockId.normalize("Minecraft:Stone"));
	}

	@Test
	void rejectsMalformedIds() {
		assertNull(BlockId.normalize(null));
		assertNull(BlockId.normalize(""));
		assertNull(BlockId.normalize("   "));
		assertNull(BlockId.normalize(":stone"));
		assertNull(BlockId.normalize("minecraft:"));
		assertNull(BlockId.normalize("a:b:c"));
		assertNull(BlockId.normalize("bad id"));
		assertNull(BlockId.normalize("minecraft:Bad Space"));
	}

	@Test
	void rejectsOverlongIds() {
		StringBuilder path = new StringBuilder("minecraft:");
		path.append("a".repeat(130));
		assertNull(BlockId.normalize(path.toString()));
	}

	@Test
	void validSyntaxMatchesNormalize() {
		assertTrue(BlockId.isValidSyntax("minecraft:stone"));
		assertFalse(BlockId.isValidSyntax("nope nope"));
	}

	@Test
	void splitsNamespaceAndPath() {
		assertEquals("minecraft", BlockId.namespace("minecraft:stone"));
		assertEquals("stone", BlockId.path("minecraft:stone"));
		assertEquals("mod", BlockId.namespace("mod:x"));
		assertEquals("x", BlockId.path("mod:x"));
	}
}
