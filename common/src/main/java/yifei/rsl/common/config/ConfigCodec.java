package yifei.rsl.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import yifei.rsl.common.RslConstants;
import yifei.rsl.common.block.DefaultBlocks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads, migrates and atomically writes the mod's JSON files. Pure JDK + Gson so it can
 * live in the shared source set.
 */
public final class ConfigCodec {
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private final Path configDir;
	private final Gson gson;

	public ConfigCodec(Path configDir) {
		this.configDir = configDir;
		this.gson = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	public Path settingsPath() {
		return configDir.resolve(RslConstants.SETTINGS_FILE);
	}

	public Path statePath() {
		return configDir.resolve(RslConstants.STATE_FILE);
	}

	public Path areasPath(Path saveDir) {
		return saveDir.resolve(RslConstants.AREAS_FILE);
	}

	public LoadResult load() throws IOException {
		Files.createDirectories(configDir);
		List<String> warnings = new ArrayList<>();
		RslSettings settings = null;
		RslState state = new RslState();
		List<AreaSpec> legacySaveAreas = new ArrayList<>();
		boolean migrated = false;
		boolean writeSettings = false;
		boolean writeState = false;

		Path settingsPath = settingsPath();
		JsonObject root = null;
		if (Files.exists(settingsPath)) {
			try {
				root = JsonParser.parseString(Files.readString(settingsPath, StandardCharsets.UTF_8)).getAsJsonObject();
			} catch (RuntimeException e) {
				Path backup = moveToBackup(settingsPath, "randomskyblockisland.corrupt-");
				warnings.add("Settings file was corrupt; backed up to " + backup.getFileName());
				writeSettings = true;
				writeState = true;
			}
		} else {
			writeSettings = true;
			writeState = true;
		}

		if (root == null) {
			settings = defaults();
		} else {
			int version = 0;
			if (root.has("configVersion") && root.get("configVersion").isJsonPrimitive()
					&& root.get("configVersion").getAsJsonPrimitive().isNumber()) {
				version = root.get("configVersion").getAsInt();
			}
			try {
				settings = gson.fromJson(root, RslSettings.class);
				if (settings == null) {
					settings = defaults();
				}
				if (!root.has("allowedBlocks") || root.get("allowedBlocks").isJsonNull()) {
					settings.allowedBlocks = new ArrayList<>(DefaultBlocks.load());
					writeSettings = true;
				}
				if (version < RslConstants.CONFIG_VERSION) {
					migrated = true;
					writeSettings = true;
					writeState = true;
					Path backup = backupV1(settingsPath);
					warnings.add("Migrated config v" + version + " to v" + RslConstants.CONFIG_VERSION
							+ "; backup: " + backup.getFileName());

					if (!root.has("defaultArea") && root.has("areas")) {
						AreaSpec[] oldAreas = gson.fromJson(root.get("areas"), AreaSpec[].class);
						if (oldAreas != null && oldAreas.length > 0) {
							settings.defaultArea = oldAreas[0];
							for (int i = 1; i < oldAreas.length; i++) {
								if (oldAreas[i] != null) {
									legacySaveAreas.add(oldAreas[i]);
								}
							}
						}
					} else if (root.has("defaultArea") && root.has("areas")) {
						warnings.add("Legacy 'areas' field ignored because 'defaultArea' is present");
					}

					if (root.has("isRunning")) {
						state.isRunning = root.get("isRunning").getAsBoolean();
					}
					if (root.has("secondsRemaining")) {
						state.secondsRemaining = root.get("secondsRemaining").getAsInt();
					}
				}
			} catch (RuntimeException e) {
				Path backup = moveToBackup(settingsPath, "randomskyblockisland.corrupt-");
				warnings.add("Settings file could not be parsed (" + e.getMessage()
						+ "); backed up to " + backup.getFileName());
				settings = defaults();
				writeSettings = true;
				writeState = true;
			}
		}

		settings.sanitize();
		state.sanitize();

		Path statePath = statePath();
		if (Files.exists(statePath)) {
			try {
				RslState loaded = gson.fromJson(Files.readString(statePath, StandardCharsets.UTF_8), RslState.class);
				if (loaded != null) {
					state = loaded;
					state.sanitize();
				}
			} catch (RuntimeException e) {
				Path backup = moveToBackup(statePath, "randomskyblockisland.state.corrupt-");
				warnings.add("State file was corrupt; backed up to " + backup.getFileName());
				writeState = true;
			}
		} else {
			writeState = true;
		}

		if (migrated || writeSettings) {
			saveSettings(settings);
		}
		if (migrated || writeState) {
			saveState(state);
		}

		LoadResult result = new LoadResult();
		result.settings = settings;
		result.state = state;
		result.legacySaveAreas = legacySaveAreas;
		result.migrated = migrated;
		result.warnings = warnings;
		return result;
	}

	/**
	 * Reads the settings file again without touching the state file or performing migration
	 * writes. Used by the in-game reload command; the caller decides whether the result is
	 * safe to apply.
	 */
	public RslSettings reloadSettings() throws IOException {
		Path path = settingsPath();
		if (!Files.exists(path)) {
			return defaults();
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			RslSettings settings = gson.fromJson(root, RslSettings.class);
			if (settings == null) {
				settings = defaults();
			}
			if (!root.has("allowedBlocks") || root.get("allowedBlocks").isJsonNull()) {
				settings.allowedBlocks = new ArrayList<>(DefaultBlocks.load());
			}
			settings.sanitize();
			return settings;
		} catch (RuntimeException e) {
			throw new IOException("Invalid settings file: " + e.getMessage(), e);
		}
	}

	public void saveSettings(RslSettings settings) throws IOException {
		RslSettings copy = settings.copy();
		copy.sanitize();
		writeJson(settingsPath(), copy);
	}

	public void saveState(RslState state) throws IOException {
		RslState copy = state.copy();
		copy.sanitize();
		writeJson(statePath(), copy);
	}

	public AreasLoadResult loadAreas(Path saveDir) throws IOException {
		Path path = areasPath(saveDir);
		if (!Files.exists(path)) {
			return new AreasLoadResult(new AreasFile(), false, null);
		}
		try {
			AreasFile file = gson.fromJson(Files.readString(path, StandardCharsets.UTF_8), AreasFile.class);
			if (file == null) {
				file = new AreasFile();
			}
			file.sanitize();
			return new AreasLoadResult(file, false, null);
		} catch (RuntimeException e) {
			Path backup = moveToBackup(path, "rsl_areas.corrupt-");
			return new AreasLoadResult(new AreasFile(), true,
					"Areas file was corrupt; backed up to " + backup.getFileName());
		}
	}

	public void saveAreas(Path saveDir, AreasFile areas) throws IOException {
		AreasFile copy = areas.copy();
		copy.sanitize();
		writeJson(areasPath(saveDir), copy);
	}

	private RslSettings defaults() {
		RslSettings settings = new RslSettings();
		settings.allowedBlocks = new ArrayList<>(DefaultBlocks.load());
		settings.sanitize();
		return settings;
	}

	private Path backupV1(Path settingsPath) throws IOException {
		Path backup = configDir.resolve(RslConstants.BACKUP_FILE);
		if (Files.exists(backup)) {
			backup = configDir.resolve("randomskyblockisland.v1.bak." + timestamp() + ".json");
		}
		Files.copy(settingsPath, backup, StandardCopyOption.COPY_ATTRIBUTES);
		return backup;
	}

	private Path moveToBackup(Path source, String prefix) throws IOException {
		Path parent = source.getParent() != null ? source.getParent() : configDir;
		Path backup = parent.resolve(prefix + timestamp() + ".json");
		Files.move(source, backup, StandardCopyOption.REPLACE_EXISTING);
		return backup;
	}

	private void writeJson(Path path, Object value) throws IOException {
		Files.createDirectories(path.getParent());
		Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
		Files.writeString(tmp, gson.toJson(value) + System.lineSeparator(), StandardCharsets.UTF_8);
		try {
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static String timestamp() {
		return LocalDateTime.now().format(TIMESTAMP);
	}

	public static final class LoadResult {
		public RslSettings settings;
		public RslState state;
		public List<AreaSpec> legacySaveAreas;
		public boolean migrated;
		public List<String> warnings;
	}

	public static final class AreasLoadResult {
		public final AreasFile areas;
		public final boolean recovered;
		public final String message;

		public AreasLoadResult(AreasFile areas, boolean recovered, String message) {
			this.areas = areas;
			this.recovered = recovered;
			this.message = message;
		}
	}
}
