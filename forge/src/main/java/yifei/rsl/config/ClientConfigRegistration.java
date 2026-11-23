package yifei.rsl.config;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Client-only bridge for the optional Cloth Config screen.
 *
 * <p>This class is loaded through {@code DistExecutor} so a dedicated server never resolves
 * the client-only Forge {@link ConfigScreenHandler} type. The Cloth-backed screen itself is
 * only touched inside the factory lambda, which runs after the mod-presence check.</p>
 */
public final class ClientConfigRegistration {
    private ClientConfigRegistration() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        if (!ModList.get().isLoaded("cloth_config")) {
            return;
        }
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> ModConfigScreen.create(parent))
        );
    }
}
