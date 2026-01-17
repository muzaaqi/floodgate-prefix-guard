package me.mumu.floodgateguard;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.geysermc.floodgate.api.FloodgateApi;

public final class FloodgatePrefixGuard extends JavaPlugin implements Listener {

    private static final String REQUIRED_PREFIX = ".";

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Floodgate") == null) {
            getLogger().severe("Floodgate not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FloodgatePrefixGuard enabled.");
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        FloodgateApi api = FloodgateApi.getInstance();

        if (!api.isFloodgatePlayer(event.getUniqueId())) {
            return;
        }

        String username = event.getName();

        if (!username.startsWith(REQUIRED_PREFIX)) {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                "§cInvalid Bedrock login detected.\n\n"
              + "§7Your account was not recognized correctly by Floodgate.\n"
              + "§7This would create a different UUID and corrupt your data.\n\n"
              + "§ePlease rejoin the server via the official Bedrock address.\n"
              + "§eIf the problem persists, contact an administrator."
            );
        }
    }
}
