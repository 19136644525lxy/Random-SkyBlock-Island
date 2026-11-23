package yifei.rsl.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * Per-context persistence for {@link HiddenBlocks}. A context is a save or a
 * multiplayer server, so each world keeps its own hidden list. The old global
 * file is used as a seed when a context has no file yet.
 */
public final class HiddenBlocksFile {
    /** Old, global file. Kept as a seed for migration and for older builds. */
    public static final String FILE_NAME = "rsl-hidden-blocks.json";
    /** Directory holding one JSON file per save/server context. */
    public static final String DIRECTORY_NAME = "rsl-hidden-blocks";
    private static final String CORRUPT_PREFIX = "rsl-hidden-blocks.corrupt-";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_NAME_LENGTH = 32;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path path;
    private final String contextKey;
    private HiddenBlocks blocks = new HiddenBlocks();
    private String name = "";
    private boolean loadError;

    private HiddenBlocksFile(Path path, String contextKey) {
        this.path = path;
        this.contextKey = contextKey;
    }

    /**
     * Loads the old global file, without context migration. Used by tests and
     * kept for compatibility with older callers.
     */
    public static HiddenBlocksFile load(Path configDir) {
        HiddenBlocksFile file = new HiddenBlocksFile(configDir.resolve(FILE_NAME), null);
        file.reload();
        return file;
    }

    /**
     * Loads the hidden list for one save/server. If the per-context file does
     * not exist yet, the old global file is copied into this context on first
     * use, so an existing hidden list survives the upgrade.
     */
    public static HiddenBlocksFile load(Path configDir, String contextKey) {
        String context = normalizeContext(contextKey);
        HiddenBlocksFile file = new HiddenBlocksFile(contextPath(configDir, context), context);
        file.reload();
        if (!Files.exists(file.path)) {
            file.migrateLegacy(configDir.resolve(FILE_NAME));
        }
        return file;
    }

    public static Path contextPath(Path configDir, String contextKey) {
        String context = normalizeContext(contextKey);
        return configDir.resolve(DIRECTORY_NAME).resolve(hash(context) + ".json");
    }

    /**
     * Re-reads the current file. Missing files are an empty set; malformed files
     * are moved aside and treated as empty.
     */
    public boolean reload() {
        HiddenBlocks loaded = new HiddenBlocks();
        name = "";
        loadError = false;
        if (!Files.exists(path)) {
            blocks = loaded;
            return true;
        }
        try {
            Payload payload = read(path);
            if (payload != null && payload.hidden != null) {
                loaded.hideAll(payload.hidden);
            }
            if (payload != null && payload.name != null) {
                name = payload.name.trim();
            }
            blocks = loaded;
            return true;
        } catch (Exception exception) {
            backupCorrupt(path);
            blocks = loaded;
            loadError = true;
            return false;
        }
    }

    public boolean save() {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Payload payload = new Payload();
            payload.context = contextKey;
            payload.name = name.isEmpty() ? null : name;
            payload.hidden = blocks.list();
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(payload, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public HiddenBlocks blocks() {
        return blocks;
    }

    /**
     * Copies the hidden list and display name from another context file into
     * this one. Used by the "copy from another save" picker. The current state
     * is restored when saving fails.
     */
    public boolean importFrom(Path source) {
        if (source == null || !Files.exists(source) || source.toAbsolutePath().equals(
                path.toAbsolutePath())) {
            return false;
        }
        try {
            Payload payload = read(source);
            if (payload == null || payload.hidden == null) {
                return false;
            }
            HiddenBlocks previousBlocks = blocks;
            String previousName = name;
            HiddenBlocks next = new HiddenBlocks();
            next.hideAll(payload.hidden);
            blocks = next;
            setName(payload.name == null ? "" : payload.name);
            if (save()) {
                return true;
            }
            blocks = previousBlocks;
            name = previousName;
            return false;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Lists every hidden-list file under the config directory, newest first.
     * Corrupt or unreadable files are skipped.
     */
    public static List<PoolInfo> listAll(Path configDir) {
        Path directory = configDir.resolve(DIRECTORY_NAME);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<PoolInfo> pools = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
                    .filter(candidate -> !candidate.getFileName().toString()
                            .startsWith(CORRUPT_PREFIX))
                    .forEach(candidate -> {
                        try {
                            Payload payload = read(candidate);
                            if (payload == null) {
                                return;
                            }
                            int count = payload.hidden == null ? 0 : payload.hidden.size();
                            String context = payload.context == null ? "" : payload.context.trim();
                            String displayName = payload.name == null ? "" : payload.name.trim();
                            pools.add(new PoolInfo(candidate, context, displayName, count,
                                    Files.getLastModifiedTime(candidate).toMillis()));
                        } catch (Exception ignored) {
                            // Skip unreadable files; the rest of the list is still useful.
                        }
                    });
        } catch (IOException exception) {
            return List.of();
        }
        pools.sort(Comparator.comparingLong(PoolInfo::modifiedAt).reversed());
        return pools;
    }

    /** One hidden-list file on disk, as shown by the copy picker. */
    public record PoolInfo(Path path, String contextKey, String name, int count, long modifiedAt) {
        public String displayName() {
            if (name != null && !name.isEmpty()) {
                return name;
            }
            return contextKey == null || contextKey.isEmpty()
                    ? path.getFileName().toString()
                    : contextKey;
        }
    }

    public Path path() {
        return path;
    }

    public String contextKey() {
        return contextKey;
    }

    /** Display name of this hidden list; empty when unnamed. */
    public String name() {
        return name;
    }

    /**
     * Updates the display name. Blank clears it and long values are truncated.
     * Returns true when the stored name changed.
     */
    public boolean setName(String value) {
        String next = value == null ? "" : value.trim();
        if (next.length() > MAX_NAME_LENGTH) {
            next = next.substring(0, MAX_NAME_LENGTH);
        }
        if (next.equals(name)) {
            return false;
        }
        name = next;
        return true;
    }

    public boolean hadLoadError() {
        return loadError;
    }

    private void migrateLegacy(Path legacy) {
        if (legacy.equals(path) || !Files.exists(legacy)) {
            return;
        }
        try {
            Payload payload = read(legacy);
            if (payload == null || payload.hidden == null) {
                return;
            }
            HiddenBlocks seeded = new HiddenBlocks();
            seeded.hideAll(payload.hidden);
            if (seeded.isEmpty()) {
                return;
            }
            blocks = seeded;
            // Keep the old global file untouched; it remains a fallback seed
            // for other saves/servers that have never been opened before.
            save();
        } catch (Exception exception) {
            backupCorrupt(legacy);
            loadError = true;
        }
    }

    private static Payload read(Path source) throws IOException {
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, Payload.class);
        }
    }

    private static String normalizeContext(String contextKey) {
        if (contextKey == null) {
            return "unknown";
        }
        String value = contextKey.trim();
        return value.isEmpty() ? "unknown" : value;
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private void backupCorrupt(Path source) {
        if (!Files.exists(source)) {
            return;
        }
        Path parent = source.toAbsolutePath().getParent();
        String name = CORRUPT_PREFIX + LocalDateTime.now().format(TIMESTAMP) + ".json";
        try {
            Files.move(source, parent == null ? source.resolveSibling(name) : parent.resolve(name),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // The screen still falls back to an empty set.
        }
    }

    private static final class Payload {
        private String context;
        private String name;
        private List<String> hidden;
    }
}
