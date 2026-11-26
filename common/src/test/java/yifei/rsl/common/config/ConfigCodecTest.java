package yifei.rsl.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCodecTest {
	@TempDir
	Path dir;

	private ConfigCodec codec() {
		return new ConfigCodec(dir);
	}

	@Test
	void createsDefaultsWhenFilesAreMissing() throws IOException {
		ConfigCodec.LoadResult result = codec().load();

		assertEquals(3, result.settings.configVersion);
		assertEquals(180, result.settings.transformDelaySeconds);
		assertEquals(269, result.settings.allowedBlocks.size());
		assertFalse(result.state.isRunning);
		assertEquals(0, result.state.secondsRemaining);
		assertTrue(Files.exists(dir.resolve("randomskyblockisland.json")));
		assertTrue(Files.exists(dir.resolve("randomskyblockisland.state.json")));

		ConfigCodec.LoadResult again = codec().load();
		assertEquals(269, again.settings.allowedBlocks.size());
	}

	@Test
	void roundTripsSettingsAndState() throws IOException {
		ConfigCodec.LoadResult result = codec().load();
		result.settings.transformDelaySeconds = 42;
		result.settings.broadcastMode = "nearby";
		result.settings.allowedBlocks = new ArrayList<>(List.of("minecraft:stone", "examplemod:block"));
		result.settings.poolAccess = new ArrayList<>(List.of(
				new PoolAccessEntry("11111111-1111-1111-1111-111111111111", "Alice",
						PoolAccessEntry.Role.OWNER)));
		codec().saveSettings(result.settings);

		RslState state = new RslState();
		state.isRunning = true;
		state.secondsRemaining = 77;
		codec().saveState(state);

		ConfigCodec.LoadResult loaded = codec().load();
		assertEquals(42, loaded.settings.transformDelaySeconds);
		assertEquals("nearby", loaded.settings.broadcastMode);
		assertEquals(List.of("minecraft:stone", "examplemod:block"), loaded.settings.allowedBlocks);
		assertEquals(1, loaded.settings.poolAccess.size());
		assertEquals("Alice", loaded.settings.poolAccess.get(0).name);
		assertEquals(PoolAccessEntry.Role.OWNER, loaded.settings.poolAccess.get(0).role);
		assertTrue(loaded.state.isRunning);
		assertEquals(77, loaded.state.secondsRemaining);
	}

	@Test
	void malformedConfigVersionFallsBackToDefaults() throws IOException {
		String bad = """
				{
				  "configVersion": "oops",
				  "transformDelaySeconds": 42
				}
				""";
		Files.writeString(dir.resolve("randomskyblockisland.json"), bad, StandardCharsets.UTF_8);

		ConfigCodec.LoadResult result = codec().load();

		assertEquals(180, result.settings.transformDelaySeconds);
		assertEquals(269, result.settings.allowedBlocks.size());
		assertFalse(result.warnings.isEmpty());
		try (var files = Files.list(dir)) {
			assertTrue(files.anyMatch(path -> path.getFileName().toString()
					.startsWith("randomskyblockisland.corrupt-")));
		}
	}

	@Test
	void migratesV1WithLegacyAreasAndState() throws IOException {
		String v1 = """
				{
				  "transformDelaySeconds": 33,
				  "secondsRemaining": 21,
				  "isRunning": true,
				  "allowedBlocks": ["minecraft:stone", "minecraft:dirt"],
				  "areas": [
				    {"name": "default", "minX": -1, "minY": 64, "minZ": -4, "maxX": 4, "maxY": 66, "maxZ": 1},
				    {"name": "farm", "minX": 10, "minY": 60, "minZ": 10, "maxX": 12, "maxY": 62, "maxZ": 12}
				  ]
				}
				""";
		Files.writeString(dir.resolve("randomskyblockisland.json"), v1, StandardCharsets.UTF_8);

		ConfigCodec.LoadResult result = codec().load();

		assertTrue(result.migrated);
		assertEquals(33, result.settings.transformDelaySeconds);
		assertEquals("default", result.settings.defaultArea.name);
		assertEquals(2, result.settings.allowedBlocks.size());
		assertEquals(1, result.legacySaveAreas.size());
		assertEquals("farm", result.legacySaveAreas.get(0).name);
		assertTrue(result.state.isRunning);
		assertEquals(21, result.state.secondsRemaining);

		Path backup = dir.resolve("randomskyblockisland.v1.bak.json");
		assertTrue(Files.exists(backup));
		assertEquals(v1, Files.readString(backup, StandardCharsets.UTF_8));

		String migrated = Files.readString(dir.resolve("randomskyblockisland.json"), StandardCharsets.UTF_8);
		assertTrue(migrated.contains("\"configVersion\": 3"));
		assertFalse(migrated.contains("\"isRunning\""));
		assertFalse(migrated.contains("\"secondsRemaining\""));
		assertFalse(migrated.contains("\"areas\""));
	}

	@Test
	void missingAllowedBlocksFallsBackToDefaults() throws IOException {
		Files.writeString(dir.resolve("randomskyblockisland.json"), "{\"configVersion\":2,\"transformDelaySeconds\":10}");
		ConfigCodec.LoadResult result = codec().load();
		assertEquals(269, result.settings.allowedBlocks.size());
		assertEquals(10, result.settings.transformDelaySeconds);
	}

	@Test
	void emptyAllowedBlocksStaysEmpty() throws IOException {
		Files.writeString(dir.resolve("randomskyblockisland.json"),
				"{\"configVersion\":2,\"allowedBlocks\":[]}");
		ConfigCodec.LoadResult result = codec().load();
		assertTrue(result.settings.allowedBlocks.isEmpty());
	}

	@Test
	void corruptSettingsAreBackedUp() throws IOException {
		Files.writeString(dir.resolve("randomskyblockisland.json"), "{ this is not json");
		ConfigCodec.LoadResult result = codec().load();
		assertEquals(269, result.settings.allowedBlocks.size());
		assertFalse(result.migrated);
		try (var files = Files.list(dir)) {
			assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("randomskyblockisland.corrupt-")));
		}
	}

	@Test
	void areasRoundTrip() throws IOException {
		Path saveDir = dir.resolve("world");
		AreasFile file = new AreasFile();
		file.activeDimension = "minecraft:the_nether";
		file.defaultArea = new AreaSpec("default", -1, 64, -4, 4, 66, 1);
		file.areas.add(new AreaSpec("farm", 10, 60, 10, 12, 62, 12));
		file.areas.add(new AreaSpec("nether", -5, 70, -5, 5, 72, 5));
		codec().saveAreas(saveDir, file);

		ConfigCodec.AreasLoadResult loaded = codec().loadAreas(saveDir);
		assertFalse(loaded.recovered);
		assertEquals(2, loaded.areas.areas.size());
		assertEquals("farm", loaded.areas.areas.get(0).name);
		assertEquals(3, loaded.areas.configVersion);
		assertEquals("minecraft:the_nether", loaded.areas.activeDimension);
		assertEquals("default", loaded.areas.defaultArea.name);
		assertEquals(-1, loaded.areas.defaultArea.minX);
		assertEquals(4, loaded.areas.defaultArea.maxX);
	}

	@Test
	void areasFileWithoutDefaultIsCompatible() throws IOException {
		Path saveDir = dir.resolve("old-world");
		Files.createDirectories(saveDir);
		Files.writeString(saveDir.resolve("rsl_areas.json"), """
				{
				  "configVersion": 2,
				  "areas": [
				    {"name": "farm", "minX": 10, "minY": 60, "minZ": 10, "maxX": 12, "maxY": 62, "maxZ": 12}
				  ]
				}
				""", StandardCharsets.UTF_8);

		ConfigCodec.AreasLoadResult loaded = codec().loadAreas(saveDir);

		assertFalse(loaded.recovered);
		assertNull(loaded.areas.activeDimension);
		assertNull(loaded.areas.defaultArea);
		assertEquals(1, loaded.areas.areas.size());
		assertEquals("farm", loaded.areas.areas.get(0).name);
	}

	@Test
	void areasFileCopyAndSanitizeDefault() {
		AreasFile file = new AreasFile();
		file.defaultArea = new AreaSpec("default", 5, 70, 5, 1, 64, 1);

		AreasFile copy = file.copy();
		assertEquals("default", copy.defaultArea.name);
		assertEquals(5, copy.defaultArea.minX);

		file.sanitize();
		assertEquals(1, file.defaultArea.minX);
		assertEquals(5, file.defaultArea.maxX);
		assertEquals(64, file.defaultArea.minY);
		assertEquals(70, file.defaultArea.maxY);
	}

	@Test
	void areasFileSanitizesActiveDimension() {
		AreasFile file = new AreasFile();
		file.activeDimension = " MINECRAFT:The_Nether ";
		file.sanitize();
		assertEquals("minecraft:the_nether", file.activeDimension);

		file.activeDimension = "bad id";
		file.sanitize();
		assertNull(file.activeDimension);

		AreasFile copy = new AreasFile();
		copy.activeDimension = "minecraft:the_end";
		assertEquals("minecraft:the_end", copy.copy().activeDimension);
	}

	@Test
	void corruptAreasAreBackedUp() throws IOException {
		Path saveDir = dir.resolve("world");
		Files.createDirectories(saveDir);
		Files.writeString(saveDir.resolve("rsl_areas.json"), "not json");
		ConfigCodec.AreasLoadResult loaded = codec().loadAreas(saveDir);
		assertTrue(loaded.recovered);
		assertTrue(loaded.areas.areas.isEmpty());
		try (var files = Files.list(saveDir)) {
			assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("rsl_areas.corrupt-")));
		}
	}
}
