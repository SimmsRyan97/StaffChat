package com.whiteiverson.staffchat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
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
        boolean sent;

        switch (provider) {
            case "DISCORDSRV":
                sent = sendViaDiscordSrv(senderName, content);
                break;
            case "ESSENTIALS":
                sent = sendViaEssentialsCommand(senderName, content);
                break;
            default:
                sent = autoSend(senderName, content);
                break;
        }

        if (!sent && provider.equals("AUTO")) {
            plugin.getLogger().fine("Discord relay was enabled, but no provider was available for this message.");
        }
    }

    private boolean autoSend(String senderName, String content) {
        if (isPluginEnabled("DiscordSRV")) {
            return sendViaDiscordSrv(senderName, content);
        }

        if (isPluginEnabled("Essentials") || isPluginEnabled("EssentialsDiscord") || isPluginEnabled("EssentialsXDiscord")) {
            return sendViaEssentialsCommand(senderName, content);
        }

        return false;
    }

    private boolean sendViaDiscordSrv(String senderName, String content) {
        if (!isPluginEnabled("DiscordSRV")) {
            if (!warnedDiscordSrvUnavailable) {
                warnedDiscordSrvUnavailable = true;
                plugin.getLogger().warning("Discord relay provider is DiscordSRV, but DiscordSRV is not enabled.");
            }
            return false;
        }

        String message = settings.formatDiscordMessage(senderName, content);

        try {
            Class<?> discordSrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Object discordSrvPlugin = discordSrvClass.getMethod("getPlugin").invoke(null);
            if (discordSrvPlugin == null) {
                return false;
            }

            Object jda = discordSrvPlugin.getClass().getMethod("getJda").invoke(discordSrvPlugin);
            if (jda == null) {
                return false;
            }

            Object textChannel = resolveTextChannel(jda);
            if (textChannel == null) {
                plugin.getLogger().warning("DiscordSRV relay could not find a text channel. Check discord.channel-id or discord.channel-name.");
                return false;
            }

            Method sendMessageMethod = textChannel.getClass().getMethod("sendMessage", CharSequence.class);
            Object action = sendMessageMethod.invoke(textChannel, message);
            action.getClass().getMethod("queue").invoke(action);
            return true;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("DiscordSRV relay failed: " + exception.getMessage());
            return false;
        }
    }

    private Object resolveTextChannel(Object jda) throws ReflectiveOperationException {
        String channelId = settings.getDiscordChannelId();
        String channelName = settings.getDiscordChannelName();

        if (channelId != null && !channelId.isBlank()) {
            Method byId = jda.getClass().getMethod("getTextChannelById", String.class);
            Object channel = byId.invoke(jda, channelId);
            if (channel != null) {
                return channel;
            }
        }

        if (channelName != null && !channelName.isBlank()) {
            Method byName = jda.getClass().getMethod("getTextChannelsByName", String.class, boolean.class);
            Object channels = byName.invoke(jda, channelName, true);
            if (channels instanceof List) {
                List<?> list = (List<?>) channels;
                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
        }

        return null;
    }

    private boolean sendViaEssentialsCommand(String senderName, String content) {
        if (!isPluginEnabled("Essentials") && !isPluginEnabled("EssentialsDiscord") && !isPluginEnabled("EssentialsXDiscord")) {
            if (!warnedEssentialsUnavailable) {
                warnedEssentialsUnavailable = true;
                plugin.getLogger().warning("Discord relay provider is Essentials, but no Essentials plugin was detected.");
            }
            return false;
        }

        String commandTemplate = settings.getEssentialsRelayCommand();
        if (commandTemplate == null || commandTemplate.isBlank()) {
            if (!warnedEssentialsCommandMissing) {
                warnedEssentialsCommandMissing = true;
                plugin.getLogger().warning("discord.essentials-relay-command is empty. Cannot relay via Essentials.");
            }
            return false;
        }

        String command = commandTemplate
            .replace("%sender%", senderName)
            .replace("%message%", settings.formatDiscordMessage(senderName, content))
            .replace("%channel_id%", settings.getDiscordChannelId())
            .replace("%channel_name%", settings.getDiscordChannelName());

        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        return true;
    }

    private boolean isPluginEnabled(String pluginName) {
        Plugin loadedPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return loadedPlugin != null && loadedPlugin.isEnabled();
    }
}