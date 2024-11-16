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

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
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
    private String nowallplayercountMessage;
    private String singleserverplayeronlineMessage;
    private String noplayersonlineMessage;
    private static final String VERSION = "1.0";
    private final ProxyServer server;
    private Path dataDirectory;

    @Inject
    public MeowVeloList(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
        loadLanguage(); 
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getCommandManager().register("meowlist", new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                CommandSource source = invocation.source();
                String[] args = invocation.arguments();

                if (!source.hasPermission("meowvelolist.meowlist")) {
                    source.sendMessage(Component.text(nopermissionMessage));  
                    return;
                }

                int totalPlayers = server.getAllPlayers().size();
                StringBuilder response = new StringBuilder();
                response.append(Component.text("§a≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡")
                        .decorate(TextDecoration.BOLD)).append("\n");
                response.append(nowallplayercountMessage).append(totalPlayers).append("\n");
                response.append("§a------------------------------------------------------------\n");

                for (RegisteredServer registeredServer : server.getAllServers()) {
                    String serverName = registeredServer.getServerInfo().getName();
                    List<String> playerNames = registeredServer.getPlayersConnected().stream()
                            .map(Player::getUsername)
                            .collect(Collectors.toList());

                    response.append(serverPrefix).append(serverName).append(" §7(")
                            .append(playerNames.size()).append(singleserverplayeronlineMessage).append("§7)").append("\n");

                    if (!playerNames.isEmpty()) {
                        response.append(playersPrefix)
                                .append(String.join(", ", playerNames)).append("\n");
                    } else {
                        response.append(noplayersonlineMessage).append("\n");
                    }

                    response.append("§a------------------------------------------------------------\n");
                }

                source.sendMessage(Component.text(response.toString()));
            }
        });
        checkUpdate();
    }

    private void checkUpdate() {
        String currentVersion = VERSION; 
        String latestVersionUrl = "https://github.com/Zhang12334/MeowVeloList/releases/latest";

        try {
            String latestVersion = null;
            HttpURLConnection connection = (HttpURLConnection) new URL(latestVersionUrl).openConnection();
            connection.setInstanceFollowRedirects(false); 

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                String redirectUrl = connection.getHeaderField("Location");
                if (redirectUrl != null && redirectUrl.contains("github.com")) {
                    latestVersion = extractVersionFromRedirectUrl(redirectUrl);
                }
            }

            if (latestVersion == null) {
                server.getConsoleCommandSource().sendMessage(Component.text(checkfailedMessage));  
            } else {
                if (!currentVersion.equals(latestVersion)) {
                    server.getConsoleCommandSource().sendMessage(Component.text(updateavailableMessage + latestVersion));
                    server.getConsoleCommandSource().sendMessage(Component.text(updateurlMessage + latestVersionUrl));  
                } else {
                    server.getConsoleCommandSource().sendMessage(Component.text(nowusinglatestversionMessage));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            server.getConsoleCommandSource().sendMessage(Component.text(checkfailedMessage));
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

    private void loadLanguage() {
        String language = getLanguageFromConfig(); 

        if ("zh_cn".equalsIgnoreCase(language)) {
            loadChineseSimplifiedMessages();
        } else if ("zh_tc".equalsIgnoreCase(language)) {
            loadChineseTraditionalMessages();
        } else {
            loadEnglishMessages();
        }
    }

    private String getLanguageFromConfig() {
        File pluginDir = new File(dataDirectory.toFile(), "MeowVeloList");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }

        File configFile = new File(pluginDir, "config.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
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
        startupMessage = "MeowVeloList 已加载！";
        shutdownMessage = "MeowVeloList 已卸载！";
        notenableMessage = "插件未启用，请前往配置文件中设置！";
        nowusingversionMessage = "当前使用版本:";
        checkingupdateMessage = "正在检查更新...";
        checkfailedMessage = "检查更新失败，请检查你的网络状况！";
        updateavailableMessage = "有新版本可用：";
        updateurlMessage = "下载地址：";
        oldversionmaycauseproblemMessage = "旧版本可能会导致一些问题，请及时更新。";
        nowusinglatestversionMessage = "你正在使用最新版本。";
        reloadedMessage = "配置已重新加载！";
        nopermissionMessage = "你没有权限执行此命令！";
        serverPrefix = "服务器 ";
        playersPrefix = "玩家: ";
        nowallplayercountMessage = "当前在线人数: ";
        singleserverplayeronlineMessage = "个玩家在线";
        noplayersonlineMessage = "当前没有在线玩家";
    }

    // 加载繁体中文消息
    private void loadChineseTraditionalMessages() {
        startupMessage = "MeowVeloList 已載入！";
        shutdownMessage = "MeowVeloList 已卸載！";
        notenableMessage = "插件未啟用，請前往配置文件中設置！";
        nowusingversionMessage = "當前使用版本:";
        checkingupdateMessage = "正在檢查更新...";
        checkfailedMessage = "檢查更新失敗，請檢查你的網絡狀況！";
        updateavailableMessage = "有新版本可用：";
        updateurlMessage = "下載地址：";
        oldversionmaycauseproblemMessage = "舊版本可能會導致一些問題，請及時更新。";
        nowusinglatestversionMessage = "你正在使用最新版本。";
        reloadedMessage = "配置已重新加載！";
        nopermissionMessage = "你沒有權限執行此命令！";
        serverPrefix = "伺服器 ";
        playersPrefix = "玩家: ";
        nowallplayercountMessage = "當前線上人數: ";
        singleserverplayeronlineMessage = "個在線玩家";
        noplayersonlineMessage = "當前沒有在線玩家";
    }

    // 加载英文消息
    private void loadEnglishMessages() {
        startupMessage = "MeowVeloList has loaded!";
        shutdownMessage = "MeowVeloList has unloaded!";
        notenableMessage = "Plugin not enabled, please set it in the config!";
        nowusingversionMessage = "Now using version:";
        checkingupdateMessage = "Checking for updates...";
        checkfailedMessage = "Failed to check for updates, please check your network connection!";
        updateavailableMessage = "An update is available:";
        updateurlMessage = "Download URL:";
        oldversionmaycauseproblemMessage = "An old version may cause problems, please update as soon as possible.";
        nowusinglatestversionMessage = "You are using the latest version.";
        reloadedMessage = "Configuration has been reloaded!";
        nopermissionMessage = "You don't have permission to execute this command!";
        serverPrefix = "Server ";
        playersPrefix = "Players: ";
        nowallplayercountMessage = "Current online players: ";
        singleserverplayeronlineMessage = " player(s) online";
        noplayersonlineMessage = "No players online";
    }
}
