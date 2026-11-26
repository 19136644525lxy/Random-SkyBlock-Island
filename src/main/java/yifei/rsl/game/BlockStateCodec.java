package yifei.rsl.game;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Serializes a block state as {@code id[k=v,...]} so the snapshot file can keep
 * orientation/slab/waterlog properties without depending on NBT helpers. The
 * {@link SnapshotStorage} pairs this with {@code Registries.BLOCK}.
 */
public final class BlockStateCodec {
	private BlockStateCodec() {
	}

	public static String encode(BlockState state) {
		String id = Registries.BLOCK.getId(state.getBlock()).toString();
		Map<Property<?>, Comparable<?>> entries = state.getEntries();
		if (entries.isEmpty()) {
			return id;
		}
		StringJoiner joiner = new StringJoiner(",", "[", "]");
		entries.entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getKey().getName()))
				.forEach(entry -> joiner.add(entry.getKey().getName() + "=" + valueName(entry.getValue())));
		return id + joiner;
	}

	public static BlockState decode(String encoded) {
		int bracket = encoded.indexOf('[');
		String id = bracket < 0 ? encoded : encoded.substring(0, bracket);
		Block block = Registries.BLOCK.get(Identifier.tryParse(id));
		BlockState state = block.getDefaultState();
		if (bracket < 0) {
			return state;
		}
		String body = encoded.substring(bracket + 1, encoded.endsWith("]") ? encoded.length() - 1 : encoded.length());
		if (body.isEmpty()) {
			return state;
		}
		for (String part : body.split(",")) {
			int equals = part.indexOf('=');
			if (equals <= 0 || equals == part.length() - 1) {
				continue;
			}
			String name = part.substring(0, equals);
			String value = part.substring(equals + 1);
			for (Property<?> property : state.getProperties()) {
				if (!property.getName().equals(name)) {
					continue;
				}
				Optional<?> parsed = property.parse(value);
				if (parsed.isPresent()) {
					state = with(state, property, parsed.get());
				}
				break;
			}
		}
		return state;
	}

	private static String valueName(Object value) {
		return value instanceof Enum<?> enumeration ? enumeration.name() : String.valueOf(value);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static BlockState with(BlockState state, Property property, Object value) {
		return state.with(property, (Comparable) value);
	}
}
