package com.whiteiverson.staffchat;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;

public final class DiscordRelayService {
    private final StaffChatPlugin plugin;
    private final StaffChatSettings settings;

    private boolean warnedDiscordSrvUnavailable;
    private boolean warnedEssentialsUnavailable;
    private boolean warnedEssentialsCommandMissing;

    public DiscordRelayService(StaffChatPlugin plugin, StaffChatSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void relayStaffMessage(String senderName, String content) {
        if (!settings.isDiscordEnabled()) {
            return;
        }

        String provider = settings.getDiscordProvider().toUpperCase(Locale.ROOT);

        switch (provider) {
            case "DISCORDSRV":
                sendViaDiscordSrv(senderName, content);
                break;
            case "ESSENTIALS":
                sendViaEssentials(senderName, content);
                break;
            default:
                // AUTO: DiscordSRV first, Essentials fallback
                if (isPluginEnabled("DiscordSRV")) {
                    sendViaDiscordSrv(senderName, content);
                } else if (isPluginEnabled("EssentialsDiscord") || isPluginEnabled("EssentialsXDiscord")) {
                    sendViaEssentials(senderName, content);
                } else {
                    plugin.getLogger()
                            .fine("Discord relay enabled but no provider (DiscordSRV / EssentialsDiscord) is loaded.");
                }
                break;
        }
    }

    // ---- DiscordSRV --------------------------------------------------------

    private void sendViaDiscordSrv(String senderName, String content) {
        if (!isPluginEnabled("DiscordSRV")) {
            if (!warnedDiscordSrvUnavailable) {
                warnedDiscordSrvUnavailable = true;
                plugin.getLogger().warning("StaffChat: discord.provider is DISCORDSRV but DiscordSRV is not loaded.");
            }
            return;
        }

        String channelId = settings.getDiscordChannelId();
        if (channelId.isBlank()) {
            plugin.getLogger().warning("StaffChat: discord.channel-id is empty. Cannot send to Discord.");
            return;
        }

        String message = settings.formatDiscordMessage(senderName, content);

        // DiscordSRV is a provided dependency — on the compile classpath but not
        // bundled.
        // JDA (used by DiscordSRV internally) is accessed via reflection to avoid
        // needing
        // a separate JDA dependency declaration while still keeping clean DiscordSRV
        // imports.
        try {
            Object jda = DiscordSRV.getPlugin().getJda();
            if (jda == null) {
                plugin.getLogger().warning("StaffChat: DiscordSRV JDA instance is null — is the bot connected?");
                return;
            }

            Object channel = jda.getClass().getMethod("getTextChannelById", String.class).invoke(jda, channelId);
            if (channel == null) {
                plugin.getLogger().warning("StaffChat: Could not find Discord channel ID " + channelId
                        + ". Check the bot has access to it.");
                return;
            }

            // JDA 4 uses sendMessage(CharSequence); this reflection handles both versions
            // safely.
            Method sendMethod;
            try {
                sendMethod = channel.getClass().getMethod("sendMessage", CharSequence.class);
            } catch (NoSuchMethodException ignored) {
                sendMethod = channel.getClass().getMethod("sendMessage", String.class);
            }

            Object restAction = sendMethod.invoke(channel, message);
            restAction.getClass().getMethod("queue").invoke(restAction);
        } catch (Exception exception) {
            plugin.getLogger().warning("StaffChat: DiscordSRV relay failed — " + exception.getMessage());
        }
    }

    // ---- EssentialsXDiscord ------------------------------------------------

    private void sendViaEssentials(String senderName, String content) {
        if (!isPluginEnabled("EssentialsDiscord") && !isPluginEnabled("EssentialsXDiscord")) {
            if (!warnedEssentialsUnavailable) {
                warnedEssentialsUnavailable = true;
                plugin.getLogger()
                        .warning("StaffChat: discord.provider is ESSENTIALS but EssentialsDiscord is not loaded.");
            }
            return;
        }

        String commandTemplate = settings.getEssentialsRelayCommand();
        if (commandTemplate == null || commandTemplate.isBlank()) {
            if (!warnedEssentialsCommandMissing) {
                warnedEssentialsCommandMissing = true;
                plugin.getLogger()
                        .warning("StaffChat: discord.essentials-relay-command is empty. Cannot relay via Essentials.");
            }
            return;
        }

        String message = settings.formatDiscordMessage(senderName, content);
        String command = commandTemplate
                .replace("%sender%", senderName)
                .replace("%message%", message)
                .replace("%channel_id%", settings.getDiscordChannelId())
                .replace("%channel_name%", settings.getDiscordChannelName());

        // dispatchCommand must run on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    // ---- Helpers -----------------------------------------------------------

    private boolean isPluginEnabled(String pluginName) {
        Plugin loadedPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return loadedPlugin != null && loadedPlugin.isEnabled();
    }
}