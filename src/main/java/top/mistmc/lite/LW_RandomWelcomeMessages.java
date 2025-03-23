package top.mistmc.lite;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LW_RandomWelcomeMessages extends JavaPlugin implements Listener {

    private final Random random = new Random();
    private FileConfiguration config;
    private Messages messages;
    private boolean debugMode;

    // 配置字段
    private List<MessageEntry> messageEntries;
    private Map<String, String> shiciCategories;
    private int shiciTimeout;
    private boolean soundEnabled;
    private Sound soundType;
    private float soundVolume, soundPitch;
    private boolean useActionBar;
    private Map<LocalDate, String[]> festivalDates;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messages = new Messages(this);
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);

        // 插件启动成功提示
        printStartupMessage();
    }

    private void printStartupMessage() {
        messages.getList(Messages.SYSTEM + ".startup-messages", Arrays.asList(
                "===== LW-RandomWelcomeMessages =====",
                "作者: {author}",
                "QQ交流群: {qq-group}",
                "老万的随机欢迎消息插件已加载！"
        )).forEach(line -> {
            String formatted = messages.getWithPlaceholders(line, line,
                    new Messages.Pair("{author}", "mc506lw"),
                    new Messages.Pair("{qq-group}", "645921477"));
            getLogger().info(formatted);
        });
    }

    private void loadConfig() {
        try {
            reloadConfig();
            config = getConfig();
            this.debugMode = config.getBoolean("debug", false);

            // 加载消息条目
            messageEntries = new ArrayList<>();
            for (String entry : config.getStringList("messages.mixed-format")) {
                String[] parts = entry.split(":", 2);
                if (parts.length == 2) {
                    try {
                        int weight = Integer.parseInt(parts[0]);
                        if (weight > 0) {
                            messageEntries.add(new MessageEntry(weight, parts[1]));
                        }
                    } catch (NumberFormatException e) {
                        getLogger().warning("无效的权重值: " + entry);
                    }
                } else {
                    throw new InvalidConfigurationException("无效的消息格式: " + entry);
                }
            }

            // 加载诗词配置
            ConfigurationSection shiciSection = config.getConfigurationSection("messages.shici");
            if (shiciSection == null) {
                throw new InvalidConfigurationException("诗词服务配置缺失！");
            }
            shiciTimeout = shiciSection.getInt("timeout", 3000);
            shiciCategories = new LinkedHashMap<>();

            // 加载通用诗词接口
            shiciCategories.put("all", shiciSection.getString("all"));

            // 加载分类接口
            ConfigurationSection categorySection = shiciSection.getConfigurationSection("categories");
            if (categorySection != null) {
                for (String key : categorySection.getKeys(false)) {
                    shiciCategories.put(key, categorySection.getString(key));
                }
            }

            // 加载节日日期（去除年份限制）
            festivalDates = new HashMap<>();
            for (String dateEntry : config.getStringList("festival.dates")) {
                String[] parts = dateEntry.split(":");
                if (parts.length == 3) {
                    String monthDay = parts[0];
                    // 使用虚拟年份（例如2020）解析日期格式
                    LocalDate dummyDate = LocalDate.parse("2020-" + monthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    festivalDates.put(dummyDate.withYear(2020), new String[]{parts[1], parts[2]});
                } else {
                    throw new InvalidConfigurationException("无效的节日日期格式: " + dateEntry);
                }
            }

            // 加载音效设置
            soundEnabled = config.getBoolean("sound.enable", true);
            try {
                soundType = Sound.valueOf(config.getString("sound.type", "ENTITY_PLAYER_LEVELUP"));
            } catch (IllegalArgumentException e) {
                soundType = Sound.ENTITY_PLAYER_LEVELUP;
            }
            soundVolume = (float) config.getDouble("sound.volume", 1.0);
            soundPitch = (float) config.getDouble("sound.pitch", 1.0);

            // 加载其他设置
            useActionBar = config.getBoolean("settings.use-actionbar", true);

        } catch (InvalidConfigurationException e) {
            getLogger().severe("配置文件错误: " + e.getMessage());
            // 重新生成默认配置文件
            saveDefaultConfig();
            getLogger().warning("已生成默认配置文件！");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 播放音效
        if (soundEnabled) {
            player.playSound(player.getLocation(), soundType, soundVolume, soundPitch);
        }

        // 异步获取欢迎消息
        CompletableFuture.supplyAsync(() -> getWelcomeMessage(player))
                .thenAcceptAsync(message -> {
                    if (message == null) return;

                    String formatted = colorize(message);

                    if (useActionBar) {
                        sendActionBar(player, formatted);
                    } else {
                        player.sendMessage(formatted);
                    }
                }, sync -> Bukkit.getScheduler().runTask(this, sync));
    }

    private String getWelcomeMessage(Player player) {
        // 节日模式优先
        if (config.getBoolean("festival.enable", false)) {
            String festivalMessage = handleFestivalMessage(player);
            if (festivalMessage != null) return festivalMessage;
        }

        // 加权随机选择消息模板
        int totalWeight = messageEntries.stream().mapToInt(MessageEntry::getWeight).sum();
        int randomValue = random.nextInt(totalWeight);
        int cumulative = 0;
        for (MessageEntry entry : messageEntries) {
            cumulative += entry.getWeight();
            if (randomValue < cumulative) {
                return processTemplate(player, entry.getTemplate());
            }
        }
        return messages.get("system-messages.default-welcome", "欢迎加入！");
    }

    private String handleFestivalMessage(Player player) {
        LocalDate today = LocalDate.now().withYear(2020); // 使用虚拟年份匹配
        String[] festivalInfo = festivalDates.get(today);
        if (festivalInfo != null) {
            String poem = fetchShici(festivalInfo[2]); // 第三个参数是诗词类型
            if (poem != null) {
                List<String> festivalTemplates = messages.getFestivalTemplates();
                if (festivalTemplates.isEmpty()) {
                    return messages.getWithPlaceholders(
                            Messages.FESTIVAL + ".default-template",
                            "&a“{jieri_shici}” &e{festival_name}快乐！",
                            new Messages.Pair("{jieri_shici}", poem),
                            new Messages.Pair("{festival_name}", festivalInfo[1])
                    );
                }
                String template = festivalTemplates.get(random.nextInt(festivalTemplates.size()));
                return messages.getWithPlaceholders(template,template,
                        new Messages.Pair("{jieri_shici}", poem),
                        new Messages.Pair("{festival_name}", festivalInfo[1]),
                        new Messages.Pair("{player}", player.getName())
                );
            }
        }
        return null;
    }

    private String processTemplate(Player player, String template) {
        // 处理诗词分类占位符
        Matcher shiciMatcher = Pattern.compile("\\{shici_(\\w+)\\}").matcher(template);
        while (shiciMatcher.find()) {
            String category = shiciMatcher.group(1);
            template = template.replace(shiciMatcher.group(0), fetchShici(category));
        }

        // 处理通用诗词占位符
        template = template.replace("{shici}", fetchShici("all"));

        // 处理随机内容占位符
        template = template.replace("{random}", getRandomContent());

        // 基础占位符替换
        return template.replace("{player}", player.getName())
                .replace("{hitokoto}", fetchHitokoto());
    }

    private String fetchHitokoto() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(config.getString("messages.hitokoto.api-url")).openConnection();
            conn.setConnectTimeout(config.getInt("messages.hitokoto.timeout"));
            conn.setRequestMethod("GET");

            // 添加请求头，避免被服务器拒绝
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "LW_RandomWelcomeMessages/1.0");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // 解析 JSON 响应
                JSONObject json = new JSONObject(response.toString());
                if (json.has("hitokoto")) {
                    return json.getString("hitokoto");
                } else {
                    getLogger().warning("一言API返回格式错误: " + response);
                    return messages.get("api-messages.hitokoto-failure", "一言获取失败");
                }
            }
        } catch (Exception e) {
            getLogger().warning(messages.getApiError("一言API", e.getMessage()));
            return messages.get("api-messages.hitokoto-failure", "今日寄语加载中...");
        }
    }

    private String fetchShici(String category) {
        String url = shiciCategories.getOrDefault(category, shiciCategories.get("all"));
        if (url == null) return messages.get("api-messages.shici-failure", "诗词服务未配置");

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(shiciTimeout);
            conn.setRequestMethod("GET");

            // 添加请求头，避免被服务器拒绝
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "LW_RandomWelcomeMessages/1.0");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // 解析 JSON 响应
                JSONObject json = new JSONObject(response.toString());
                if (json.has("content")) {
                    return json.getString("content");
                } else {
                    getLogger().warning("诗词API返回格式错误: " + response);
                    return messages.get("api-messages.shici-failure", "诗词解析失败");
                }
            }
        } catch (Exception e) {
            getLogger().warning(messages.getApiError("诗词API", e.getMessage()));
            return messages.get("api-messages.shici-failure", "暂未找到合适诗句");
        }
    }

    private String getRandomContent() {
        // 加载随机权重配置
        ConfigurationSection randomWeightSection = config.getConfigurationSection("messages.random-weight");
        if (randomWeightSection == null) {
            return messages.get("system-messages.random-content-failure", "随机内容未配置");
        }

        int shiciWeight = randomWeightSection.getInt("shici", 50);
        int hitokotoWeight = randomWeightSection.getInt("hitokoto", 50);
        int totalWeight = shiciWeight + hitokotoWeight;

        // 根据权重随机选择
        int randomValue = random.nextInt(totalWeight);
        if (randomValue < shiciWeight) {
            return fetchShici("all");
        } else {
            return fetchHitokoto();
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
        } catch (NoSuchMethodError e) {
            // 旧版本的逻辑（如 1.8）
            getLogger().warning(messages.get("system-messages.actionbar-not-supported", "动作栏功能需要 1.9+ 版本支持"));
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("rwm")) return false;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!hasPermission(sender, "rwm.reload")) return true;
                loadConfig();
                sender.sendMessage(messages.get("command-messages.reload-message", "&a配置重载成功"));
                break;
            case "send":
                if (!hasPermission(sender, "rwm.send")) return true;
                if (sender instanceof Player) {
                    onPlayerJoin(new PlayerJoinEvent((Player) sender, "测试消息"));
                }
                break;
            case "test":
                if (!hasPermission(sender, "rwm.test")) return true;
                if (args.length == 3) {
                    if (sender instanceof Player) {
                        try {
                            // 使用MonthDay解析不含年份的日期
                            MonthDay monthDay = MonthDay.parse(
                                    args[1] + "-" + args[2],
                                    DateTimeFormatter.ofPattern("MM-dd")
                            );
                            // 替换原有解析方式为：
                            LocalDate date = LocalDate.of(2020, Integer.parseInt(args[1]), Integer.parseInt(args[2]));


                // 获取节日信息
                String[] festivalInfo = festivalDates.get(date);
                if (festivalInfo != null) {
                    // 获取诗词内容
                    String poem = fetchShici(festivalInfo[1]); // 修改第三个参数为正确的索引

                    if (poem != null) {
                        // 获取节日模板
                        List<String> festivalTemplates = messages.getFestivalTemplates();
                        String festivalMessage;
                        if (festivalTemplates.isEmpty()) {
                            // 使用默认模板
                            festivalMessage = messages.getWithPlaceholders(
                                    Messages.FESTIVAL + ".default-template",
                                    "&a“{jieri_shici}” &e{festival_name}快乐！",
                                    new Messages.Pair("{jieri_shici}", poem),
                                    new Messages.Pair("{festival_name}", festivalInfo[1])
                            );
                        } else {
                            // 随机选择一个模板
                            String template = festivalTemplates.get(random.nextInt(festivalTemplates.size()));
                            festivalMessage = messages.getWithPlaceholders(template, template,
                                    new Messages.Pair("{jieri_shici}", poem),
                                    new Messages.Pair("{festival_name}", festivalInfo[1]),
                                    new Messages.Pair("{player}", ((Player) sender).getName())
                            );
                        }
                        // 发送节日消息
                        sender.sendMessage(colorize(festivalMessage));
                    } else {
                        sender.sendMessage(messages.get("command-messages.festival-not-found", "&c未找到该节日信息"));
                    }
                } else {
                    sender.sendMessage(messages.get("command-messages.festival-not-found", "&c未找到该节日信息"));
                }
            } catch (Exception e) {
                getLogger().warning(messages.get("command-messages.festival-parse-failure", "&4error: " + e.getMessage()));
                sender.sendMessage(messages.get("command-messages.festival-parse-failure", "&c节日日期解析失败"));
            }
        } else {
            sender.sendMessage(messages.get("command-messages.festival-missing-args", "&c缺少参数，请使用 /rwm test MM DD"));
        }
    }
    break;

            default:
                sender.sendMessage(messages.get("command-messages.unknown-command", "&c未知命令，使用 /rwm 查看帮助"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        messages.getList("command-messages.help-lines", Arrays.asList(
                "&6==== &eLW-RandomWelcomeMessages &6====",
                "&e/rwm reload &7- 重载配置文件",
                "&e/rwm send &7- 向自己发送测试消息",
                "&e/rwm test MM DD&7- 测试节日命令是否正常，需要包含参数 MM-DD"
        )).forEach(sender::sendMessage);
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        sender.sendMessage(messages.get("command-messages.no-permission", "&c⚠ 权限不足！"));
        return false;
    }

    @Override
    public void onDisable() {
        getLogger().info(messages.get("system-messages.shutdown", "老万的随机欢迎消息插件已卸载！"));
    }
}

class MessageEntry {
    private final int weight;
    private final String template;

    public MessageEntry(int weight, String template) {
        this.weight = weight;
        this.template = template;
    }

    public int getWeight() {
        return weight;
    }

    public String getTemplate() {
        return template;
    }
}