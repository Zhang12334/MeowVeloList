package com.meow.meowvelolist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.yaml.snakeyaml.Yaml;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.TextComponent;
import java.util.Arrays;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import com.meow.meowvelolist.Metrics;
@Plugin(
        id = "meowvelolist", 
        name = "MeowVeloList", 
        version = "1.0", 
        description = "一个在 Velocity 端显示玩家列表的插件", 
        authors = {"Zhang1233"} 
)
public class MeowVeloList {
    private String startupMessage;
    private String nowusingversionMessage;
    private String checkingupdateMessage;
    private String checkfailedMessage;
    private String updateavailableMessage;
    private String updateurlMessage;
    private String oldversionmaycauseproblemMessage;
    private String nowusinglatestversionMessage;
    private String nopermissionMessage;
    private String nowallplayercountMessage;
    private String singleserverplayeronlineMessage;
    private String noplayersonlineMessage;
    private String serverPrefix;
    private String playersPrefix;
    private String reloadsuccessMessage;
    private static final String VERSION = "1.0";
    private final ProxyServer server;
    private Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    @Inject
    public MeowVeloList(ProxyServer server, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
        loadLanguage(); 
        server.getConsoleCommandSource().sendMessage(Component.text(startupMessage));
        this.metricsFactory = metricsFactory;
    }

    // 订阅初始化事件
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        int pluginId = 23904; // <-- Replace with the id of your plugin!
        Metrics metrics = metricsFactory.make(this, pluginId);
        server.getCommandManager().register("mlist", new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                CommandSource source = invocation.source();
                String[] args = invocation.arguments();

                if (!source.hasPermission("meowvelolist.mlist")) {
                    source.sendMessage(Component.text(nopermissionMessage));
                    return;
                }

                int totalPlayers = server.getAllPlayers().size();
                TextComponent.Builder response = Component.text();

                // 使用全新的分隔符
                String separator = "§a≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡";

                response.append(Component.text(separator).decorate(TextDecoration.BOLD)).append(Component.newline());
                response.append(Component.text(nowallplayercountMessage + totalPlayers)).append(Component.newline());
                response.append(Component.text(separator)).append(Component.newline()); // 这行已替换为新的分隔符

                boolean firstServer = true; // 用于控制是否是第一个服务器

                for (RegisteredServer registeredServer : server.getAllServers()) {
                    String serverName = registeredServer.getServerInfo().getName();
                    List<String> playerNames = registeredServer.getPlayersConnected().stream()
                            .map(Player::getUsername)
                            .collect(Collectors.toList());

                    // 如果不是第一个服务器，添加分隔符
                    if (!firstServer) {
                        response.append(Component.text(separator)).append(Component.newline()); // 每个服务器之间添加分隔符
                    }

                    // 添加当前服务器信息
                    response.append(Component.text(serverPrefix + serverName + " §7(" + playerNames.size() + singleserverplayeronlineMessage + "§7)").append(Component.newline()));

                    // 玩家信息
                    if (!playerNames.isEmpty()) {
                        response.append(Component.text(playersPrefix + String.join(", ", playerNames)).append(Component.newline()));
                    } else {
                        response.append(Component.text(noplayersonlineMessage)).append(Component.newline());
                    }

                    firstServer = false; // 第一个服务器输出后，设置为false，后续服务器添加分隔符
                }

                response.append(Component.text(separator)).append(Component.newline()); // 输出最后一个服务器信息后再加上分隔符

                source.sendMessage(response.build());  // 发送构建的文本组件
            }
        });
        server.getCommandManager().register("mlist_reload", new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                CommandSource source = invocation.source();
                if (!source.hasPermission("meowvelolist.reload")) {
                    source.sendMessage(Component.text(nopermissionMessage));
                    return;
                }
                loadLanguage();  // 重载语言文件
                source.sendMessage(Component.text(reloadsuccessMessage));
            }
        });
        // 异步执行更新检查
        CompletableFuture.runAsync(() -> checkUpdate());
    }

    private void checkUpdate() {
        String currentVersion = VERSION;
        server.getConsoleCommandSource().sendMessage(Component.text(nowusingversionMessage + currentVersion));
        server.getConsoleCommandSource().sendMessage(Component.text(checkingupdateMessage));
        String latestVersionUrl = "https://github.com/Zhang12334/MeowVeloList/releases/latest";

        // 可用的前缀列表
        List<String> prefixList = Arrays.asList(
                "https://ghp.ci/",   
                ""
        );

        String latestVersion = null;

        // 逐一尝试不同前缀
        for (String prefix : prefixList) {
            try {
                String urlToCheck = prefix + latestVersionUrl;

                // 发送请求
                HttpURLConnection connection = (HttpURLConnection) new URL(urlToCheck).openConnection();
                connection.setInstanceFollowRedirects(false); // 防止自动跳转

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    String redirectUrl = connection.getHeaderField("Location");
                    if (redirectUrl != null && redirectUrl.contains("github.com")) {
                        latestVersion = extractVersionFromRedirectUrl(redirectUrl);
                        break;  // 成功获取版本后退出循环
                    }
                }
            } catch (IOException e) {
                // 连接失败，尝试下一个前缀
                continue;
            }
        }

        if (latestVersion == null) {
            server.getConsoleCommandSource().sendMessage(Component.text(checkfailedMessage));  
        } else {
            if (!currentVersion.equals(latestVersion)) {
                server.getConsoleCommandSource().sendMessage(Component.text(updateavailableMessage + latestVersion));
                server.getConsoleCommandSource().sendMessage(Component.text(updateurlMessage + latestVersionUrl));  
                server.getConsoleCommandSource().sendMessage(Component.text(oldversionmaycauseproblemMessage));
            } else {
                server.getConsoleCommandSource().sendMessage(Component.text(nowusinglatestversionMessage));
            }
        }
    }

    private String extractVersionFromRedirectUrl(String redirectUrl) {
        String versionPrefix = "tag/";
        int versionIndex = redirectUrl.lastIndexOf(versionPrefix);
        if (versionIndex != -1) {
            return redirectUrl.substring(versionIndex + versionPrefix.length());
        }
        return null;
    }

    // 加载语言设置
    private void loadLanguage() {
        String language = getLanguageFromConfig(); 

        if ("zh_cn".equalsIgnoreCase(language)) {
            loadChineseSimplifiedMessages();
        } else if ("zh_tc".equalsIgnoreCase(language)) {
            loadChineseTraditionalMessages();
        } else if ("en".equalsIgnoreCase(language)) {
            loadEnglishMessages();
        } else {
            loadChineseSimplifiedMessages();
        }
    }

    private String getLanguageFromConfig() {
        // 使用路径字符串而不是 File 对象
        File pluginDir = new File(dataDirectory.toFile().getPath());  // 获取路径
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();  // 如果目录不存在，创建目录
        }

        File configFile = new File(pluginDir, "config.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);  // 如果文件不存在，创建默认配置文件
        }

        try {
            Yaml yaml = new Yaml();
            java.util.Map<String, Object> config = yaml.load(java.nio.file.Files.newInputStream(configFile.toPath()));
            return (String) config.getOrDefault("language", "zh_cn");
        } catch (Exception e) {
            e.printStackTrace();
            return "zh_cn";
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
                Yaml yaml = new Yaml();
                java.util.Map<String, Object> config = new java.util.HashMap<>();
                config.put("language", "zh_cn");

                yaml.dump(config, java.nio.file.Files.newBufferedWriter(configFile.toPath()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载简体中文消息
    private void loadChineseSimplifiedMessages() {
        startupMessage = "[MeowVeloList] MeowVeloList 已加载!";
        nowusingversionMessage = "[MeowVeloList] 当前使用版本:";
        checkingupdateMessage = "[MeowVeloList] 正在检查更新...";
        checkfailedMessage = "[MeowVeloList] 检查更新失败，请检查你的网络状况!";
        updateavailableMessage = "[MeowVeloList] 有新版本可用：";
        updateurlMessage = "[MeowVeloList] 下载地址：";
        oldversionmaycauseproblemMessage = "[MeowVeloList] 使用旧版本可能会导致问题，请尽快更新!";
        nowusinglatestversionMessage = "[MeowVeloList] 正在使用最新版本!";
        nopermissionMessage = "你没有权限执行此命令!";
        nowallplayercountMessage = "§6当前在线人数: §e";
        singleserverplayeronlineMessage = " §e个在线玩家";
        noplayersonlineMessage = "§7当前没有在线玩家";
        serverPrefix = "§e子服 ";
        playersPrefix = "§7玩家列表: ";
        reloadsuccessMessage = "§a重载配置文件成功!";
    }

    // 加载繁体中文消息
    private void loadChineseTraditionalMessages() {
        startupMessage = "[MeowVeloList] MeowVeloList 已加載!";
        nowusingversionMessage = "[MeowVeloList] 當前使用版本：";
        checkingupdateMessage = "[MeowVeloList] 正在檢查更新...";
        checkfailedMessage = "[MeowVeloList] 檢查更新失敗，請檢查你的網路狀況!";
        updateavailableMessage = "[MeowVeloList] 有新版本可用：";
        updateurlMessage = "[MeowVeloList] 下載地址：";
        oldversionmaycauseproblemMessage = "[MeowVeloList] 使用舊版本可能會導致問題，請盡快更新!";
        nowusinglatestversionMessage = "[MeowVeloList] 正在使用最新版本!";
        nopermissionMessage = "你沒有權限執行此命令!";
        nowallplayercountMessage = "§6當前線上人數: §e";
        singleserverplayeronlineMessage = " §e個在線玩家";
        noplayersonlineMessage = "§7當前沒有在線玩家";
        serverPrefix = "§e子伺服 ";
        playersPrefix = "§7玩家列表: ";
        reloadsuccessMessage = "§a重載配置文件成功!";
    }

    // 加载英文消息
    private void loadEnglishMessages() {
        startupMessage = "[MeowVeloList] MeowVeloList has been loaded!";
        nowusingversionMessage = "[MeowVeloList] Currently using version:";
        checkingupdateMessage = "[MeowVeloList] Checking for updates...";
        checkfailedMessage = "[MeowVeloList] Update check failed. Please check your network connection!";
        updateavailableMessage = "[MeowVeloList] A new version is available:";
        updateurlMessage = "[MeowVeloList] Download URL:";
        oldversionmaycauseproblemMessage = "[MeowVeloList] Using an old version may cause issues. Please update as soon as possible!";
        nowusinglatestversionMessage = "[MeowVeloList] You are using the latest version!";
        nopermissionMessage = "You do not have permission to execute this command!";
        nowallplayercountMessage = "§6Current online players: §e";
        singleserverplayeronlineMessage = " §eplayer(s) online";
        noplayersonlineMessage = "§7No players online";
        serverPrefix = "§eServer ";
        playersPrefix = "§7Player list: ";
        reloadsuccessMessage = "§aReloaded config file successfully!";
    }
}
