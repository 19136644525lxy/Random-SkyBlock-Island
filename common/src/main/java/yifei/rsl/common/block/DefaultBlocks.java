package yifei.rsl.common.block;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads the default block pool from the classpath. The same resource is packaged in
 * both the Fabric and the Forge jar.
 */
public final class DefaultBlocks {
	public static final String RESOURCE = "/rsl/default_blocks.json";

	private DefaultBlocks() {
	}

	public static List<String> load() {
		try (InputStream in = DefaultBlocks.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing default block resource: " + RESOURCE);
			}
			return load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + RESOURCE, e);
		}
	}

	public static List<String> load(InputStream in) throws IOException {
		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			String[] raw = new Gson().fromJson(reader, String[].class);
			if (raw == null || raw.length == 0) {
				throw new IllegalArgumentException("Default block resource is empty: " + RESOURCE);
			}

			Set<String> unique = new LinkedHashSet<>();
			for (String entry : raw) {
				String normalized = BlockId.normalize(entry);
				if (normalized == null) {
					throw new IllegalArgumentException("Invalid default block id: " + entry);
				}
				unique.add(normalized);
			}
			if (unique.size() != raw.length) {
				throw new IllegalArgumentException("Duplicate ids in default block resource: " + RESOURCE);
			}
			return List.copyOf(new ArrayList<>(unique));
		}
	}
}
