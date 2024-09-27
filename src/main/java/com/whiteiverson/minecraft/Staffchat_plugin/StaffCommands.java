package com.whiteiverson.minecraft.Staffchat_plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class StaffCommands implements CommandExecutor {
    private final Plugin plugin;

    public StaffCommands() {
        this.plugin = Main.getPlugin(Main.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command label matches
        if (!label.equalsIgnoreCase("staffchat") && !label.equalsIgnoreCase("sc")) {
            return false;
        }

        // Help message for the staff chat command
        final String helpMessage = ChatColor.GREEN + "&6Staff Chat\n&3>&6 /" + label + " reload";

        // No arguments provided
        if (args.length == 0) {
            sender.sendMessage(helpMessage);
            return true; // Return true to indicate the command was handled
        }

        // Check permission for other commands
        if (!sender.hasPermission("staffchat.command")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that!");
            return true;
        }

        // Handle the reload command
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Staff Chat was reloaded!");
            return true;
        }

        // If the command isn't recognised, show the help message
        sender.sendMessage(helpMessage);
        return true; // Return true to indicate the command was handled
    }
}