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
		settings.broadcastMode = "NOPE";
		settings.defaultArea = null;
		settings.allowedBlocks = List.of("Stone", "minecraft:stone", "bad id", " ", "mod:block");
		settings.sanitize();

		assertEquals(1, settings.transformDelaySeconds);
		assertEquals(1, settings.maxAreaVolume);
		assertEquals(256, settings.blocksPerTick);
		assertEquals(512, settings.broadcastRadius);
		assertEquals("all", settings.broadcastMode);
		assertEquals("default", settings.defaultArea.name);
		assertEquals(List.of("minecraft:stone", "bad id", "mod:block"), settings.allowedBlocks);
	}

	@Test
	void sanitizesPoolAccess() {
		RslSettings settings = new RslSettings();
		settings.poolAccess = new java.util.ArrayList<>(List.of(
				new PoolAccessEntry("11111111-1111-1111-1111-111111111111", "Alice",
						PoolAccessEntry.Role.EDITOR),
				new PoolAccessEntry("bad-uuid", "Bad", PoolAccessEntry.Role.OWNER)));
		settings.sanitize();
		assertEquals(1, settings.poolAccess.size());
		assertEquals(PoolAccessEntry.Role.EDITOR, settings.poolAccess.get(0).role);
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
		settings.poolAccess = new java.util.ArrayList<>(List.of(
				new PoolAccessEntry("11111111-1111-1111-1111-111111111111", "Alice",
						PoolAccessEntry.Role.OWNER)));
		RslSettings copy = settings.copy();
		copy.allowedBlocks.add("mod:block");
		copy.poolAccess.get(0).name = "Changed";
		assertEquals(1, settings.allowedBlocks.size());
		assertEquals(2, copy.allowedBlocks.size());
		assertEquals("Alice", settings.poolAccess.get(0).name);
		assertEquals("Changed", copy.poolAccess.get(0).name);
		assertFalse(settings.defaultArea == copy.defaultArea);
		assertTrue(settings.defaultArea.display().equals(copy.defaultArea.display()));
	}
}
