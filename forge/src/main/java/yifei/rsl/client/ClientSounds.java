package yifei.rsl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import yifei.rsl.config.ModConfig;

/**
 * Forge mirror of the Fabric ClientSounds.
 */
public final class ClientSounds {
    private ClientSounds() {
    }

    public static void play(SoundEvent event, float volume, float pitch) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ModConfig.isLoaded() || !ModConfig.isSoundEnabled()) {
            return;
        }
        double scale = ModConfig.getSoundVolume();
        if (scale <= 0.0) {
            return;
        }
        client.player.playSound(event, (float) (volume * scale), pitch);
    }
}
