package com.whiteiverson.staffchat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class StaffChatListener implements Listener {
    private static final String SEND_PERMISSION = "staffchat.send";
    private static final String RECEIVE_PERMISSION = "staffchat.receive";

    private final StaffChatSettings settings;

    public StaffChatListener(StaffChatSettings settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        String prefix = settings.getChatPrefix();

        if (prefix == null || prefix.isEmpty() || !message.startsWith(prefix)) {
            return;
        }

        if (!sender.hasPermission(SEND_PERMISSION) && !settings.isPublicSendingAllowed()) {
            sender.sendMessage(settings.getNoPermissionMessage());
            event.setCancelled(true);
            return;
        }

        String content = message.substring(prefix.length()).trim();
        if (content.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        String formattedMessage = settings.formatStaffMessage(sender.getDisplayName(), content);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(RECEIVE_PERMISSION)) {
                onlinePlayer.sendMessage(formattedMessage);
            }
        }
    }
}
