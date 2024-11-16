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
    private static final String VERSION = "1.0";
    private final ProxyServer server;
    private Path dataDirectory;

    @Inject
    public MeowVeloList(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
        server.getConsoleCommandSource().sendMessage(Component.text(startupMessage));
        loadLanguage(); 
    }
    
    // 订阅初始化事件
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

                // 使用全新的分隔符
                String separator = "§a≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡";

                response.append(Component.text(separator).decorate(TextDecoration.BOLD)).append("\n");
                response.append(nowallplayercountMessage).append(totalPlayers).append("\n");
                response.append(separator).append("\n"); // 这行已替换为新的分隔符

                boolean firstServer = true; // 用于控制是否是第一个服务器

                for (RegisteredServer registeredServer : server.getAllServers()) {
                    String serverName = registeredServer.getServerInfo().getName();
                    List<String> playerNames = registeredServer.getPlayersConnected().stream()
                            .map(Player::getUsername)
                            .collect(Collectors.toList());

                    // 如果不是第一个服务器，添加分隔符
                    if (!firstServer) {
                        response.append(separator).append("\n"); // 每个服务器之间添加分隔符
                    }

                    // 添加当前服务器信息
                    response.append(serverPrefix).append(serverName).append(" §7(")
                            .append(playerNames.size()).append(singleserverplayeronlineMessage).append("§7)").append("\n");

                    // 玩家信息
                    if (!playerNames.isEmpty()) {
                        response.append(playersPrefix).append(String.join(", ", playerNames)).append("\n");
                    } else {
                        response.append(noplayersonlineMessage).append("\n");
                    }

                    firstServer = false; // 第一个服务器输出后，设置为false，后续服务器添加分隔符
                }

                response.append(separator).append("\n"); // 输出最后一个服务器信息后再加上分隔符

                source.sendMessage(Component.text(response.toString()));
            }
        });
        checkUpdate();
    }

    // 检查更新
    private void checkUpdate() {
        String currentVersion = VERSION; 
        server.getConsoleCommandSource().sendMessage(Component.text(nowusingversionMessage + currentVersion));
        server.getConsoleCommandSource().sendMessage(Component.text(checkingupdateMessage));
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
                    server.getConsoleCommandSource().sendMessage(Component.text(oldversionmaycauseproblemMessage));
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

    // 加载语言设置
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
        File pluginDir = new File("MeowVeloList");
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
        startupMessage = "[MeowVeloList] MeowVeloList 已加载！";
        nowusingversionMessage = "[MeowVeloList] 当前使用版本:";
        checkingupdateMessage = "[MeowVeloList] 正在检查更新...";
        checkfailedMessage = "[MeowVeloList] 检查更新失败，请检查你的网络状况！";
        updateavailableMessage = "[MeowVeloList] 有新版本可用：";
        updateurlMessage = "[MeowVeloList] 下载地址：";
        oldversionmaycauseproblemMessage = "[MeowVeloList] 使用旧版本可能会导致问题，请尽快更新！";
        nowusinglatestversionMessage = "[MeowVeloList] 正在使用最新版本！";
        nopermissionMessage = "你没有权限执行此命令！";
        nowallplayercountMessage = "§6当前在线人数：§e";
        singleserverplayeronlineMessage = " §e个在线玩家";
        noplayersonlineMessage = "§7当前没有在线玩家";
    }

    // 加载繁体中文消息
    private void loadChineseTraditionalMessages() {
        startupMessage = "[MeowVeloList] MeowVeloList 已加載！";
        nowusingversionMessage = "[MeowVeloList] 當前使用版本：";
        checkingupdateMessage = "[MeowVeloList] 正在檢查更新...";
        checkfailedMessage = "[MeowVeloList] 檢查更新失敗，請檢查你的網路狀況！";
        updateavailableMessage = "[MeowVeloList] 有新版本可用：";
        updateurlMessage = "[MeowVeloList] 下載地址：";
        oldversionmaycauseproblemMessage = "[MeowVeloList] 使用舊版本可能會導致問題，請盡快更新！";
        nowusinglatestversionMessage = "[MeowVeloList] 正在使用最新版本！";
        nopermissionMessage = "你沒有權限執行此命令！";
        nowallplayercountMessage = "§6當前線上人數：§e";
        singleserverplayeronlineMessage = " §e個在線玩家";
        noplayersonlineMessage = "§7當前沒有在線玩家";
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
    }
}
