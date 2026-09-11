package yifei.rsl.config;

import yifei.rsl.RandomSkyBlockIsland;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Cloth Config 配置界面构建类
 *
 * 移植说明：
 * - Text → Component
 * - net.minecraft.client.gui.screen.Screen → net.minecraft.client.gui.screens.Screen
 * - Cloth Config API 包名在 Forge/Fabric 上一致（me.shedaniel.clothconfig2.api）
 *
 * 该类仅在客户端被调用（通过 Forge ConfigScreenHandler），服务端不会加载此类。
 */
public class ModConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("rsl.config.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory settings = builder.getOrCreateCategory(Component.translatable("rsl.config.category.settings"));

        settings.addEntry(entryBuilder.startIntField(
                Component.translatable("rsl.config.transform_delay"),
                ModConfig.transformDelaySeconds)
            .setDefaultValue(180)
            .setTooltip(Component.translatable("rsl.config.transform_delay.tooltip"))
            .setSaveConsumer(newValue -> ModConfig.transformDelaySeconds = newValue)
            .build()
        );

        builder.setSavingRunnable(() -> {
            ModConfig.saveConfig();
            RandomSkyBlockIsland.LOGGER.info("Config saved");
        });

        return builder.build();
    }
}
