package me.muzone.floodgateprefixguard;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

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
        getLogger().info("FloodgatePrefixGuard berhasil diaktifkan.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        FloodgateApi api = FloodgateApi.getInstance();
        UUID uuid = event.getUniqueId();
        String username = event.getName();

        if (api.isFloodgatePlayer(uuid)) {
            if (!username.startsWith(REQUIRED_PREFIX)) {
                event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    "§c[Guard] Invalid Bedrock Identity\n\n"
                    + "§7A synchronization error occurred with your name prefix.\n"
                    + "§7To prevent UUID conflicts and potential data loss, your login was aborted.\n\n"
                    + "§eSolution: Please try rejoining the server."
                );
            }
        }
    }
}