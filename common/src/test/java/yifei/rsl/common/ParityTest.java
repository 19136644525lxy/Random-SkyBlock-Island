package yifei.rsl.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 双端一致性检查：语言键集合与命令字面量必须完全一致。
 * 这些检查不需要 Minecraft 运行时，直接读取两个平台的源码/资源。
 */
class ParityTest {
    private static final Path FABRIC_LANG = Path.of("src/main/resources/assets/rsl/lang");
    private static final Path FORGE_LANG = Path.of("forge/src/main/resources/assets/rsl/lang");
    private static final Path FABRIC_COMMANDS = Path.of("src/main/java/yifei/rsl/commands");
    private static final Path FORGE_COMMANDS = Path.of("forge/src/main/java/yifei/rsl/commands");
    private static final Pattern LITERAL = Pattern.compile("literal\\(\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern ARGUMENT = Pattern.compile("argument\\(\\s*\"([^\"]+)\"");

    @Test
    void languageKeysMatchAcrossPlatforms() throws IOException {
        Set<String> fabricZh = keys(FABRIC_LANG.resolve("zh_cn.json"));
        Set<String> fabricEn = keys(FABRIC_LANG.resolve("en_us.json"));
        Set<String> forgeZh = keys(FORGE_LANG.resolve("zh_cn.json"));
        Set<String> forgeEn = keys(FORGE_LANG.resolve("en_us.json"));

        assertEquals(fabricZh, fabricEn, "Fabric zh_cn/en_us 键集合不一致");
        assertEquals(fabricZh, forgeZh, "Fabric/Forge zh_cn 键集合不一致");
        assertEquals(fabricEn, forgeEn, "Fabric/Forge en_us 键集合不一致");
    }

    @Test
    void commandLiteralsMatchAcrossPlatforms() throws IOException {
        Set<String> fabric = commandNames(FABRIC_COMMANDS, LITERAL);
        Set<String> forge = commandNames(FORGE_COMMANDS, LITERAL);
        assertEquals(fabric, forge, "Fabric/Forge 命令 literal 集合不一致");
        assertTrue(fabric.contains("randomskyblock"), "缺少根命令 randomskyblock");
        assertTrue(fabric.contains("blocks"), "缺少 blocks 子命令");
        assertTrue(fabric.contains("status"), "缺少 status 子命令");
        assertTrue(fabric.contains("reload"), "缺少 reload 子命令");
    }

    @Test
    void commandArgumentsMatchAcrossPlatforms() throws IOException {
        Set<String> fabric = commandNames(FABRIC_COMMANDS, ARGUMENT);
        Set<String> forge = commandNames(FORGE_COMMANDS, ARGUMENT);
        assertEquals(fabric, forge, "Fabric/Forge 命令参数名集合不一致");
    }

    private static Set<String> keys(Path file) throws IOException {
        assertTrue(Files.isRegularFile(file), "缺少语言文件: " + file);
        JsonObject object = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        return new TreeSet<>(object.keySet());
    }

    private static Set<String> commandNames(Path directory, Pattern pattern) throws IOException {
        assertTrue(Files.isDirectory(directory), "缺少命令目录: " + directory);
        Set<String> names = new LinkedHashSet<>();
        try (Stream<Path> stream = Files.walk(directory)) {
            List<Path> files = stream.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : files) {
                Matcher matcher = pattern.matcher(Files.readString(file));
                while (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        }
        return names;
    }
}
