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

import javax.inject.Inject;
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

    private final ProxyServer server;

    @Inject
    public MeowVeloList(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Register the command /meowlist
        server.getCommandManager().register("meowlist", new PlayerInfoCommand(server));
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
            StringBuilder response = new StringBuilder("§aTotal Online Players: " + totalPlayers + "\n");

            // Get online players per server
            for (RegisteredServer registeredServer : server.getAllServers()) {
                String serverName = registeredServer.getServerInfo().getName();
                List<String> playerNames = registeredServer.getPlayersConnected().stream()
                        .map(Player::getUsername)
                        .collect(Collectors.toList());

                response.append("§eServer: ").append(serverName).append(" §7(").append(playerNames.size()).append(")\n");
                response.append("§bPlayers: ").append(String.join(", ", playerNames)).append("\n");
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
}
