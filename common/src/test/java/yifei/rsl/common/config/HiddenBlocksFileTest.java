package yifei.rsl.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiddenBlocksFileTest {
    @TempDir
    Path directory;

    @Test
    void missingFileIsEmpty() {
        HiddenBlocksFile file = HiddenBlocksFile.load(directory);

        assertEquals(0, file.blocks().count());
        assertFalse(file.hadLoadError());
    }

    @Test
    void roundTripsThroughDisk() {
        HiddenBlocksFile file = HiddenBlocksFile.load(directory);
        file.blocks().hideAll(List.of("minecraft:stone", "modid:custom"));

        assertTrue(file.save());
        assertTrue(Files.exists(file.path()));

        HiddenBlocksFile reloaded = HiddenBlocksFile.load(directory);
        assertTrue(reloaded.blocks().isHidden("minecraft:stone"));
        assertTrue(reloaded.blocks().isHidden("modid:custom"));
        assertEquals(List.of("minecraft:stone", "modid:custom"), reloaded.blocks().list());
    }

    @Test
    void unknownIdsArePreserved() {
        HiddenBlocksFile file = HiddenBlocksFile.load(directory);
        file.blocks().hideAll(List.of("removedmod:missing_block"));
        assertTrue(file.save());

        HiddenBlocksFile reloaded = HiddenBlocksFile.load(directory);

        assertTrue(reloaded.blocks().isHidden("removedmod:missing_block"));
    }

    @Test
    void malformedFileIsBackedUpAndTreatedAsEmpty() throws IOException {
        Path path = directory.resolve(HiddenBlocksFile.FILE_NAME);
        Files.writeString(path, "{ this is not json");

        HiddenBlocksFile file = HiddenBlocksFile.load(directory);

        assertTrue(file.hadLoadError());
        assertEquals(0, file.blocks().count());
        assertFalse(Files.exists(path));
        try (Stream<Path> files = Files.list(directory)) {
            assertTrue(files.anyMatch(candidate -> candidate.getFileName().toString()
                    .startsWith("rsl-hidden-blocks.corrupt-")));
        }
    }

    @Test
    void saveCreatesParentDirectories() {
        Path nested = directory.resolve("config");
        HiddenBlocksFile file = HiddenBlocksFile.load(nested);
        file.blocks().hideAll(List.of("minecraft:stone"));

        assertTrue(file.save());
        assertTrue(Files.exists(nested.resolve(HiddenBlocksFile.FILE_NAME)));
    }

    @Test
    void contextFilesAreIndependent() {
        Path config = directory.resolve("config");
        HiddenBlocksFile first = HiddenBlocksFile.load(config, "sp:first");
        first.blocks().hideAll(List.of("minecraft:stone"));
        assertTrue(first.save());

        HiddenBlocksFile second = HiddenBlocksFile.load(config, "sp:second");
        assertTrue(second.blocks().isEmpty());
        second.blocks().hideAll(List.of("minecraft:dirt"));
        assertTrue(second.save());

        HiddenBlocksFile firstAgain = HiddenBlocksFile.load(config, "sp:first");
        assertTrue(firstAgain.blocks().isHidden("minecraft:stone"));
        assertFalse(firstAgain.blocks().isHidden("minecraft:dirt"));

        assertNotEquals(first.path(), second.path());
    }

    @Test
    void legacyFileSeedsEachContextOnce() {
        Path config = directory.resolve("config");
        HiddenBlocksFile legacy = HiddenBlocksFile.load(config);
        legacy.blocks().hideAll(List.of("minecraft:stone"));
        assertTrue(legacy.save());

        HiddenBlocksFile first = HiddenBlocksFile.load(config, "sp:first");
        assertTrue(first.blocks().isHidden("minecraft:stone"));
        assertTrue(Files.exists(first.path()));

        HiddenBlocksFile second = HiddenBlocksFile.load(config, "mp:example.org");
        assertTrue(second.blocks().isHidden("minecraft:stone"));
        assertNotEquals(first.path(), second.path());
    }

    @Test
    void contextPathIsStableAndCollisionResistant() {
        Path config = directory.resolve("config");
        Path first = HiddenBlocksFile.contextPath(config, "sp:world");
        Path again = HiddenBlocksFile.contextPath(config, "sp:world");
        Path other = HiddenBlocksFile.contextPath(config, "sp:other");

        assertEquals(first, again);
        assertNotEquals(first, other);
        assertEquals(HiddenBlocksFile.DIRECTORY_NAME,
                first.getParent().getFileName().toString());
    }

    @Test
    void missingLegacyStillMeansEmptyContext() {
        Path config = directory.resolve("config");
        HiddenBlocksFile file = HiddenBlocksFile.load(config, "sp:new-world");

        assertTrue(file.blocks().isEmpty());
        assertFalse(Files.exists(file.path()));
    }

    @Test
    void nameRoundTripsAndIsOptional() {
        HiddenBlocksFile file = HiddenBlocksFile.load(directory);
        assertTrue(file.name().isEmpty());
        assertTrue(file.setName("石头集合"));
        file.blocks().hideAll(List.of("minecraft:stone"));
        assertTrue(file.save());

        HiddenBlocksFile reloaded = HiddenBlocksFile.load(directory);
        assertEquals("石头集合", reloaded.name());
        assertTrue(reloaded.blocks().isHidden("minecraft:stone"));

        assertTrue(reloaded.setName(""));
        assertTrue(reloaded.save());
        assertEquals("", HiddenBlocksFile.load(directory).name());
    }

    @Test
    void nameIsTrimmedTruncatedAndChangeAware() {
        HiddenBlocksFile file = HiddenBlocksFile.load(directory);
        assertTrue(file.setName("  abc  "));
        assertEquals("abc", file.name());
        assertFalse(file.setName("abc"));
        file.setName("x".repeat(40));
        assertEquals(32, file.name().length());
    }

    @Test
    void listAllReportsEveryContextFile() {
        Path config = directory.resolve("config");
        HiddenBlocksFile first = HiddenBlocksFile.load(config, "sp:first");
        first.setName("石头集合");
        first.blocks().hideAll(List.of("minecraft:stone"));
        assertTrue(first.save());
        HiddenBlocksFile second = HiddenBlocksFile.load(config, "sp:second");
        second.blocks().hideAll(List.of("minecraft:dirt", "minecraft:gravel"));
        assertTrue(second.save());

        List<HiddenBlocksFile.PoolInfo> pools = HiddenBlocksFile.listAll(config);

        assertEquals(2, pools.size());
        HiddenBlocksFile.PoolInfo named = pools.stream()
                .filter(pool -> "石头集合".equals(pool.name())).findFirst().orElseThrow();
        assertEquals("sp:first", named.contextKey());
        assertEquals(1, named.count());
        HiddenBlocksFile.PoolInfo unnamed = pools.stream()
                .filter(pool -> "sp:second".equals(pool.contextKey())).findFirst().orElseThrow();
        assertEquals(2, unnamed.count());
        assertEquals("sp:second", unnamed.displayName());
    }

    @Test
    void importFromCopiesBlocksAndName() {
        Path config = directory.resolve("config");
        HiddenBlocksFile source = HiddenBlocksFile.load(config, "sp:source");
        source.setName("来源池");
        source.blocks().hideAll(List.of("minecraft:stone", "modid:custom"));
        assertTrue(source.save());

        HiddenBlocksFile target = HiddenBlocksFile.load(config, "sp:target");
        assertTrue(target.importFrom(source.path()));
        assertEquals("来源池", target.name());
        assertEquals(List.of("minecraft:stone", "modid:custom"), target.blocks().list());

        HiddenBlocksFile reloaded = HiddenBlocksFile.load(config, "sp:target");
        assertEquals("来源池", reloaded.name());
        assertEquals(2, reloaded.blocks().count());
    }

    @Test
    void importFromRejectsMissingAndSelf() {
        Path config = directory.resolve("config");
        HiddenBlocksFile file = HiddenBlocksFile.load(config, "sp:one");
        file.blocks().hideAll(List.of("minecraft:stone"));
        assertTrue(file.save());

        assertFalse(file.importFrom(file.path()));
        assertFalse(file.importFrom(config.resolve("missing.json")));
        assertEquals(1, file.blocks().count());
    }
}
