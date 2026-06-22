package com.whiteiverson.staffchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class StaffChatCommand implements CommandExecutor {
    private static final String COMMAND_PERMISSION = "staffchat.command";

    private final StaffChatPlugin plugin;
    private final StaffChatSettings settings;
    private final DiscordRelayService discordRelayService;

    public StaffChatCommand(StaffChatPlugin plugin, StaffChatSettings settings,
            DiscordRelayService discordRelayService) {
        this.plugin = plugin;
        this.settings = settings;
        this.discordRelayService = discordRelayService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(settings.getHelpHeader());
            sender.sendMessage(settings.getHelpReloadLine());
            return true;
        }

        if (!sender.hasPermission(COMMAND_PERMISSION)) {
            sender.sendMessage(settings.getNoPermissionMessage());
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            settings.reload();
            discordRelayService.unregisterInboundRelay();
            discordRelayService.registerInboundRelay();
            discordRelayService.refreshNow();
            sender.sendMessage(settings.getReloadedMessage());
            return true;
        }

        sender.sendMessage(settings.getHelpHeader());
        sender.sendMessage(settings.getHelpReloadLine());
        return true;
    }
}
