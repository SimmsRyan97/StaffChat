package com.whiteiverson.staffchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class StaffChatCommand implements CommandExecutor {
    private static final String COMMAND_PERMISSION = "staffchat.command";

    private final StaffChatPlugin plugin;
    private final StaffChatSettings settings;

    public StaffChatCommand(StaffChatPlugin plugin, StaffChatSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
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
            sender.sendMessage(settings.getReloadedMessage());
            return true;
        }

        sender.sendMessage(settings.getHelpHeader());
        sender.sendMessage(settings.getHelpReloadLine());
        return true;
    }
}
