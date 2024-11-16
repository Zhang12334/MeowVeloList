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
        id = "meowvelolist", // 插件ID
        name = "MeowVeloList", // 插件名称
        version = "1.0", // 插件版本
        description = "一个在 Velocity 端显示玩家列表的插件", // 插件描述
        authors = {"Zhang1233"} // 插件作者
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

    // 构造函数，注入 ProxyServer 和数据目录路径
    @Inject
    public MeowVeloList(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        loadLanguage(); // 载入语言
    }

    // 代理初始化时注册命令
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 注册 /meowlist 命令
        server.getCommandManager().register("meowlist", new PlayerInfoCommand(server));
        // 检查更新
        checkUpdate();
    }

    // 加载语言设置
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

    // 显示玩家信息的命令
    private static class PlayerInfoCommand implements SimpleCommand {
        private final ProxyServer server;

        public PlayerInfoCommand(ProxyServer server) {
            this.server = server;
        }

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            
            // 获取在线玩家总数
            int totalPlayers = server.getAllPlayers().size();
            StringBuilder response = new StringBuilder(MeowVeloList.getInstance().nowusingversionMessage + ": " + totalPlayers + "\n");

            // 获取每个服务器的在线玩家
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

            // 向玩家或控制台发送信息
            source.sendMessage(Component.text(response.toString()));
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            return List.of(); // 该命令没有建议
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return true; // 所有玩家和控制台都有权限使用此命令
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
                // 拼接最终的请求URL
                HttpURLConnection connection = (HttpURLConnection) new URL(url + latestVersionUrl).openConnection();
                connection.setInstanceFollowRedirects(false); // 禁止自动重定向

                // 发送请求并获取响应代码
                int responseCode = connection.getResponseCode();

                // 如果响应是 302，则获取重定向地址
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    String redirectUrl = connection.getHeaderField("Location");

                    // 这里可以解析新版本的下载链接
                    // 例如重定向 URL 是 https://github.com/Zhang12334/MeowVeloList/releases/tag/1.1
                    if (redirectUrl != null && redirectUrl.contains("github.com")) {
                        latestVersion = extractVersionFromRedirectUrl(redirectUrl);
                        break;
                    }
                }
            }

            if (latestVersion == null) {
                source.sendMessage(Component.text(checkfailedMessage));  // 如果无法获取最新版本
            } else {
                // 比较当前版本和最新版本
                if (!currentVersion.equals(latestVersion)) {
                    source.sendMessage(Component.text(updateavailableMessage + latestVersion));
                    source.sendMessage(Component.text(updateurlMessage + latestVersionUrl));  // 这里可以替换成具体的下载链接
                } else {
                    source.sendMessage(Component.text(nowusinglatestversionMessage));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            source.sendMessage(Component.text(checkfailedMessage));
        }
    }

    // 解析重定向后的URL，提取版本信息
    private String extractVersionFromRedirectUrl(String redirectUrl) {
        // 解析GitHub的URL，假设格式是 https://github.com/username/project/releases/tag/1.1
        String versionPrefix = "tag/";
        int versionIndex = redirectUrl.lastIndexOf(versionPrefix);
        if (versionIndex != -1) {
            return redirectUrl.substring(versionIndex + versionPrefix.length());
        }
        return null;
    }

}
