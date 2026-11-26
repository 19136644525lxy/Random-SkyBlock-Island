package yifei.rsl.game;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Base64;

/**
 * Turns a block-entity's NBT into a Base64 string and back. The payload stored in
 * the pool is written without the coordinates/id so it can be re-applied at any
 * block position.
 */
public final class NbtPayload {
	private NbtPayload() {
	}

	public static String encode(NbtCompound nbt) {
		if (nbt == null || nbt.isEmpty()) {
			return "";
		}
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			 DataOutputStream out = new DataOutputStream(bytes)) {
			NbtIo.write(nbt, out);
			return Base64.getEncoder().encodeToString(bytes.toByteArray());
		} catch (Exception e) {
			return "";
		}
	}

	public static NbtCompound decode(String base64) {
		if (base64 == null || base64.isEmpty()) {
			return null;
		}
		try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
			 DataInputStream in = new DataInputStream(bytes)) {
			return NbtIo.read(in);
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean apply(BlockEntity blockEntity, String base64) {
		if (blockEntity == null || base64 == null || base64.isEmpty()) {
			return false;
		}
		try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
			 DataInputStream in = new DataInputStream(bytes)) {
			NbtCompound nbt = NbtIo.read(in);
			if (nbt == null) {
				return false;
			}
			nbt.remove("id");
			nbt.remove("x");
			nbt.remove("y");
			nbt.remove("z");
			blockEntity.readNbt(nbt);
			blockEntity.markDirty();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
