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

        new ConfigManager(this).setupConfig();

        getCommand("floodgateprefixguard").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        printStartupBanner();
        new UpdateChecker(this, "muzone/FloodgatePrefixGuard").check(version -> {
            if (!this.getDescription().getVersion().equals(version) && getConfig().getBoolean("auto-update")) {
                new UpdateChecker(this, "muzone/FloodgatePrefixGuard").download(version);
            }
        });
    }
    
    private void printStartupBanner() {
        getLogger().info("FloodgatePrefixGuard Enabled.");
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
    public void onLogin(PlayerLoginEvent event) {
        FloodgateApi api = FloodgateApi.getInstance();
        UUID uuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getName();

        FloodgatePlayer fPlayer = api.getPlayer(uuid);

        if (fPlayer == null) {
            getLogger().info("[Log] Login Java: " + username + " (UUID: " + uuid + ")");
            return; 
        }

        if (getConfig().getBoolean("allow-linked-bypass") && fPlayer.getLinkedPlayer() != null) {
            getLogger().info("[Log] Login Bedrock (Linked): " + username + " -> Bypass Check.");
            return;
        }

        String requiredPrefix = getConfig().getString("required-prefix", ".");

        if (username.startsWith(requiredPrefix)) {
            getLogger().info("[Log] Login Bedrock (Valid): " + username + " -> Prefix aman.");
        } else {
            getLogger().warning("[BLOCK] Login Bedrock (Invalid): " + username + " -> Prefix hilang! Menendang pemain...");

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