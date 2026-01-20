package me.muzone.floodgateprefixguard;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

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
            getLogger().severe(ANSI_RED + "Floodgate not found! Disabling plugin." + ANSI_RESET);
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
                    getLogger().info(ANSI_CYAN + " Set 'auto-update: true' in config for automatic updates." + ANSI_RESET);
                }
                
                getLogger().info(ANSI_YELLOW + "----------------------------------------" + ANSI_RESET);
            }
        });
    }

    private void printStartupBanner() {
        getLogger().info(ANSI_CYAN + "========================================" + ANSI_RESET);
        getLogger().info(ANSI_CYAN + "   FloodgatePrefixGuard v" + getDescription().getVersion() + ANSI_RESET);
        getLogger().info(ANSI_CYAN + "   Created by " + getDescription().getAuthors() + ANSI_RESET);
        getLogger().info("");
        getLogger().info(ANSI_GREEN + "   Status: Enabled" + ANSI_RESET);
        getLogger().info(ANSI_GREEN + "   Protection: Active" + ANSI_RESET);
        
        if (getConfig().getBoolean("staff-notify")) {
            getLogger().info(ANSI_YELLOW + "   Staff Notify: ON" + ANSI_RESET);
        } else {
            getLogger().info(ANSI_RED + "   Staff Notify: OFF" + ANSI_RESET);
        }
        
        getLogger().info(ANSI_CYAN + "========================================" + ANSI_RESET);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("fpg.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }
            
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "[FloodgatePrefixGuard] Configuration reloaded!");
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        FloodgateApi api = FloodgateApi.getInstance();
        UUID uuid = event.getUniqueId();
        String username = event.getName();

        FloodgatePlayer fPlayer = api.getPlayer(uuid);
        
        if (fPlayer == null) {
            getLogger().info("[Log] Login Java: " + username + " (UUID: " + uuid + ")");
            return;
        }

        if (getConfig().getBoolean("allow-linked-bypass") && fPlayer.getLinkedPlayer() != null) {
            getLogger().info("[Log] Login Bedrock (Linked): " + username + " -> Terhubung ke akun Java (Bypass Check).");
            return;
        }

        String requiredPrefix = getConfig().getString("required-prefix", ".");

        if (username.startsWith(requiredPrefix)) {
            getLogger().info("[Log] Login Bedrock (Valid): " + username + " -> Prefix sesuai.");
        } else {

            getLogger().warning("[BLOCK] Login Bedrock (Invalid): " + username + " -> Prefix hilang! Menendang pemain...");

            List<String> msgList = getConfig().getStringList("kick-message");
            String kickReason = msgList.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.joining("\n"));

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickReason);

            if (getConfig().getBoolean("staff-notify")) {
                String notifyMsg = ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("staff-notify-message", "&c[Guard] %player% kicked.")
                    .replace("%player%", username));
                
                getServer().broadcast(notifyMsg, "fpg.notify");
            }
        }
    }
}