package top.mistmc.lite;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Messages {
    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;

    // 将配置节点常量改为 public
    public static final String
            COMMAND = "command-messages",
            FESTIVAL = "festival-messages",
            API = "api-messages",
            SYSTEM = "system-messages",
            SOUND = "sound-messages",
            DEBUG = "debug-messages";

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadMessages();
    }

    public void reloadMessages() {
        File file = new File(plugin.getDataFolder(), "message.yml");
        if (!file.exists()) {
            plugin.saveResource("message.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    // 将 get 方法改为 public
    public String get(String path, String def) {
        return messagesConfig.getString(path, def).replace('&', '§');
    }

    // 将 getWithPlaceholders 方法改为 public
    public String getWithPlaceholders(String path, String def, Pair... placeholders) {
        String message = get(path, def);
        for (Pair pair : placeholders) {
            message = message.replace(pair.key(), pair.value());
        }
        return message;
    }

    // 将 getList 方法改为 public
    public List<String> getList(String path, List<String> defaults) {
        if (!messagesConfig.contains(path)) {
            return defaults.stream()
                    .map(s -> s.replace('&', '§'))
                    .collect(Collectors.toList());
        }
        return messagesConfig.getStringList(path).stream()
                .map(s -> s.replace('&', '§'))
                .collect(Collectors.toList());
    }

    // API错误消息
    public String getApiError(String apiName, String errorDetail) {
        return getWithPlaceholders(
                API + ".error-template",
                "&c{api} 请求失败: {error}",
                new Pair("{api}", apiName),
                new Pair("{error}", errorDetail)
        );
    }

    // 调试消息
    public String getDebugMessage(String type, Pair... pairs) {
        String raw = get(DEBUG + "." + type, "");
        return getWithPlaceholders(raw, raw, pairs);
    }

    // 节日消息模板
    public List<String> getFestivalTemplates() {
        return getList(FESTIVAL + ".templates", Arrays.asList(
                "&a“{jieri_shici}” &e{festival_name}快乐！",
                "&e{festival_name}特别问候：&6{message}"
        ));
    }

    // 将 Pair 类改为 public
    public static class Pair {
        private final String key;
        private final String value;

        public Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String key() {
            return key;
        }

        public String value() {
            return value;
        }
    }
}