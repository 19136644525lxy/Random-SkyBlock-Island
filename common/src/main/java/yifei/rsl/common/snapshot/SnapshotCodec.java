package yifei.rsl.common.snapshot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Binary (reader/writer) format for {@link RegionSnapshot}. Uses a compact
 * palette + index layout so a default-sized area (65,536 blocks) costs roughly
 * 256 KB instead of megabytes of JSON.
 */
public final class SnapshotCodec {
	private static final int MAGIC = 0x52534C53; // "RSLS"
	private static final int VERSION = 1;
	private static final int MAX_AREAS = 4096;
	private static final int MAX_PALETTE = 65536;

	private SnapshotCodec() {
	}

	public static void save(Path file, RegionSnapshot snapshot) throws IOException {
		Path parent = file.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
			out.writeInt(MAGIC);
			out.writeInt(VERSION);
			out.writeUTF(snapshot.dimension());
			List<SnapshotArea> areas = snapshot.areas();
			out.writeInt(areas.size());
			for (SnapshotArea area : areas) {
				out.writeUTF(area.name());
				out.writeInt(area.minX());
				out.writeInt(area.minY());
				out.writeInt(area.minZ());
				out.writeInt(area.sizeX());
				out.writeInt(area.sizeY());
				out.writeInt(area.sizeZ());
				List<String> palette = area.palette();
				out.writeInt(palette.size());
				for (String entry : palette) {
					out.writeUTF(entry);
				}
				int[] indices = area.indices();
				out.writeInt(indices.length);
				for (int index : indices) {
					out.writeInt(index);
				}
			}
		}
	}

	public static RegionSnapshot load(Path file) throws IOException {
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
			if (in.readInt() != MAGIC) {
				throw new IOException("not a Random SkyBlock Island snapshot file");
			}
			int version = in.readInt();
			if (version != VERSION) {
				throw new IOException("unsupported snapshot version " + version);
			}
			String dimension = in.readUTF();
			int areaCount = in.readInt();
			if (areaCount <= 0 || areaCount > MAX_AREAS) {
				throw new IOException("invalid snapshot area count " + areaCount);
			}
			List<SnapshotArea> areas = new ArrayList<>(areaCount);
			for (int i = 0; i < areaCount; i++) {
				String name = in.readUTF();
				int minX = in.readInt();
				int minY = in.readInt();
				int minZ = in.readInt();
				int sizeX = in.readInt();
				int sizeY = in.readInt();
				int sizeZ = in.readInt();
				int paletteCount = in.readInt();
				if (paletteCount <= 0 || paletteCount > MAX_PALETTE) {
					throw new IOException("invalid snapshot palette count " + paletteCount);
				}
				List<String> palette = new ArrayList<>(paletteCount);
				for (int p = 0; p < paletteCount; p++) {
					palette.add(in.readUTF());
				}
				int indexCount = in.readInt();
				int[] indices = new int[indexCount];
				for (int p = 0; p < indexCount; p++) {
					indices[p] = in.readInt();
				}
				areas.add(new SnapshotArea(name, minX, minY, minZ, sizeX, sizeY, sizeZ, palette, indices));
			}
			return new RegionSnapshot(dimension, areas);
		}
	}
}
