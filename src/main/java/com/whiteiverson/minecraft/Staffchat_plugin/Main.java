package com.whiteiverson.minecraft.Staffchat_plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register event listener for Chat
        getServer().getPluginManager().registerEvents(new Chat(), this);

        // Register command executor if the command is present in plugin.yml
        if (getCommand("staffchat") != null) {
            getCommand("staffchat").setExecutor(new StaffCommands());
        } else {
            getLogger().warning("Command 'staffchat' is not defined in plugin.yml!");
        }

        // Log the plugin start message
        getLogger().info("StaffChat plugin has started!");

        // Load the default config if it does not exist
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Log the plugin stop message
        getLogger().info("StaffChat plugin has stopped!");
    }
}