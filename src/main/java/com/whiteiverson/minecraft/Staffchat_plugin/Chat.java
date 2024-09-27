package com.whiteiverson.minecraft.Staffchat_plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class Chat implements Listener {
    private final Plugin plugin = Main.getPlugin(Main.class);

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Get chat and plugin prefixes from config
        String chatPrefix = plugin.getConfig().getString("chat-prefix");
        String pluginPrefix = plugin.getConfig().getString("plugin-prefix");

        // Check if message starts with the chat prefix and the player has the required permission
        if (message.startsWith(chatPrefix) && player.hasPermission("staffchat.send")) {
            // Construct the staff chat message
            String formattedMessage = ChatColor.translateAlternateColorCodes('&', 
                pluginPrefix + " " + player.getDisplayName() + ChatColor.DARK_GRAY + " > " 
                + ChatColor.GRAY + message.substring(chatPrefix.length()));

            // Cancel the public chat event
            event.setCancelled(true);

            // Send the message to all players with the 'staffchat.receive' permission
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("staffchat.receive")) {
                    onlinePlayer.sendMessage(formattedMessage);
                }
            }
        }
    }
}