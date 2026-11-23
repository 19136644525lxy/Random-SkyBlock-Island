package yifei.rsl.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RslConstantsTest {
	@Test
	void configVersionIsTwo() {
		assertEquals(2, RslConstants.CONFIG_VERSION);
	}

	@Test
	void fileNamesAreStable() {
		assertEquals("randomskyblockisland.json", RslConstants.SETTINGS_FILE);
		assertEquals("randomskyblockisland.state.json", RslConstants.STATE_FILE);
		assertEquals("rsl_areas.json", RslConstants.AREAS_FILE);
	}
}
