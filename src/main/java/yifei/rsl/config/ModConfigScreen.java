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
				ModConfig.transformDelaySeconds)
			.setDefaultValue(180)
			.setTooltip(Text.translatable("rsl.config.transform_delay.tooltip"))
			.setSaveConsumer(newValue -> ModConfig.transformDelaySeconds = newValue)
			.build()
		);
		
		if (!ModConfig.areas.isEmpty()) {
			ConfigCategory area = builder.getOrCreateCategory(Text.translatable("rsl.config.category.area"));
			TransformArea firstArea = ModConfig.areas.get(0);
			
			area.addEntry(entryBuilder.startIntField(
					Text.translatable("rsl.config.min_x"),
					firstArea.minX)
				.setDefaultValue(-1)
				.setSaveConsumer(newValue -> firstArea.minX = newValue)
				.build()
			);
			
			area.addEntry(entryBuilder.startIntField(
					Text.translatable("rsl.config.min_y"),
					firstArea.minY)
				.setDefaultValue(64)
				.setSaveConsumer(newValue -> firstArea.minY = newValue)
				.build()
			);
			
			area.addEntry(entryBuilder.startIntField(
					Text.translatable("rsl.config.min_z"),
					firstArea.minZ)
				.setDefaultValue(-4)
				.setSaveConsumer(newValue -> firstArea.minZ = newValue)
				.build()
			);
			
			area.addEntry(entryBuilder.startIntField(
					Text.translatable("rsl.config.max_x"),
					firstArea.maxX)
				.setDefaultValue(4)
				.setSaveConsumer(newValue -> firstArea.maxX = newValue)
				.build()
			);
			
			area.addEntry(entryBuilder.startIntField(
					Text.translatable("rsl.config.max_y"),
					firstArea.maxY)
				.setDefaultValue(66)
				.setSaveConsumer(newValue -> firstArea.maxY = newValue)
				.build()
			);
			
			area.addEntry(entryBuilder.startIntField(
					Text.translatable("rsl.config.max_z"),
					firstArea.maxZ)
				.setDefaultValue(1)
				.setSaveConsumer(newValue -> firstArea.maxZ = newValue)
				.build()
			);
		}
		
		builder.setSavingRunnable(() -> {
			ModConfig.saveConfig();
			RandomSkyBlockIsland.LOGGER.info("Config saved");
		});
		
		return builder.build();
	}
}
