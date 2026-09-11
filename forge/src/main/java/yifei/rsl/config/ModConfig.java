package yifei.rsl.config;

import yifei.rsl.RandomSkyBlockIsland;
import yifei.rsl.game.IslandGame;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 模组配置管理类
 *
 * 移植说明：
 * - Registries.BLOCK → ForgeRegistries.BLOCKS
 * - Identifier → ResourceLocation
 * - Registries.BLOCK.getId → ForgeRegistries.BLOCKS.getKey
 * - Registries.BLOCK.get → ForgeRegistries.BLOCKS.getValue
 * - block.getDefaultState() → block.defaultBlockState()
 * - 使用 FMLPaths.CONFIGDIR 获取正确配置目录（避免相对路径写入错误位置）
 *
 * 配置系统保留原 JSON 格式，与 Fabric 版完全一致，确保两个版本配置文件可互通。
 */
public class ModConfig {
    private static final String CONFIG_FILE = "randomskyblockisland.json";

    public static int transformDelaySeconds = 180;
    public static int secondsRemaining = 0;
    public static boolean isRunning = false;
    // 默认区域：存全局配置（与原版行为一致）
    public static TransformArea defaultArea = new TransformArea("default", -1, 64, -4, 4, 66, 1);
    // 额外区域：存档级配置（每个存档独立）
    public static List<TransformArea> saveAreas = new ArrayList<>();
    // 存档目录，用于读写 saveAreas
    private static Path saveDir = null;
    public static List<Block> allowedBlocks = new ArrayList<>();

    static {
        addDefaultBlocks();
    }

    /** 合并所有区域（游戏逻辑用） */
    public static List<TransformArea> getAllAreas() {
        List<TransformArea> all = new ArrayList<>();
        all.add(defaultArea);
        all.addAll(saveAreas);
        return all;
    }

    public static void setSaveDir(Path dir) {
        saveDir = dir;
    }

    private static void addDefaultBlocks() {
        allowedBlocks.add(Blocks.OAK_LOG);
        allowedBlocks.add(Blocks.OAK_PLANKS);
        allowedBlocks.add(Blocks.OAK_LEAVES);
        allowedBlocks.add(Blocks.OAK_WOOD);
        allowedBlocks.add(Blocks.STRIPPED_OAK_LOG);
        allowedBlocks.add(Blocks.STRIPPED_OAK_WOOD);

        allowedBlocks.add(Blocks.SPRUCE_LOG);
        allowedBlocks.add(Blocks.SPRUCE_PLANKS);
        allowedBlocks.add(Blocks.SPRUCE_LEAVES);
        allowedBlocks.add(Blocks.SPRUCE_WOOD);
        allowedBlocks.add(Blocks.STRIPPED_SPRUCE_LOG);
        allowedBlocks.add(Blocks.STRIPPED_SPRUCE_WOOD);

        allowedBlocks.add(Blocks.BIRCH_LOG);
        allowedBlocks.add(Blocks.BIRCH_PLANKS);
        allowedBlocks.add(Blocks.BIRCH_LEAVES);
        allowedBlocks.add(Blocks.BIRCH_WOOD);
        allowedBlocks.add(Blocks.STRIPPED_BIRCH_LOG);
        allowedBlocks.add(Blocks.STRIPPED_BIRCH_WOOD);

        allowedBlocks.add(Blocks.JUNGLE_LOG);
        allowedBlocks.add(Blocks.JUNGLE_PLANKS);
        allowedBlocks.add(Blocks.JUNGLE_LEAVES);
        allowedBlocks.add(Blocks.JUNGLE_WOOD);
        allowedBlocks.add(Blocks.STRIPPED_JUNGLE_LOG);
        allowedBlocks.add(Blocks.STRIPPED_JUNGLE_WOOD);

        allowedBlocks.add(Blocks.ACACIA_LOG);
        allowedBlocks.add(Blocks.ACACIA_PLANKS);
        allowedBlocks.add(Blocks.ACACIA_LEAVES);
        allowedBlocks.add(Blocks.ACACIA_WOOD);
        allowedBlocks.add(Blocks.STRIPPED_ACACIA_LOG);
        allowedBlocks.add(Blocks.STRIPPED_ACACIA_WOOD);

        allowedBlocks.add(Blocks.DARK_OAK_LOG);
        allowedBlocks.add(Blocks.DARK_OAK_PLANKS);
        allowedBlocks.add(Blocks.DARK_OAK_LEAVES);
        allowedBlocks.add(Blocks.DARK_OAK_WOOD);
        allowedBlocks.add(Blocks.STRIPPED_DARK_OAK_LOG);
        allowedBlocks.add(Blocks.STRIPPED_DARK_OAK_WOOD);

        allowedBlocks.add(Blocks.CRIMSON_STEM);
        allowedBlocks.add(Blocks.CRIMSON_PLANKS);
        allowedBlocks.add(Blocks.CRIMSON_HYPHAE);
        allowedBlocks.add(Blocks.CRIMSON_NYLIUM);
        allowedBlocks.add(Blocks.STRIPPED_CRIMSON_STEM);
        allowedBlocks.add(Blocks.STRIPPED_CRIMSON_HYPHAE);

        allowedBlocks.add(Blocks.WARPED_STEM);
        allowedBlocks.add(Blocks.WARPED_PLANKS);
        allowedBlocks.add(Blocks.WARPED_HYPHAE);
        allowedBlocks.add(Blocks.WARPED_NYLIUM);
        allowedBlocks.add(Blocks.STRIPPED_WARPED_STEM);
        allowedBlocks.add(Blocks.STRIPPED_WARPED_HYPHAE);

        allowedBlocks.add(Blocks.COAL_ORE);
        allowedBlocks.add(Blocks.IRON_ORE);
        allowedBlocks.add(Blocks.GOLD_ORE);
        allowedBlocks.add(Blocks.DIAMOND_ORE);
        allowedBlocks.add(Blocks.REDSTONE_ORE);
        allowedBlocks.add(Blocks.LAPIS_ORE);
        allowedBlocks.add(Blocks.EMERALD_ORE);
        allowedBlocks.add(Blocks.NETHER_GOLD_ORE);
        allowedBlocks.add(Blocks.NETHER_QUARTZ_ORE);
        allowedBlocks.add(Blocks.ANCIENT_DEBRIS);

        allowedBlocks.add(Blocks.COAL_BLOCK);
        allowedBlocks.add(Blocks.IRON_BLOCK);
        allowedBlocks.add(Blocks.GOLD_BLOCK);
        allowedBlocks.add(Blocks.DIAMOND_BLOCK);
        allowedBlocks.add(Blocks.EMERALD_BLOCK);
        allowedBlocks.add(Blocks.LAPIS_BLOCK);
        allowedBlocks.add(Blocks.REDSTONE_BLOCK);
        allowedBlocks.add(Blocks.NETHERITE_BLOCK);

        allowedBlocks.add(Blocks.STONE);
        allowedBlocks.add(Blocks.COBBLESTONE);
        allowedBlocks.add(Blocks.STONE_BRICKS);
        allowedBlocks.add(Blocks.CHISELED_STONE_BRICKS);
        allowedBlocks.add(Blocks.CRACKED_STONE_BRICKS);
        allowedBlocks.add(Blocks.MOSSY_STONE_BRICKS);
        allowedBlocks.add(Blocks.MOSSY_COBBLESTONE);
        allowedBlocks.add(Blocks.GRANITE);
        allowedBlocks.add(Blocks.POLISHED_GRANITE);
        allowedBlocks.add(Blocks.DIORITE);
        allowedBlocks.add(Blocks.POLISHED_DIORITE);
        allowedBlocks.add(Blocks.ANDESITE);
        allowedBlocks.add(Blocks.POLISHED_ANDESITE);

        allowedBlocks.add(Blocks.NETHERRACK);
        allowedBlocks.add(Blocks.NETHER_BRICKS);
        allowedBlocks.add(Blocks.RED_NETHER_BRICKS);
        allowedBlocks.add(Blocks.CHISELED_NETHER_BRICKS);
        allowedBlocks.add(Blocks.CRACKED_NETHER_BRICKS);
        allowedBlocks.add(Blocks.BASALT);
        allowedBlocks.add(Blocks.POLISHED_BASALT);
        allowedBlocks.add(Blocks.BLACKSTONE);
        allowedBlocks.add(Blocks.POLISHED_BLACKSTONE);
        allowedBlocks.add(Blocks.POLISHED_BLACKSTONE_BRICKS);
        allowedBlocks.add(Blocks.CHISELED_POLISHED_BLACKSTONE);
        allowedBlocks.add(Blocks.GILDED_BLACKSTONE);
        allowedBlocks.add(Blocks.MAGMA_BLOCK);
        allowedBlocks.add(Blocks.SOUL_SAND);
        allowedBlocks.add(Blocks.SOUL_SOIL);

        allowedBlocks.add(Blocks.END_STONE);
        allowedBlocks.add(Blocks.END_STONE_BRICKS);
        allowedBlocks.add(Blocks.OBSIDIAN);
        allowedBlocks.add(Blocks.CRYING_OBSIDIAN);
        allowedBlocks.add(Blocks.PURPUR_BLOCK);
        allowedBlocks.add(Blocks.PURPUR_PILLAR);

        allowedBlocks.add(Blocks.BRICKS);
        allowedBlocks.add(Blocks.SANDSTONE);
        allowedBlocks.add(Blocks.SMOOTH_SANDSTONE);
        allowedBlocks.add(Blocks.CHISELED_SANDSTONE);
        allowedBlocks.add(Blocks.CUT_SANDSTONE);
        allowedBlocks.add(Blocks.RED_SANDSTONE);
        allowedBlocks.add(Blocks.SMOOTH_RED_SANDSTONE);
        allowedBlocks.add(Blocks.CHISELED_RED_SANDSTONE);
        allowedBlocks.add(Blocks.CUT_RED_SANDSTONE);
        allowedBlocks.add(Blocks.QUARTZ_BLOCK);
        allowedBlocks.add(Blocks.SMOOTH_QUARTZ);
        allowedBlocks.add(Blocks.CHISELED_QUARTZ_BLOCK);
        allowedBlocks.add(Blocks.QUARTZ_BRICKS);
        allowedBlocks.add(Blocks.QUARTZ_PILLAR);
        allowedBlocks.add(Blocks.PRISMARINE);
        allowedBlocks.add(Blocks.PRISMARINE_BRICKS);
        allowedBlocks.add(Blocks.DARK_PRISMARINE);
        allowedBlocks.add(Blocks.SMOOTH_STONE);
        allowedBlocks.add(Blocks.TERRACOTTA);
        allowedBlocks.add(Blocks.BONE_BLOCK);
        allowedBlocks.add(Blocks.CLAY);
        allowedBlocks.add(Blocks.GLASS);

        allowedBlocks.add(Blocks.GRASS_BLOCK);
        allowedBlocks.add(Blocks.DIRT);
        allowedBlocks.add(Blocks.COARSE_DIRT);
        allowedBlocks.add(Blocks.PODZOL);
        allowedBlocks.add(Blocks.MYCELIUM);
        allowedBlocks.add(Blocks.BEDROCK);
        allowedBlocks.add(Blocks.SNOW_BLOCK);
        allowedBlocks.add(Blocks.ICE);
        allowedBlocks.add(Blocks.PACKED_ICE);
        allowedBlocks.add(Blocks.BLUE_ICE);
        allowedBlocks.add(Blocks.FROSTED_ICE);
        allowedBlocks.add(Blocks.HAY_BLOCK);
        allowedBlocks.add(Blocks.MELON);
        allowedBlocks.add(Blocks.PUMPKIN);
        allowedBlocks.add(Blocks.CARVED_PUMPKIN);
        allowedBlocks.add(Blocks.JACK_O_LANTERN);
        allowedBlocks.add(Blocks.SHROOMLIGHT);
        allowedBlocks.add(Blocks.BROWN_MUSHROOM_BLOCK);
        allowedBlocks.add(Blocks.RED_MUSHROOM_BLOCK);
        allowedBlocks.add(Blocks.MUSHROOM_STEM);

        allowedBlocks.add(Blocks.BRAIN_CORAL_BLOCK);
        allowedBlocks.add(Blocks.BUBBLE_CORAL_BLOCK);
        allowedBlocks.add(Blocks.FIRE_CORAL_BLOCK);
        allowedBlocks.add(Blocks.HORN_CORAL_BLOCK);
        allowedBlocks.add(Blocks.TUBE_CORAL_BLOCK);

        allowedBlocks.add(Blocks.FURNACE);
        allowedBlocks.add(Blocks.BLAST_FURNACE);
        allowedBlocks.add(Blocks.SMOKER);
        allowedBlocks.add(Blocks.CRAFTING_TABLE);
        allowedBlocks.add(Blocks.DISPENSER);
        allowedBlocks.add(Blocks.DROPPER);
        allowedBlocks.add(Blocks.PISTON);
        allowedBlocks.add(Blocks.STICKY_PISTON);
        allowedBlocks.add(Blocks.OBSERVER);
        allowedBlocks.add(Blocks.NOTE_BLOCK);
        allowedBlocks.add(Blocks.JUKEBOX);
        allowedBlocks.add(Blocks.BEACON);
        allowedBlocks.add(Blocks.RESPAWN_ANCHOR);
        allowedBlocks.add(Blocks.LODESTONE);
        allowedBlocks.add(Blocks.SHULKER_BOX);
        allowedBlocks.add(Blocks.BARREL);
        allowedBlocks.add(Blocks.SMITHING_TABLE);
        allowedBlocks.add(Blocks.CARTOGRAPHY_TABLE);
        allowedBlocks.add(Blocks.FLETCHING_TABLE);
        allowedBlocks.add(Blocks.LOOM);
        allowedBlocks.add(Blocks.TARGET);

        allowedBlocks.add(Blocks.GLOWSTONE);
        allowedBlocks.add(Blocks.SEA_LANTERN);
        allowedBlocks.add(Blocks.REDSTONE_LAMP);
        allowedBlocks.add(Blocks.SPONGE);
        allowedBlocks.add(Blocks.WET_SPONGE);
        allowedBlocks.add(Blocks.BEE_NEST);
        allowedBlocks.add(Blocks.BEEHIVE);
        allowedBlocks.add(Blocks.HONEY_BLOCK);
        allowedBlocks.add(Blocks.HONEYCOMB_BLOCK);
        allowedBlocks.add(Blocks.SLIME_BLOCK);
        allowedBlocks.add(Blocks.DRIED_KELP_BLOCK);
        allowedBlocks.add(Blocks.NETHER_WART_BLOCK);
        allowedBlocks.add(Blocks.WARPED_WART_BLOCK);
        allowedBlocks.add(Blocks.TNT);
        allowedBlocks.add(Blocks.BOOKSHELF);

        allowedBlocks.add(Blocks.WHITE_WOOL);
        allowedBlocks.add(Blocks.ORANGE_WOOL);
        allowedBlocks.add(Blocks.MAGENTA_WOOL);
        allowedBlocks.add(Blocks.LIGHT_BLUE_WOOL);
        allowedBlocks.add(Blocks.YELLOW_WOOL);
        allowedBlocks.add(Blocks.LIME_WOOL);
        allowedBlocks.add(Blocks.PINK_WOOL);
        allowedBlocks.add(Blocks.GRAY_WOOL);
        allowedBlocks.add(Blocks.LIGHT_GRAY_WOOL);
        allowedBlocks.add(Blocks.CYAN_WOOL);
        allowedBlocks.add(Blocks.PURPLE_WOOL);
        allowedBlocks.add(Blocks.BLUE_WOOL);
        allowedBlocks.add(Blocks.BROWN_WOOL);
        allowedBlocks.add(Blocks.GREEN_WOOL);
        allowedBlocks.add(Blocks.RED_WOOL);
        allowedBlocks.add(Blocks.BLACK_WOOL);

        allowedBlocks.add(Blocks.WHITE_CONCRETE);
        allowedBlocks.add(Blocks.ORANGE_CONCRETE);
        allowedBlocks.add(Blocks.MAGENTA_CONCRETE);
        allowedBlocks.add(Blocks.LIGHT_BLUE_CONCRETE);
        allowedBlocks.add(Blocks.YELLOW_CONCRETE);
        allowedBlocks.add(Blocks.LIME_CONCRETE);
        allowedBlocks.add(Blocks.PINK_CONCRETE);
        allowedBlocks.add(Blocks.GRAY_CONCRETE);
        allowedBlocks.add(Blocks.LIGHT_GRAY_CONCRETE);
        allowedBlocks.add(Blocks.CYAN_CONCRETE);
        allowedBlocks.add(Blocks.PURPLE_CONCRETE);
        allowedBlocks.add(Blocks.BLUE_CONCRETE);
        allowedBlocks.add(Blocks.BROWN_CONCRETE);
        allowedBlocks.add(Blocks.GREEN_CONCRETE);
        allowedBlocks.add(Blocks.RED_CONCRETE);
        allowedBlocks.add(Blocks.BLACK_CONCRETE);

        allowedBlocks.add(Blocks.WHITE_TERRACOTTA);
        allowedBlocks.add(Blocks.ORANGE_TERRACOTTA);
        allowedBlocks.add(Blocks.MAGENTA_TERRACOTTA);
        allowedBlocks.add(Blocks.LIGHT_BLUE_TERRACOTTA);
        allowedBlocks.add(Blocks.YELLOW_TERRACOTTA);
        allowedBlocks.add(Blocks.LIME_TERRACOTTA);
        allowedBlocks.add(Blocks.PINK_TERRACOTTA);
        allowedBlocks.add(Blocks.GRAY_TERRACOTTA);
        allowedBlocks.add(Blocks.LIGHT_GRAY_TERRACOTTA);
        allowedBlocks.add(Blocks.CYAN_TERRACOTTA);
        allowedBlocks.add(Blocks.PURPLE_TERRACOTTA);
        allowedBlocks.add(Blocks.BLUE_TERRACOTTA);
        allowedBlocks.add(Blocks.BROWN_TERRACOTTA);
        allowedBlocks.add(Blocks.GREEN_TERRACOTTA);
        allowedBlocks.add(Blocks.RED_TERRACOTTA);
        allowedBlocks.add(Blocks.BLACK_TERRACOTTA);

        allowedBlocks.add(Blocks.WHITE_STAINED_GLASS);
        allowedBlocks.add(Blocks.ORANGE_STAINED_GLASS);
        allowedBlocks.add(Blocks.MAGENTA_STAINED_GLASS);
        allowedBlocks.add(Blocks.LIGHT_BLUE_STAINED_GLASS);
        allowedBlocks.add(Blocks.YELLOW_STAINED_GLASS);
        allowedBlocks.add(Blocks.LIME_STAINED_GLASS);
        allowedBlocks.add(Blocks.PINK_STAINED_GLASS);
        allowedBlocks.add(Blocks.GRAY_STAINED_GLASS);
        allowedBlocks.add(Blocks.LIGHT_GRAY_STAINED_GLASS);
        allowedBlocks.add(Blocks.CYAN_STAINED_GLASS);
        allowedBlocks.add(Blocks.PURPLE_STAINED_GLASS);
        allowedBlocks.add(Blocks.BLUE_STAINED_GLASS);
        allowedBlocks.add(Blocks.BROWN_STAINED_GLASS);
        allowedBlocks.add(Blocks.GREEN_STAINED_GLASS);
        allowedBlocks.add(Blocks.RED_STAINED_GLASS);
        allowedBlocks.add(Blocks.BLACK_STAINED_GLASS);

        allowedBlocks.add(Blocks.WHITE_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.ORANGE_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.MAGENTA_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.YELLOW_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.LIME_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.PINK_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.GRAY_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.CYAN_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.PURPLE_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.BLUE_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.BROWN_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.GREEN_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.RED_GLAZED_TERRACOTTA);
        allowedBlocks.add(Blocks.BLACK_GLAZED_TERRACOTTA);

        allowedBlocks.add(Blocks.INFESTED_STONE);
        allowedBlocks.add(Blocks.INFESTED_COBBLESTONE);
        allowedBlocks.add(Blocks.INFESTED_STONE_BRICKS);
        allowedBlocks.add(Blocks.INFESTED_MOSSY_STONE_BRICKS);
        allowedBlocks.add(Blocks.INFESTED_CRACKED_STONE_BRICKS);
        allowedBlocks.add(Blocks.INFESTED_CHISELED_STONE_BRICKS);
    }

    public static void loadConfig() {
        // 使用 Forge 的 FMLPaths 获取标准配置目录，避免相对路径写到错误位置
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        Logger logger = RandomSkyBlockIsland.LOGGER;

        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                String json = reader.lines().reduce("", (a, b) -> a + b);
                parseConfig(json);
                logger.info("Config loaded from {}", configPath.toAbsolutePath());
            } catch (Exception e) {
                // 捕获所有异常（包括解析异常），回退到默认配置，防止崩溃
                logger.warn("Failed to load config from {}, using defaults. Reason: {}",
                        configPath.toAbsolutePath(), e.toString());
                if (logger.isDebugEnabled()) {
                    // debug 模式下打印完整堆栈
                    e.printStackTrace();
                }
            }
        } else {
            logger.info("Config file not found at {}, creating default config", configPath.toAbsolutePath());
            saveConfig();
        }
    }

    public static void saveConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        Logger logger = RandomSkyBlockIsland.LOGGER;

        try {
            Files.createDirectories(configPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                writer.write(generateConfigJson());
            }
            logger.debug("Config saved to {}", configPath.toAbsolutePath());
        } catch (IOException e) {
            // 配置写入失败时输出完整异常信息（含路径和堆栈），便于排查目录权限或磁盘问题
            logger.error("Failed to save config to {}: {}",
                    configPath.toAbsolutePath(), e.getMessage(), e);
        }

        IslandGame game = RandomSkyBlockIsland.getIslandGame();
        if (game != null) {
            game.updateDelay();
        }
    }

    private static String generateConfigJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"transformDelaySeconds\": ").append(transformDelaySeconds).append(",\n");
        sb.append("  \"secondsRemaining\": ").append(secondsRemaining).append(",\n");
        sb.append("  \"isRunning\": ").append(isRunning).append(",\n");
        sb.append("  \"defaultArea\": {\n");
        sb.append("    \"name\": \"").append(defaultArea.name).append("\",\n");
        sb.append("    \"minX\": ").append(defaultArea.minX).append(",\n");
        sb.append("    \"minY\": ").append(defaultArea.minY).append(",\n");
        sb.append("    \"minZ\": ").append(defaultArea.minZ).append(",\n");
        sb.append("    \"maxX\": ").append(defaultArea.maxX).append(",\n");
        sb.append("    \"maxY\": ").append(defaultArea.maxY).append(",\n");
        sb.append("    \"maxZ\": ").append(defaultArea.maxZ).append("\n");
        sb.append("  },\n");
        sb.append("  \"allowedBlocks\": [\n");

        for (int i = 0; i < allowedBlocks.size(); i++) {
            Block block = allowedBlocks.get(i);
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            sb.append("    \"").append(id.toString()).append("\"");
            if (i < allowedBlocks.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    private static void parseConfig(String json) {
        try {
            String delayStr = extractValue(json, "\"transformDelaySeconds\"");
        if (delayStr != null) {
            transformDelaySeconds = Integer.parseInt(delayStr);
        }

        String remainingStr = extractValue(json, "\"secondsRemaining\"");
        if (remainingStr != null) {
            secondsRemaining = Integer.parseInt(remainingStr);
        }

        String runningStr = extractValue(json, "\"isRunning\"");
        if (runningStr != null) {
            isRunning = Boolean.parseBoolean(runningStr);
        }

        // 解析默认区域
        String defaultAreaStr = extractValue(json, "\"defaultArea\"");
        if (defaultAreaStr != null) {
            parseDefaultArea(defaultAreaStr);
        } else {
            // 向后兼容：旧格式用 areas 数组，第一个作为 defaultArea，其余迁移到 saveAreas
            String areasStr = extractValue(json, "\"areas\"");
            if (areasStr != null) {
                parseAreasCompat(areasStr);
            }
        }

            String blocksStr = extractValue(json, "\"allowedBlocks\"");
            if (blocksStr != null) {
                allowedBlocks.clear();
                int start = blocksStr.indexOf("[");
                int end = blocksStr.lastIndexOf("]");
                if (start >= 0 && end > start) {
                    String[] blockIds = blocksStr.substring(start + 1, end).split(",");
                    for (String idStr : blockIds) {
                        String trimmed = idStr.trim().replace("\"", "");
                        if (!trimmed.isEmpty()) {
                            // 使用 tryParse 替代已弃用的 new ResourceLocation(String)
                            ResourceLocation id = ResourceLocation.tryParse(trimmed);
                            Block block = ForgeRegistries.BLOCKS.getValue(id);
                            if (block != Blocks.AIR) {
                                allowedBlocks.add(block);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            RandomSkyBlockIsland.LOGGER.warn("Failed to parse config, using defaults: {}", e.getMessage());
        }
    }

    /** 向后兼容：旧 areas 数组，第一个→defaultArea，其余→saveAreas */
    private static void parseAreasCompat(String areasStr) {
        int start = areasStr.indexOf("[");
        if (start < 0) return;

        int pos = start + 1;
        boolean isFirst = true;
        while (pos < areasStr.length()) {
            int braceStart = areasStr.indexOf("{", pos);
            if (braceStart < 0) break;

            int braceCount = 1;
            int braceEnd = braceStart + 1;
            while (braceEnd < areasStr.length() && braceCount > 0) {
                if (areasStr.charAt(braceEnd) == '{') braceCount++;
                if (areasStr.charAt(braceEnd) == '}') braceCount--;
                braceEnd++;
            }

            String areaStr = areasStr.substring(braceStart, braceEnd);
            if (isFirst) {
                parseDefaultArea(areaStr);
                isFirst = false;
            } else {
                parseSaveArea(areaStr);
            }
            pos = braceEnd;
        }
    }

    private static void parseDefaultArea(String areaStr) {
        try {
            String name = extractValue(areaStr, "\"name\"");
            if (name != null) {
                name = name.replace("\"", "");
            } else {
                name = "default";
            }

            int minX = parseIntValue(areaStr, "\"minX\"");
            int minY = parseIntValue(areaStr, "\"minY\"");
            int minZ = parseIntValue(areaStr, "\"minZ\"");
            int maxX = parseIntValue(areaStr, "\"maxX\"");
            int maxY = parseIntValue(areaStr, "\"maxY\"");
            int maxZ = parseIntValue(areaStr, "\"maxZ\"");

            defaultArea = new TransformArea(name, minX, minY, minZ, maxX, maxY, maxZ);
        } catch (Exception e) {
            RandomSkyBlockIsland.LOGGER.warn("Failed to parse default area: {}", e.getMessage());
        }
    }

    private static void parseSaveArea(String areaStr) {
        try {
            String name = extractValue(areaStr, "\"name\"");
            if (name != null) {
                name = name.replace("\"", "");
            } else {
                name = "area_" + saveAreas.size();
            }

            int minX = parseIntValue(areaStr, "\"minX\"");
            int minY = parseIntValue(areaStr, "\"minY\"");
            int minZ = parseIntValue(areaStr, "\"minZ\"");
            int maxX = parseIntValue(areaStr, "\"maxX\"");
            int maxY = parseIntValue(areaStr, "\"maxY\"");
            int maxZ = parseIntValue(areaStr, "\"maxZ\"");

            saveAreas.add(new TransformArea(name, minX, minY, minZ, maxX, maxY, maxZ));
        } catch (Exception e) {
            RandomSkyBlockIsland.LOGGER.warn("Failed to parse save area: {}", e.getMessage());
        }
    }

    // ========== 存档区域配置 ==========

    private static final String SAVE_AREAS_FILE = "rsl_areas.json";

    public static void loadSaveAreas() {
        if (saveDir == null) return;
        Path areasPath = saveDir.resolve(SAVE_AREAS_FILE);

        if (Files.exists(areasPath)) {
            try (BufferedReader reader = Files.newBufferedReader(areasPath)) {
                String json = reader.lines().reduce("", (a, b) -> a + b);
                saveAreas.clear();
                String areasStr = extractValue(json, "\"areas\"");
                if (areasStr != null) {
                    parseSaveAreasArray(areasStr);
                }
                RandomSkyBlockIsland.LOGGER.info("Save areas loaded from file");
            } catch (IOException e) {
                RandomSkyBlockIsland.LOGGER.warn("Failed to load save areas: {}", e.getMessage());
            }
        }
    }

    public static void saveSaveAreas() {
        if (saveDir == null) return;
        Path areasPath = saveDir.resolve(SAVE_AREAS_FILE);

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"areas\": [\n");
            for (int i = 0; i < saveAreas.size(); i++) {
                TransformArea area = saveAreas.get(i);
                sb.append("    {\n");
                sb.append("      \"name\": \"").append(area.name).append("\",\n");
                sb.append("      \"minX\": ").append(area.minX).append(",\n");
                sb.append("      \"minY\": ").append(area.minY).append(",\n");
                sb.append("      \"minZ\": ").append(area.minZ).append(",\n");
                sb.append("      \"maxX\": ").append(area.maxX).append(",\n");
                sb.append("      \"maxY\": ").append(area.maxY).append(",\n");
                sb.append("      \"maxZ\": ").append(area.maxZ).append("\n");
                sb.append("    }");
                if (i < saveAreas.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n}");

            try (BufferedWriter writer = Files.newBufferedWriter(areasPath)) {
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            RandomSkyBlockIsland.LOGGER.warn("Failed to save save areas: {}", e.getMessage());
        }
    }

    private static void parseSaveAreasArray(String areasStr) {
        int start = areasStr.indexOf("[");
        if (start < 0) return;

        int pos = start + 1;
        while (pos < areasStr.length()) {
            int braceStart = areasStr.indexOf("{", pos);
            if (braceStart < 0) break;

            int braceCount = 1;
            int braceEnd = braceStart + 1;
            while (braceEnd < areasStr.length() && braceCount > 0) {
                if (areasStr.charAt(braceEnd) == '{') braceCount++;
                if (areasStr.charAt(braceEnd) == '}') braceCount--;
                braceEnd++;
            }

            String areaStr = areasStr.substring(braceStart, braceEnd);
            parseSaveArea(areaStr);
            pos = braceEnd;
        }
    }

    public static TransformArea getAreaByName(String name) {
        if (defaultArea.name.equals(name)) return defaultArea;
        return saveAreas.stream().filter(a -> a.name.equals(name)).findFirst().orElse(null);
    }

    public static boolean addArea(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (getAreaByName(name) != null) {
            return false;
        }
        saveAreas.add(new TransformArea(name, minX, minY, minZ, maxX, maxY, maxZ));
        return true;
    }

    public static boolean removeArea(String name) {
        // 不能删除默认区域
        if (defaultArea.name.equals(name)) return false;
        return saveAreas.removeIf(a -> a.name.equals(name));
    }

    public static boolean renameArea(String oldName, String newName) {
        if (getAreaByName(newName) != null) return false;
        if (defaultArea.name.equals(oldName)) {
            defaultArea.name = newName;
            return true;
        }
        TransformArea area = saveAreas.stream().filter(a -> a.name.equals(oldName)).findFirst().orElse(null);
        if (area == null) return false;
        area.name = newName;
        return true;
    }

    private static String extractValue(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) return null;

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex < 0) return null;

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (json.charAt(valueStart) == '{') {
            int braceCount = 1;
            int valueEnd = valueStart + 1;
            while (valueEnd < json.length() && braceCount > 0) {
                if (json.charAt(valueEnd) == '{') braceCount++;
                if (json.charAt(valueEnd) == '}') braceCount--;
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        } else if (json.charAt(valueStart) == '[') {
            int bracketCount = 1;
            int valueEnd = valueStart + 1;
            while (valueEnd < json.length() && bracketCount > 0) {
                if (json.charAt(valueEnd) == '[') bracketCount++;
                if (json.charAt(valueEnd) == ']') bracketCount--;
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    private static int parseIntValue(String json, String key) {
        String value = extractValue(json, key);
        return value != null ? Integer.parseInt(value) : 0;
    }
}
