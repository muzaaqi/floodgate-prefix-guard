package me.muzone.floodgateprefixguard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String githubRepo;

    public UpdateChecker(JavaPlugin plugin, String githubRepo) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
    }

    public void check(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    Scanner scanner = new Scanner(connection.getInputStream());
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNext()) {
                        response.append(scanner.next());
                    }
                    scanner.close();

                    String json = response.toString();
                    if (json.contains("\"tag_name\":\"")) {
                        String version = json.split("\"tag_name\":\"")[1].split("\"")[0];
                        consumer.accept(version.replace("v", ""));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Gagal mengecek update: " + e.getMessage());
            }
        });
    }

    public void download(String version) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("Memulai proses download update v" + version + "...");

                String fileName = plugin.getName() + "-" + version + ".jar";
                URL downloadUrl = new URL("https://github.com/" + githubRepo + "/releases/download/v" + version + "/" + fileName);

                File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
                if (!updateFolder.exists()) {
                    updateFolder.mkdirs();
                }

                File outputFile = new File(updateFolder, plugin.getName() + "-" + version + ".jar");

                try (ReadableByteChannel rbc = Channels.newChannel(downloadUrl.openStream());
                    FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }

                plugin.getLogger().info("Update v" + version + " berhasil diunduh!");
                plugin.getLogger().info("Update akan terpasang otomatis saat restart server berikutnya.");
                
            } catch (IOException e) {
                plugin.getLogger().severe("Gagal mengunduh update: " + e.getMessage());
                plugin.getLogger().severe("Silakan download manual di: https://github.com/" + githubRepo + "/releases");
            }
        });
    }
}