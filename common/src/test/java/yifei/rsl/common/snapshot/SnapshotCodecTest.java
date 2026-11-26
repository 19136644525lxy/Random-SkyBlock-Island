package yifei.rsl.common.snapshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotCodecTest {
	@TempDir
	Path dir;

	@Test
	void roundTripsAreasAndIndices() throws IOException {
		SnapshotArea first = new SnapshotArea("base", -4, 64, -2, 2, 2, 2,
				List.of("minecraft:stone", "minecraft:oak_stairs[facing=north,half=bottom]"),
				new int[]{0, 1, 0, 1, 1, 0, 0, 1});
		SnapshotArea second = new SnapshotArea("extra", 10, 70, 10, 1, 1, 1,
				List.of("minecraft:air"), new int[]{0});
		RegionSnapshot snapshot = new RegionSnapshot("minecraft:overworld", List.of(first, second));

		Path file = this.dir.resolve("nested/rsl_snapshot.bin");
		SnapshotCodec.save(file, snapshot);

		RegionSnapshot loaded = SnapshotCodec.load(file);
		assertEquals(snapshot.dimension(), loaded.dimension());
		assertEquals(2, loaded.areas().size());
		assertEquals("base", loaded.areas().get(0).name());
		assertEquals(-4, loaded.areas().get(0).minX());
		assertEquals(List.of("minecraft:stone", "minecraft:oak_stairs[facing=north,half=bottom]"),
				loaded.areas().get(0).palette());
		assertArrayEquals(new int[]{0, 1, 0, 1, 1, 0, 0, 1}, loaded.areas().get(0).indices());
		assertEquals(9L, loaded.blockCount());
	}

	@Test
	void rejectsInvalidSnapshotFiles() {
		assertThrows(IOException.class, () -> SnapshotCodec.load(this.dir.resolve("missing.bin")));
		try {
			Files.write(this.dir.resolve("bogus.bin"), new byte[]{1, 2, 3, 4});
		} catch (IOException exception) {
			throw new AssertionError(exception);
		}
		assertThrows(IOException.class, () -> SnapshotCodec.load(this.dir.resolve("bogus.bin")));
	}

	@Test
	void rejectsMismatchedIndexCount() {
		assertThrows(IllegalArgumentException.class, () -> new SnapshotArea("bad", 0, 0, 0, 2, 2, 2,
				List.of("minecraft:stone"), new int[]{0, 0}));
	}
}
