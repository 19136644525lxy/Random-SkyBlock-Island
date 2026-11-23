package yifei.rsl.config;

import yifei.rsl.RandomSkyBlockIsland;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModConfigScreen {
	public static Screen create(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Text.translatable("rsl.config.title"));
		
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();
		ConfigCategory settings = builder.getOrCreateCategory(Text.translatable("rsl.config.category.settings"));
		
		settings.addEntry(entryBuilder.startIntField(
				Text.translatable("rsl.config.transform_delay"),
				ModConfig.getTransformDelaySeconds())
			.setDefaultValue(180)
			.setTooltip(Text.translatable("rsl.config.transform_delay.tooltip"))
			.setSaveConsumer(ModConfig::setTransformDelaySeconds)
			.build()
		);
		
		builder.setSavingRunnable(() -> {
			ModConfig.saveSettings();
			RandomSkyBlockIsland.LOGGER.info("Config saved");
		});
		
		return builder.build();
	}
}
