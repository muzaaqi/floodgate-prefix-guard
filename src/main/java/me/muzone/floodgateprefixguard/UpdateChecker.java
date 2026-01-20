package me.muzone.floodgateprefixguard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final int resourceId; // Jika nanti upload ke SpigotMC
    private final String githubRepo; // Format: "Username/RepoName"

    public UpdateChecker(JavaPlugin plugin, String githubRepo) {
        this.plugin = plugin;
        this.resourceId = 0; // Belum dipakai, persiapan jika ke SpigotMC
        this.githubRepo = githubRepo;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Cek ke GitHub API
                URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
                InputStream inputStream = url.openStream();
                Scanner scanner = new Scanner(inputStream);
                
                StringBuilder jsonResult = new StringBuilder();
                while (scanner.hasNext()) {
                    jsonResult.append(scanner.next());
                }
                
                // Parsing JSON manual (karena kita menghindari dependensi library berat)
                // Kita cari bagian "tag_name":"v1.0.0"
                String json = jsonResult.toString();
                if (json.contains("\"tag_name\":\"")) {
                    String version = json.split("\"tag_name\":\"")[1].split("\"")[0];
                    // Hapus 'v' jika ada (misal v1.0.0 -> 1.0.0)
                    version = version.replace("v", "");
                    consumer.accept(version);
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Cannot look for updates: " + exception.getMessage());
            }
        });
    }
}