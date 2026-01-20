package me.muzone.floodgateprefixguard;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;

public class ConfigManager {

    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupConfig() {
        plugin.saveDefaultConfig();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        
        FileConfiguration defaultJarConfig = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(plugin.getResource("config.yml"))
        );

        int currentVersion = currentConfig.getInt("config-version", 0);
        int newVersion = defaultJarConfig.getInt("config-version", 1);

        if (currentVersion < newVersion) {
            plugin.getLogger().info("Mendeteksi config lama (v" + currentVersion + "). Melakukan migrasi ke v" + newVersion + "...");
            migrateConfig(configFile, currentConfig, defaultJarConfig);
        }
    }

    private void migrateConfig(File configFile, FileConfiguration oldConfig, FileConfiguration newJarConfig) {
        File backupFile = new File(plugin.getDataFolder(), "config-backup-v" + oldConfig.getInt("config-version") + ".yml");
        if (configFile.renameTo(backupFile)) {
            plugin.getLogger().info("Backup config lama disimpan sebagai: " + backupFile.getName());
        } else {
            plugin.getLogger().log(Level.SEVERE, "Gagal membuat backup config! Migrasi dibatalkan.");
            return;
        }

        plugin.saveResource("config.yml", true);
        
        FileConfiguration finalConfig = YamlConfiguration.loadConfiguration(configFile);

        Set<String> keys = newJarConfig.getKeys(true);
        
        for (String key : keys) {
            if (key.equals("config-version")) continue;

            if (oldConfig.contains(key)) {
                finalConfig.set(key, oldConfig.get(key));
            }
        }

        try {
            finalConfig.save(configFile);
            plugin.getLogger().info("Migrasi config sukses! Settingan lama Anda telah dipulihkan.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal menyimpan config hasil migrasi!", e);
        }
    }
}