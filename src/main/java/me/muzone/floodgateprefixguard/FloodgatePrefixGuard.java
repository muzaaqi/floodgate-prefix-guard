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

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Floodgate") == null) {
            getLogger().severe("Floodgate not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        getCommand("floodgateprefixguard").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("FloodgatePrefixGuard enabled.");

        printStartupBanner();

        new UpdateChecker(this, "muzaaqi/floodgate-prefix-guard").getVersion(version -> {
            if (!this.getDescription().getVersion().equals(version)) {
                getLogger().info("------------------------------------");
                getLogger().info("UPDATE AVAILABLE!");
                getLogger().info("Current version: " + this.getDescription().getVersion());
                getLogger().info("New version: " + version);
                getLogger().info("Download: https://github.com/muzaaqi/floodgate-prefix-guard/releases");
                getLogger().info("------------------------------------");
            } else {
                getLogger().info("You are running the latest version.");
            }
        });
    }

    private void printStartupBanner() {
        getLogger().info(ChatColor.AQUA + "========================================");
        getLogger().info(ChatColor.AQUA + "   FloodgatePrefixGuard v" + getDescription().getVersion());
        getLogger().info(ChatColor.AQUA + "   Created by " + getDescription().getAuthors());
        getLogger().info("");
        getLogger().info(ChatColor.GREEN + "   Status: Enabled");
        getLogger().info(ChatColor.GREEN + "   Protection: Active");
        if (getConfig().getBoolean("staff-notify")) {
            getLogger().info(ChatColor.YELLOW + "   Staff Notify: ON");
        }
        getLogger().info(ChatColor.AQUA + "========================================");
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

        if (fPlayer != null) {
            if (getConfig().getBoolean("allow-linked-bypass") && fPlayer.getLinkedPlayer() != null) {
                return; 
            }

            String requiredPrefix = getConfig().getString("required-prefix", ".");

            if (!username.startsWith(requiredPrefix)) {
                
                List<String> msgList = getConfig().getStringList("kick-message");
                String kickReason = msgList.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.joining("\n"));

                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickReason);
                getLogger().warning("Blocked Bedrock player " + username + " (Missing Prefix).");

                if (getConfig().getBoolean("staff-notify")) {
                    String notifyMsg = ChatColor.translateAlternateColorCodes('&', 
                        getConfig().getString("staff-notify-message", "&c[Guard] %player% kicked.")
                        .replace("%player%", username));
                    
                    getServer().broadcast(notifyMsg, "fpg.notify");
                }
            }
        }
    }
}