package com.whiteiverson.staffchat;

import org.bukkit.plugin.java.JavaPlugin;

public final class StaffChatPlugin extends JavaPlugin {
    private StaffChatSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        settings = new StaffChatSettings(this);
        settings.reload();

        getServer().getPluginManager().registerEvents(new StaffChatListener(settings), this);

        if (getCommand("staffchat") != null) {
            getCommand("staffchat").setExecutor(new StaffChatCommand(this, settings));
        } else {
            getLogger().warning("Command 'staffchat' is not defined in plugin.yml");
        }

        getLogger().info("StaffChat enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("StaffChat disabled.");
    }
}
