package com.meow.meowvelolist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(
        id = "meowvelolist",
        name = "MeowVeloList",
        version = "1.0",
        description = "一个在 Velocity 端显示玩家列表的插件",
        authors = {"Zhang1233"}
)
public class MeowVeloList {

    private final ProxyServer server;

    // 语言变量
    private String startupMessage;
    private String shutdownMessage;
    private String notenableMessage;
    private String nowusingversionMessage;
    private String checkingupdateMessage;
    private String checkfailedMessage;
    private String updateavailableMessage;
    private String updateurlMessage;
    private String oldversionmaycauseproblemMessage;
    private String nowusinglatestversionMessage;
    private String reloadedMessage;
    private String nopermissionMessage;
    private String serverPrefix;
    private String playersPrefix;

    @Inject
    public MeowVeloList(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        loadLanguage(); // 载入语言
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Register the command /meowlist
        server.getCommandManager().register("meowlist", new PlayerInfoCommand(server));
        // 检查更新
        checkUpdate();
    }

    // Load language settings based on config
    private void loadLanguage() {
        String language = getLanguageFromConfig(); // 从配置文件获取语言代码

        // 根据语言代码加载不同的消息内容
        if ("zh_cn".equalsIgnoreCase(language)) {
            loadChineseSimplifiedMessages();
        } else if ("zh_tc".equalsIgnoreCase(language)) {
            loadChineseTraditionalMessages();
        } else {
            loadEnglishMessages();  // 默认加载英文
        }
    }

    // 获取配置文件中的语言设置
    private String getLanguageFromConfig() {
        File configFile = new File("config.yml");  // 假设配置文件名为 config.yml
        if (!configFile.exists()) {
            // 如果配置文件不存在，则创建一个默认配置文件
            createDefaultConfig(configFile);
        }

        // 读取配置文件中的内容
        try {
            Yaml yaml = new Yaml();
            java.util.Map<String, Object> config = yaml.load(java.nio.file.Files.newInputStream(configFile.toPath()));
            return (String) config.getOrDefault("language", "zh_cn");  // 默认返回 zh_cn
        } catch (Exception e) {
            e.printStackTrace();
            return "zh_cn";  // 如果读取配置失败，默认返回 zh_cn
        }
    }

    // 创建一个默认的配置文件
    private void createDefaultConfig(File configFile) {
        try {
            // 仅在配置文件不存在时创建
            if (!configFile.exists()) {
                configFile.createNewFile();
                Yaml yaml = new Yaml();
                java.util.Map<String, Object> config = new java.util.HashMap<>();
                config.put("language", "zh_cn");  // 默认语言设置为 zh_cn

                // 保存配置文件
                yaml.dump(config, java.nio.file.Files.newBufferedWriter(configFile.toPath()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载简体中文消息
    private void loadChineseSimplifiedMessages() {
        startupMessage = "MeowVeloList 已加载！";
        shutdownMessage = "MeowVeloList 已卸载！";
        notenableMessage = "插件未启用，请前往配置文件中设置！";
        nowusingversionMessage = "当前使用版本:";
        checkingupdateMessage = "正在检查更新...";
        checkfailedMessage = "检查更新失败，请检查你的网络状况！";
        updateavailableMessage = "发现新版本:";
        updateurlMessage = "新版本下载地址:";
        oldversionmaycauseproblemMessage = "旧版本可能会导致问题，请尽快更新！";
        nowusinglatestversionMessage = "您正在使用最新版本！";
        reloadedMessage = "配置文件已重载！";
        nopermissionMessage = "你没有权限执行此命令！";
        serverPrefix = "§eServer: ";
        playersPrefix = "§bPlayers: ";
    }

    // 加载繁体中文消息
    private void loadChineseTraditionalMessages() {
        startupMessage = "MeowVeloList 已加載！";
        shutdownMessage = "MeowVeloList 已卸載！";
        notenableMessage = "插件未啟用，請前往配置文件中設置！";
        nowusingversionMessage = "當前使用版本:";
        checkingupdateMessage = "正在檢查更新...";
        checkfailedMessage = "檢查更新失敗，請檢查您的網絡狀況！";
        updateavailableMessage = "發現新版本:";
        updateurlMessage = "新版本下載地址:";
        oldversionmaycauseproblemMessage = "舊版本可能會導致問題，請盡快更新！";
        nowusinglatestversionMessage = "您正在使用最新版本！";
        reloadedMessage = "配置文件已重載！";
        nopermissionMessage = "您沒有權限執行此命令！";
        serverPrefix = "§e伺服器: ";
        playersPrefix = "§b玩家: ";
    }

    // 加载英文消息
    private void loadEnglishMessages() {
        startupMessage = "MeowVeloList has been loaded!";
        shutdownMessage = "MeowVeloList has been disabled!";
        notenableMessage = "Plugin not enabled, please set it in the configuration file!";
        nowusingversionMessage = "Currently using version:";
        checkingupdateMessage = "Checking for updates...";
        checkfailedMessage = "Update check failed, please check your network!";
        updateavailableMessage = "A new version is available:";
        updateurlMessage = "Download update at:";
        oldversionmaycauseproblemMessage = "Old versions may cause problems!";
        nowusinglatestversionMessage = "You are using the latest version!";
        reloadedMessage = "Configuration file has been reloaded!";
        nopermissionMessage = "You do not have permission to execute this command!";
        serverPrefix = "§eServer: ";
        playersPrefix = "§bPlayers: ";
    }

    // Command to display the information
    private static class PlayerInfoCommand implements SimpleCommand {
        private final ProxyServer server;

        public PlayerInfoCommand(ProxyServer server) {
            this.server = server;
        }

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            
            // Get the total number of online players
            int totalPlayers = server.getAllPlayers().size();
            StringBuilder response = new StringBuilder(MeowVeloList.getInstance().nowusingversionMessage + ": " + totalPlayers + "\n");

            // Get online players per server
            for (RegisteredServer registeredServer : server.getAllServers()) {
                String serverName = registeredServer.getServerInfo().getName();
                List<String> playerNames = registeredServer.getPlayersConnected().stream()
                        .map(Player::getUsername)
                        .collect(Collectors.toList());

                response.append(MeowVeloList.getInstance().serverPrefix).append(serverName)
                        .append(" §7(").append(playerNames.size()).append(")\n");
                response.append(MeowVeloList.getInstance().playersPrefix)
                        .append(String.join(", ", playerNames)).append("\n");
            }

            // Send the information to the player/console who issued the command
            source.sendMessage(Component.text(response.toString()));
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            return List.of(); // No suggestions for this command
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return true; // All players and console can use this command
        }
    }

    // 检查更新
    private void checkUpdate() {
        // 获取当前版本号
        String currentVersion = getDescription().getVersion();
        String[] githubUrls = {
            "https://ghp.ci/",
            "https://raw.fastgit.org/",
            ""  // 最后使用源地址
        };
        String latestVersionUrl = "https://github.com/Zhang12334/MeowVeloList/releases/latest";

        try {
            String latestVersion = null;
            for (String url : githubUrls) {
                HttpURLConnection connection = (HttpURLConnection) new URL(url + latestVersionUrl).openConnection();
                connection.setInstanceFollowRedirects(false); // 不自动跟随重定向
                int responseCode = connection.getResponseCode();
                if (responseCode == 302) { // 如果 302 了
                    String redirectUrl = connection.getHeaderField("Location");
                    if (redirectUrl != null && redirectUrl.contains("tag/")) {
                        latestVersion = extractVersionFromUrl(redirectUrl);
                        break;
                    }
                }
                connection.disconnect();
                if (latestVersion != null) {
                    break;
                }
            }
            if (latestVersion == null) {
                getLogger().warning(checkfailedMessage);
                return;
            }
            if (isVersionGreater(latestVersion, currentVersion)) {
                getLogger().warning(updateavailableMessage + " v" + latestVersion);
                getLogger().warning(updateurlMessage + " https://github.com/Zhang12334/MeowVeloList/releases/latest");
                getLogger().warning(oldversionmaycauseproblemMessage);
            } else {
                getLogger().info(nowusinglatestversionMessage);
            }
        } catch (Exception e) {
            getLogger().warning(checkfailedMessage);
        }
    }

    private boolean isVersionGreater(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        for (int i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            if (v1Part > v2Part) {
                return true;
            } else if (v1Part < v2Part) {
                return false;
            }
        }
        return false;
    }

    private String extractVersionFromUrl(String url) {
        int tagIndex = url.indexOf("tag/");
        if (tagIndex != -1) {
            int endIndex = url.indexOf('/', tagIndex + 4);
            if (endIndex == -1) {
                endIndex = url.length();
            }
            return url.substring(tagIndex + 4, endIndex);
        }
        return null;
    }
}
