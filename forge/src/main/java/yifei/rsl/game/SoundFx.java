package yifei.rsl.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import yifei.rsl.config.ModConfig;

/**
 * Forge mirror of the Fabric SoundFx.
 */
public final class SoundFx {
    private SoundFx() {
    }

    public static void play(ServerPlayer player, SoundEvent event, float volume, float pitch) {
        if (player == null || !ModConfig.isLoaded() || !ModConfig.isSoundEnabled()) {
            return;
        }
        double scale = ModConfig.getSoundVolume();
        if (scale <= 0.0) {
            return;
        }
        player.playSound(event, (float) (volume * scale), pitch);
    }
}
