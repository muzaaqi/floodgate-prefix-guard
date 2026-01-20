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
    private final int resourceId;
    private final String githubRepo;

    public UpdateChecker(JavaPlugin plugin, String githubRepo) {
        this.plugin = plugin;
        this.resourceId = 0;
        this.githubRepo = githubRepo;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
                InputStream inputStream = url.openStream();
                Scanner scanner = new Scanner(inputStream);
                
                StringBuilder jsonResult = new StringBuilder();
                while (scanner.hasNext()) {
                    jsonResult.append(scanner.next());
                }
                
                String json = jsonResult.toString();
                if (json.contains("\"tag_name\":\"")) {
                    String version = json.split("\"tag_name\":\"")[1].split("\"")[0];
                    version = version.replace("v", "");
                    consumer.accept(version);
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Cannot look for updates: " + exception.getMessage());
            }
        });
    }
}