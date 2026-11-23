package yifei.rsl.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionIdTest {
	@Test
	void addsDefaultNamespace() {
		assertEquals("minecraft:the_nether", DimensionId.normalize("the_nether"));
		assertEquals("minecraft:overworld", DimensionId.normalize(" MINECRAFT:Overworld "));
	}

	@Test
	void keepsCustomNamespaces() {
		assertEquals("examplemod:custom_dimension", DimensionId.normalize("examplemod:custom_dimension"));
		assertEquals("examplemod:dimension/test", DimensionId.normalize("examplemod:dimension/test"));
	}

	@Test
	void rejectsInvalidIds() {
		assertNull(DimensionId.normalize(null));
		assertNull(DimensionId.normalize(""));
		assertNull(DimensionId.normalize("bad id"));
		assertNull(DimensionId.normalize("a:b:c"));
		assertNull(DimensionId.normalize(":bad"));
		assertNull(DimensionId.normalize("mod:"));
		assertFalse(DimensionId.isValid("mod:bad path"));
	}

	@Test
	void reportsValidity() {
		assertTrue(DimensionId.isValid("the_end"));
		assertTrue(DimensionId.isValid("examplemod:custom_dimension"));
		assertFalse(DimensionId.isValid("bad id"));
	}

	@Test
	void buildsTranslationKeys() {
		assertEquals("dimension.minecraft.the_nether", DimensionId.translationKey("the_nether"));
		assertEquals("dimension.examplemod.custom_dimension",
				DimensionId.translationKey("examplemod:custom_dimension"));
		assertEquals("dimension.unknown", DimensionId.translationKey("bad id"));
	}
}
