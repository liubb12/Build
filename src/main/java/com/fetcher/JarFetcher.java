package com.fetcher;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class JarFetcher extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        if (getCommand("getjar") != null) {
            getCommand("getjar").setExecutor(this);
        }
        getLogger().info("JarFetcher enabled! Usage: /getjar <url> <filename.jar>");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /getjar <url> <filename.jar>");
            return true;
        }

        String fileUrl = args[0];
        String targetName = args[1];

        if (!targetName.endsWith(".jar")) {
            targetName += ".jar";
        }

        File pluginsDir = getDataFolder().getParentFile();
        File targetFile = new File(pluginsDir, targetName);

        String finalName = targetName;
        sender.sendMessage(ChatColor.YELLOW + "Starting download from: " + fileUrl);

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    String newUrl = conn.getHeaderField("Location");
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                }

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }
                    sender.sendMessage(ChatColor.GREEN + "Download finished! Saved " + totalBytes + " bytes to plugins/" + finalName);
                    sender.sendMessage(ChatColor.GREEN + "You can now run: plugman load " + finalName);
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Download failed: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return true;
    }
}
