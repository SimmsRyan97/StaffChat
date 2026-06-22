package com.whiteiverson.staffchat;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StaffChatSettings {
    private static final Pattern ANGLE_HEX_PATTERN = Pattern.compile("(?i)<#([A-F0-9]{6})>");
    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("(?i)&#([A-F0-9]{6})");
    private static final Pattern BARE_HEX_PATTERN = Pattern.compile("(?i)(?<![A-Z0-9])#([A-F0-9]{6})");

    private final JavaPlugin plugin;

    private String chatPrefix;
    private String formattedMessage;
    private boolean allowPublicSending;

    private String noPermissionMessage;
    private String reloadedMessage;
    private String helpHeader;
    private String helpReloadLine;

    private boolean discordEnabled;
    private String discordProvider;
    private String discordChannelKey;
    private String discordChannelId;
    private String discordChannelName;
    private boolean discordOutboundEnabled;
    private boolean discordInboundEnabled;
    private boolean discordInboundAddPrefix;
    private String pluginPrefix;

    public StaffChatSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();

        chatPrefix = config.getString("chat.prefix", "@");
        allowPublicSending = config.getBoolean("chat.allow-public-sending", true);

        pluginPrefix = colorize(config.getString("style.plugin-prefix", "&6[StaffChat]"));
        String template = config.getString("style.message-format", "%prefix% &7%sender% &8> &f%message%");
        formattedMessage = colorize(template).replace("%prefix%", pluginPrefix);

        noPermissionMessage = colorize(
                config.getString("messages.no-permission", "&cYou do not have permission to do that."));
        reloadedMessage = colorize(config.getString("messages.reloaded", "&aStaffChat configuration reloaded."));
        helpHeader = colorize(config.getString("messages.help-header", "&6StaffChat"));
        helpReloadLine = colorize(config.getString("messages.help-reload", "&7/sc reload &8- &fReload configuration"));

        discordEnabled = config.getBoolean("discord.enabled", false);
        discordProvider = config.getString("discord.provider", "AUTO");
        discordChannelKey = config.getString("discord.channel-key", "");
        discordChannelId = config.getString("discord.channel-id", "");
        discordChannelName = config.getString("discord.channel-name", "");

        ConfigurationSection outbound = config.getConfigurationSection("discord.outbound");
        if (outbound != null) {
            discordOutboundEnabled = outbound.getBoolean("enabled", true);
        } else {
            discordOutboundEnabled = config.getBoolean("discord.send-enabled", true);
        }

        ConfigurationSection inbound = config.getConfigurationSection("discord.inbound");
        if (inbound != null) {
            discordInboundEnabled = inbound.getBoolean("enabled", true);
            discordInboundAddPrefix = inbound.getBoolean("add-prefix", true);
        } else {
            discordInboundEnabled = config.getBoolean("discord.receive-enabled", true);
            discordInboundAddPrefix = config.getBoolean("discord.receive-add-prefix", true);
        }
    }

    public String getChatPrefix() {
        return chatPrefix;
    }

    public boolean isPublicSendingAllowed() {
        return allowPublicSending;
    }

    public String formatStaffMessage(String senderName, String content) {
        return formattedMessage
                .replace("%sender%", senderName)
                .replace("%message%", content);
    }

    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public String getReloadedMessage() {
        return reloadedMessage;
    }

    public String getHelpHeader() {
        return helpHeader;
    }

    public String getHelpReloadLine() {
        return helpReloadLine;
    }

    public boolean isDiscordEnabled() {
        return discordEnabled;
    }

    public String getDiscordProvider() {
        return discordProvider == null ? "AUTO" : discordProvider;
    }

    public String getDiscordChannelId() {
        return discordChannelId == null ? "" : discordChannelId;
    }

    public String getDiscordChannelKey() {
        return discordChannelKey == null ? "" : discordChannelKey;
    }

    public String getDiscordChannelName() {
        return discordChannelName == null ? "" : discordChannelName;
    }

    public boolean isDiscordOutboundEnabled() {
        return discordOutboundEnabled;
    }

    public boolean isDiscordInboundEnabled() {
        return discordInboundEnabled;
    }

    public boolean isDiscordInboundAddPrefix() {
        return discordInboundAddPrefix;
    }

    public String getPluginPrefix() {
        return pluginPrefix == null ? "" : pluginPrefix;
    }

    private static String colorize(String text) {
        String input = text == null ? "" : text;
        input = replaceHex(input, ANGLE_HEX_PATTERN);
        input = replaceHex(input, AMP_HEX_PATTERN);
        input = replaceHex(input, BARE_HEX_PATTERN);
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private static String replaceHex(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer output = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(toSectionHex(matcher.group(1))));
        }

        matcher.appendTail(output);
        return output.toString();
    }

    private static String toSectionHex(String hex) {
        StringBuilder builder = new StringBuilder("\u00A7x");
        for (char c : hex.toCharArray()) {
            builder.append('\u00A7').append(c);
        }
        return builder.toString();
    }
}
