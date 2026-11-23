package yifei.rsl.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSpecTest {
	@Test
	void defaultsMatchExistingConfig() {
		AreaSpec area = AreaSpec.defaultArea();
		assertEquals("default", area.name);
		assertEquals(-1, area.minX);
		assertEquals(64, area.minY);
		assertEquals(-4, area.minZ);
		assertEquals(4, area.maxX);
		assertEquals(66, area.maxY);
		assertEquals(1, area.maxZ);
	}

	@Test
	void normalizedOrdersEveryAxis() {
		AreaSpec area = new AreaSpec("x", 4, 66, 1, -1, 64, -4).normalized();
		assertEquals(-1, area.minX);
		assertEquals(64, area.minY);
		assertEquals(-4, area.minZ);
		assertEquals(4, area.maxX);
		assertEquals(66, area.maxY);
		assertEquals(1, area.maxZ);
	}

	@Test
	void volumeUsesLongMath() {
		assertEquals(24L, new AreaSpec("x", 0, 0, 0, 1, 2, 3).volumeSaturated());
		assertEquals(1L, new AreaSpec("x", 5, 5, 5, 5, 5, 5).volumeSaturated());
	}

	@Test
	void volumeSaturatesInsteadOfOverflowing() {
		AreaSpec huge = new AreaSpec("x", -30_000_000, -30_000_000, -30_000_000,
				30_000_000, 30_000_000, 30_000_000);
		assertEquals(Long.MAX_VALUE, huge.volumeSaturated());
	}

	@Test
	void containsNormalizesCoordinates() {
		AreaSpec area = new AreaSpec("x", 4, 66, 1, -1, 64, -4);
		assertTrue(area.contains(0, 65, 0));
		assertTrue(area.contains(-1, 64, -4));
		assertTrue(area.contains(4, 66, 1));
		assertFalse(area.contains(5, 65, 0));
		assertFalse(area.contains(0, 63, 0));
	}

	@Test
	void validatesNames() {
		assertTrue(new AreaSpec("default", 0, 0, 0, 0, 0, 0).hasValidName());
		assertTrue(new AreaSpec("area-1_a", 0, 0, 0, 0, 0, 0).hasValidName());
		assertFalse(new AreaSpec("", 0, 0, 0, 0, 0, 0).hasValidName());
		assertFalse(new AreaSpec("bad name", 0, 0, 0, 0, 0, 0).hasValidName());
		assertFalse(new AreaSpec("a".repeat(33), 0, 0, 0, 0, 0, 0).hasValidName());
	}

	@Test
	void validatesWorldCoordinates() {
		assertTrue(new AreaSpec("x", -30_000_000, -64, -30_000_000,
				30_000_000, 320, 30_000_000).hasValidCoordinates());
		assertFalse(new AreaSpec("x", 30_000_001, 0, 0, 30_000_001, 0, 0).hasValidCoordinates());
	}
}
