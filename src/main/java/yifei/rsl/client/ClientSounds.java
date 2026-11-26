package yifei.rsl.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import yifei.rsl.config.ModConfig;

/**
 * Plays the mod's UI feedback sounds on the client, honouring the sound toggle and
 * master volume from the shared settings. All sounds are vanilla events, so no
 * extra assets are needed.
 */
public final class ClientSounds {
    private ClientSounds() {
    }

    public static void play(SoundEvent event, float volume, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
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
