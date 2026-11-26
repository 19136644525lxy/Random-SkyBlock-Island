package yifei.rsl.game;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Serializes a block state as {@code id[k=v,...]} so the snapshot file can keep
 * orientation/slab/waterlog properties without depending on NBT helpers.
 */
public final class BlockStateCodec {
	private BlockStateCodec() {
	}

	public static String encode(BlockState state) {
		ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
		if (id == null) {
			id = ResourceLocation.tryParse("minecraft:air");
		}
		String base = id == null ? "minecraft:air" : id.toString();
		Map<Property<?>, Comparable<?>> values = state.getValues();
		if (values.isEmpty()) {
			return base;
		}
		StringJoiner joiner = new StringJoiner(",", "[", "]");
		values.entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getKey().getName()))
				.forEach(entry -> joiner.add(entry.getKey().getName() + "=" + valueName(entry.getValue())));
		return base + joiner;
	}

	public static BlockState decode(String encoded) {
		int bracket = encoded.indexOf('[');
		String id = bracket < 0 ? encoded : encoded.substring(0, bracket);
		ResourceLocation location = ResourceLocation.tryParse(id);
		Block block = location == null ? null : ForgeRegistries.BLOCKS.getValue(location);
		if (block == null) {
			block = Blocks.STONE;
		}
		BlockState state = block.defaultBlockState();
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
				Optional<?> parsed = property.getValue(value);
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
		return state.setValue(property, (Comparable) value);
	}
}
