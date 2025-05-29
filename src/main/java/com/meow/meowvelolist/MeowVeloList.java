package com.meow.meowvelolist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.Yaml;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.TextComponent;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.Collections;

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
    private String unknownCommandMessage;
    private String playerNotFoundMessage;
    private String playerInfoTitle;
    private String playerInfoUuid;
    private String playerInfoCurrentServer;
    private String playerInfoConnectionAddress;
    private String playerInfoConnectionPort;
    private String playerInfoGameVersion;
    private String playerInfoPing;
    private String checkPlayerNameRequired;
    private final ProxyServer server;
    private Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private static final String VERSION_URL = "https://api.github.com/repos/Zhang12334/MeowVeloList/releases/latest";
    private static final String DOWNLOAD_URL = "https://github.com/Zhang12334/MeowVeloList/releases/latest";
    private static final String separator = "§a≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡≡";
    private final PluginContainer pluginContainer;

    @Inject
    public MeowVeloList(ProxyServer server, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory, PluginContainer pluginContainer) {
        this.server = server;
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;
        loadLanguage(); 
        server.getConsoleCommandSource().sendMessage(Component.text(startupMessage));
        this.metricsFactory = metricsFactory;
    }

    // 订阅初始化事件
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        int pluginId = 23904;
        Metrics metrics = metricsFactory.make(this, pluginId);
        server.getCommandManager().register("mlist", new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                CommandSource source = invocation.source();
                String[] args = invocation.arguments();

                if (!source.hasPermission("meowvelolist.mlist.use")) {
                    source.sendMessage(Component.text(nopermissionMessage));
                    return;
                }

                // 如果没有参数，显示玩家列表
                if (args.length == 0) {
                    showPlayerList(source);
                    return;
                }

                // 处理子命令
                switch (args[0].toLowerCase()) {
                    case "reload":
                        if (!source.hasPermission("meowvelolist.mlist.reload")) {
                            source.sendMessage(Component.text(nopermissionMessage));
                            return;
                        }
                        loadLanguage();
                        source.sendMessage(Component.text(reloadsuccessMessage));
                        break;

                    case "check":
                        if (!source.hasPermission("meowvelolist.mlist.check")) {
                            source.sendMessage(Component.text(nopermissionMessage));
                            return;
                        }
                        if (args.length < 2) {
                            source.sendMessage(Component.text(checkPlayerNameRequired));
                            return;
                        }
                        checkPlayerInfo(source, args[1]);
                        break;

                    default:
                        source.sendMessage(Component.text(unknownCommandMessage));
                        break;
                }
            }

            @Override
            public List<String> suggest(Invocation invocation) {
                String[] args = invocation.arguments();
                String partialArg = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
                
                // 如果没有参数，返回子命令列表
                if (args.length == 0) {
                    return Arrays.asList("reload", "check");
                }
                
                // 如果第一个参数是 check，返回在线玩家列表
                if (args.length == 1 && "check".startsWith(partialArg)) {
                    return Collections.singletonList("check");
                }
                
                // 如果第一个参数是 check，第二个参数补全在线玩家
                if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
                    return server.getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(name -> name.toLowerCase().startsWith(partialArg))
                            .collect(Collectors.toList());
                }
                
                return Collections.emptyList();
            }
        });

        // 异步执行更新检查
        CompletableFuture.runAsync(() -> checkUpdate());
    }

    /**
     * 显示玩家列表
     */
    private void showPlayerList(CommandSource source) {
        int totalPlayers = server.getAllPlayers().size();
        TextComponent.Builder response = Component.text();

        response.append(Component.text(separator)).append(Component.newline());
        response.append(Component.text(nowallplayercountMessage + totalPlayers)).append(Component.newline());
        response.append(Component.text(separator)).append(Component.newline());

        boolean firstServer = true;

        for (RegisteredServer registeredServer : server.getAllServers()) {
            String serverName = registeredServer.getServerInfo().getName();
            List<String> playerNames = registeredServer.getPlayersConnected().stream()
                    .map(Player::getUsername)
                    .sorted((s1, s2) -> {
                        int lowerCompare = s1.toLowerCase().compareTo(s2.toLowerCase());
                        if (lowerCompare != 0) {
                            return lowerCompare;
                        }
                        return s1.compareTo(s2);
                    })
                    .collect(Collectors.toList());

            if (!firstServer) {
                response.append(Component.text(separator)).append(Component.newline());
            }

            response.append(Component.text(serverPrefix + serverName + " §7(" + playerNames.size() + singleserverplayeronlineMessage + "§7)").append(Component.newline()));

            if (!playerNames.isEmpty()) {
                response.append(Component.text(playersPrefix + String.join(", ", playerNames)).append(Component.newline()));
            } else {
                response.append(Component.text(noplayersonlineMessage)).append(Component.newline());
            }

            firstServer = false;
        }

        response.append(Component.text(separator)).append(Component.newline());
        source.sendMessage(response.build());
    }

    /**
     * 检查玩家信息
     */
    private void checkPlayerInfo(CommandSource source, String playerName) {
        Player targetPlayer = server.getPlayer(playerName).orElse(null);
        if (targetPlayer == null) {
            source.sendMessage(Component.text(playerNotFoundMessage + playerName));
            return;
        }

        TextComponent.Builder response = Component.text();

        response.append(Component.text(separator)).append(Component.newline());
        response.append(Component.text(playerInfoTitle + targetPlayer.getUsername())).append(Component.newline());
        // 基本信息
        response.append(Component.text(playerInfoUuid + targetPlayer.getUniqueId())).append(Component.newline());
        response.append(Component.text(playerInfoCurrentServer + targetPlayer.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("未知"))).append(Component.newline());
        response.append(Component.text(playerInfoConnectionAddress + targetPlayer.getRemoteAddress().getAddress().getHostAddress())).append(Component.newline());
        response.append(Component.text(playerInfoConnectionPort + targetPlayer.getRemoteAddress().getPort())).append(Component.newline());
        response.append(Component.text(playerInfoGameVersion + targetPlayer.getProtocolVersion().getProtocol())).append(Component.newline());
        response.append(Component.text(playerInfoPing + targetPlayer.getPing() + "ms")).append(Component.newline());
        response.append(Component.text(separator)).append(Component.newline());
        source.sendMessage(response.build());
    }

    /**
     * 检查更新
     */
    public void checkUpdate() {
        String currentVersion = pluginContainer.getDescription().getVersion().orElse("unknown");
        server.getConsoleCommandSource().sendMessage(Component.text(nowusingversionMessage + currentVersion));
        server.getConsoleCommandSource().sendMessage(Component.text(checkingupdateMessage));

        try {
            // 使用 GitHub API 获取最新版本信息
            String response = fetchVersionInfo();
            JSONObject json = new JSONObject(response);
            String latestVersion = json.getString("tag_name"); // 获取最新版本号

            // 比较版本号
            if (isVersionGreater(latestVersion, currentVersion)) {
                server.getConsoleCommandSource().sendMessage(Component.text(updateavailableMessage + latestVersion));
                server.getConsoleCommandSource().sendMessage(Component.text(updateurlMessage + DOWNLOAD_URL));
                server.getConsoleCommandSource().sendMessage(Component.text(oldversionmaycauseproblemMessage));
            } else {
                server.getConsoleCommandSource().sendMessage(Component.text(nowusinglatestversionMessage));
            }
        } catch (Exception e) {
            server.getConsoleCommandSource().sendMessage(Component.text(checkfailedMessage));
        }
    }

    /**
     * 获取版本信息
     * @return JSON 响应
     * @throws Exception 如果网络请求失败
     */
    private String fetchVersionInfo() throws Exception {
        URL url = new URI(VERSION_URL).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("HTTP 响应码: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();

        return response.toString();
    }

    /**
     * 比较版本号（支持 1.1.1 格式）
     * @param latestVersion 最新版本号
     * @param currentVersion 当前版本号
     * @return 如果 latestVersion > currentVersion，返回 true
     */
    private boolean isVersionGreater(String latestVersion, String currentVersion) {
        // 移除可能的 "v" 前缀
        latestVersion = latestVersion.replaceFirst("^v", "");
        currentVersion = currentVersion.replaceFirst("^v", "");

        String[] v1Parts = latestVersion.split("\\.");
        String[] v2Parts = currentVersion.split("\\.");

        for (int i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            if (v1Part > v2Part) return true;
            if (v1Part < v2Part) return false;
        }
        return false;
    }

    // 加载语言设置
    private void loadLanguage() {
        String language = getLanguageFromConfig(); 

        if ("zh_hans".equalsIgnoreCase(language)) {
            loadChineseSimplifiedMessages();
        } else if ("zh_hant".equalsIgnoreCase(language)) {
            loadChineseTraditionalMessages();
        } else if ("en_us".equalsIgnoreCase(language)) {
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
            return (String) config.getOrDefault("language", "zh_hans");
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
                config.put("language", "zh_hans");

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
        unknownCommandMessage = "§c未知的子命令！可用命令：/mlist [reload|check <玩家名>]";
        playerNotFoundMessage = "§c找不到玩家 ";
        playerInfoTitle = "§e玩家信息 - ";
        playerInfoUuid = "§6UUID: §f";
        playerInfoCurrentServer = "§6当前服务器: §f";
        playerInfoConnectionAddress = "§6客户端IP: §f";
        playerInfoConnectionPort = "§6客户端端口: §f";
        playerInfoGameVersion = "§6协议版本: §f";
        playerInfoPing = "§6Ping: §f";
        checkPlayerNameRequired = "§c请指定要查询的玩家名称！";
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
        unknownCommandMessage = "§c未知的子命令！可用命令：/mlist [reload|check <玩家名>]";
        playerNotFoundMessage = "§c找不到玩家 ";
        playerInfoTitle = "§e玩家信息 - ";
        playerInfoUuid = "§6UUID: §f";
        playerInfoCurrentServer = "§6當前伺服器: §f";
        playerInfoConnectionAddress = "§6客戶端IP: §f";
        playerInfoConnectionPort = "§6客戶端端口: §f";
        playerInfoGameVersion = "§6協議版本: §f";
        playerInfoPing = "§6Ping: §f";
        checkPlayerNameRequired = "§c請指定要查詢的玩家名稱！";
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
        unknownCommandMessage = "§cUnknown subcommand! Available commands: /mlist [reload|check <player>]";
        playerNotFoundMessage = "§cPlayer not found: ";
        playerInfoTitle = "§ePlayer Info - ";
        playerInfoUuid = "§6UUID: §f";
        playerInfoCurrentServer = "§6Current Server: §f";
        playerInfoConnectionAddress = "§6Client IP: §f";
        playerInfoConnectionPort = "§6Client Port: §f";
        playerInfoGameVersion = "§6Protocol Version: §f";
        playerInfoPing = "§6Ping: §f";
        checkPlayerNameRequired = "§cPlease specify a player name!";
    }
}
