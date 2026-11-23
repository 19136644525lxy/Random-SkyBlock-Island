package yifei.rsl.common.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RslSettingsTest {
	@Test
	void sanitizeClampsAndNormalizes() {
		RslSettings settings = new RslSettings();
		settings.transformDelaySeconds = 0;
		settings.maxAreaVolume = -5;
		settings.blocksPerTick = 1;
		settings.broadcastRadius = 10_000;
		settings.blocksPermissionLevel = 99;
		settings.broadcastMode = "NOPE";
		settings.defaultArea = null;
		settings.allowedBlocks = List.of("Stone", "minecraft:stone", "bad id", " ", "mod:block");
		settings.sanitize();

		assertEquals(1, settings.transformDelaySeconds);
		assertEquals(1, settings.maxAreaVolume);
		assertEquals(256, settings.blocksPerTick);
		assertEquals(512, settings.broadcastRadius);
		assertEquals(4, settings.blocksPermissionLevel);
		assertEquals("all", settings.broadcastMode);
		assertEquals("default", settings.defaultArea.name);
		assertEquals(List.of("minecraft:stone", "bad id", "mod:block"), settings.allowedBlocks);
	}

	@Test
	void permissionLevelZeroIsPreserved() {
		RslSettings settings = new RslSettings();
		settings.blocksPermissionLevel = 0;
		settings.sanitize();
		assertEquals(0, settings.blocksPermissionLevel);
	}

	@Test
	void validatesBroadcastModes() {
		RslSettings settings = new RslSettings();
		settings.broadcastMode = "nearby";
		settings.sanitize();
		assertEquals("nearby", settings.broadcastMode);
		settings.broadcastMode = "summary";
		settings.sanitize();
		assertEquals("summary", settings.broadcastMode);
	}

	@Test
	void copyIsIndependent() {
		RslSettings settings = new RslSettings();
		settings.allowedBlocks = new java.util.ArrayList<>(List.of("minecraft:stone"));
		RslSettings copy = settings.copy();
		copy.allowedBlocks.add("mod:block");
		assertEquals(1, settings.allowedBlocks.size());
		assertEquals(2, copy.allowedBlocks.size());
		assertFalse(settings.defaultArea == copy.defaultArea);
		assertTrue(settings.defaultArea.display().equals(copy.defaultArea.display()));
	}
}
