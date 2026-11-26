package yifei.rsl.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import yifei.rsl.config.ModConfig;

/**
 * Server-side sound helper: sends a vanilla sound to one player with the configured
 * master volume applied.
 */
public final class SoundFx {
    private SoundFx() {
    }

    public static void play(ServerPlayerEntity player, SoundEvent event, float volume, float pitch) {
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
