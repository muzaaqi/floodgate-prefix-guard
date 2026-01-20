package me.muzone.floodgateprefixguard;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class FloodgatePrefixGuard extends JavaPlugin implements Listener, CommandExecutor {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Floodgate") == null) {
            getLogger().severe("Floodgate not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("Geyser-Spigot") == null) {
            getLogger().severe("Geyser-Spigot not found! This plugin requires Geyser.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new ConfigManager(this).setupConfig();
        getCommand("floodgateprefixguard").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info(ANSI_GREEN + "FloodgatePrefixGuard enabled." + ANSI_RESET);
        printStartupBanner();

        new UpdateChecker(this, "muzaaqi/floodgate-prefix-guard").check(newVersion -> {
            String currentVersion = getDescription().getVersion();
            if (!currentVersion.equalsIgnoreCase(newVersion)) {
                getLogger().info(ANSI_YELLOW + "----------------------------------------" + ANSI_RESET);
                getLogger().info(ANSI_YELLOW + " UPDATE AVAILABLE: v" + newVersion + ANSI_RESET);
                if (getConfig().getBoolean("auto-update")) {
                    getLogger().info(ANSI_GREEN + " Auto-Update enabled. Downloading update..." + ANSI_RESET);
                    new UpdateChecker(this, "muzaaqi/floodgate-prefix-guard").download(newVersion);
                } else {
                    getLogger().info(ANSI_CYAN + " Manual Download: https://github.com/muzaaqi/floodgate-prefix-guard/releases" + ANSI_RESET);
                }
                getLogger().info(ANSI_YELLOW + "----------------------------------------" + ANSI_RESET);
            }
        });
    }

    private void printStartupBanner() {
        getLogger().info(ANSI_CYAN + "========================================" + ANSI_RESET);
        getLogger().info(ANSI_CYAN + "   FloodgatePrefixGuard v" + getDescription().getVersion() + ANSI_RESET);
        getLogger().info(ANSI_GREEN + "   Protection: Active (UUID + Name Fallback)" + ANSI_RESET);
        getLogger().info(ANSI_CYAN + "========================================" + ANSI_RESET);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("fpg.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }
            new ConfigManager(this).setupConfig();
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "[FloodgatePrefixGuard] Configuration reloaded!");
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(PlayerLoginEvent event) {
        FloodgateApi floodgateApi = FloodgateApi.getInstance();
        GeyserApi geyserApi = GeyserApi.api();
        
        UUID uuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getName();

        boolean isBedrock = false;
        String detectionSource = "None";

        if (floodgateApi.isFloodgatePlayer(uuid)) {
            isBedrock = true;
            detectionSource = "FloodgateAPI";
        }
        
        if (!isBedrock && geyserApi.isBedrockPlayer(uuid)) {
            isBedrock = true;
            detectionSource = "GeyserAPI (UUID)";
        }

        if (!isBedrock) {
            for (GeyserConnection conn : geyserApi.onlineConnections()) {
                if (conn.javaName().equals(username)) {
                    isBedrock = true;
                    detectionSource = "GeyserAPI (Name Match)";
                    break;
                }
            }
        }

        if (!isBedrock) {
            getLogger().info("[Log] Java Login: " + username + " (UUID: " + uuid + ")");
            return;
        }

        getLogger().info("[Audit] Bedrock detected via " + detectionSource + ": " + username);

        FloodgatePlayer fPlayer = floodgateApi.getPlayer(uuid);
        if (fPlayer != null && getConfig().getBoolean("allow-linked-bypass") && fPlayer.getLinkedPlayer() != null) {
            getLogger().info("[Log] Bedrock Login (Linked): " + username + " -> Bypass Check.");
            return;
        }

        String requiredPrefix = getConfig().getString("required-prefix", ".");

        if (username.startsWith(requiredPrefix)) {
            getLogger().info("[Log] Bedrock Login (Valid): " + username + " -> Safe prefix.");
        } else {
            getLogger().warning("[BLOCK] Bedrock Login (Invalid): " + username + " -> Missing prefix! Detected via " + detectionSource);

            List<String> msgList = getConfig().getStringList("kick-message");
            String kickReason = msgList.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.joining("\n"));

            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickReason);

            if (getConfig().getBoolean("staff-notify")) {
                String notifyMsg = ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("staff-notify-message", "&c[Guard] %player% kicked.")
                    .replace("%player%", username));
                getServer().broadcast(notifyMsg, "fpg.notify");
            }
        }
    }
}