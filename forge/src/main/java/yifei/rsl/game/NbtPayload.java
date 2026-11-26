package yifei.rsl.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Forge mirror of the Fabric NbtPayload: block-entity NBT to/from Base64 with the
 * position/id metadata stripped so it can be re-applied anywhere.
 */
public final class NbtPayload {
	private NbtPayload() {
	}

	public static String encode(CompoundTag tag) {
		if (tag == null || tag.isEmpty()) {
			return "";
		}
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
			NbtIo.writeCompressed(tag, bytes);
			return Base64.getEncoder().encodeToString(bytes.toByteArray());
		} catch (Exception e) {
			return "";
		}
	}

	public static boolean apply(BlockEntity blockEntity, String base64) {
		if (blockEntity == null || base64 == null || base64.isEmpty()) {
			return false;
		}
		try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(base64))) {
			CompoundTag tag = NbtIo.readCompressed(bytes);
			if (tag == null) {
				return false;
			}
			tag.remove("id");
			tag.remove("x");
			tag.remove("y");
			tag.remove("z");
			blockEntity.load(tag);
			blockEntity.setChanged();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static CompoundTag decode(String base64) {
		if (base64 == null || base64.isEmpty()) {
			return null;
		}
		try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(base64))) {
			return NbtIo.readCompressed(bytes);
		} catch (Exception e) {
			return null;
		}
	}
}
