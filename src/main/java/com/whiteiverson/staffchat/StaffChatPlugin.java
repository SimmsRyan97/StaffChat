package com.whiteiverson.staffchat;

import org.bukkit.plugin.java.JavaPlugin;

public final class StaffChatPlugin extends JavaPlugin {
    private StaffChatSettings settings;
    private DiscordRelayService discordRelayService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        settings = new StaffChatSettings(this);
        settings.reload();
        discordRelayService = new DiscordRelayService(this, settings);
        discordRelayService.registerInboundRelay();
        discordRelayService.startAutoRecovery();

        getServer().getPluginManager().registerEvents(new StaffChatListener(settings, discordRelayService), this);

        if (getCommand("staffchat") != null) {
            getCommand("staffchat").setExecutor(new StaffChatCommand(this, settings, discordRelayService));
        } else {
            getLogger().warning("Command 'staffchat' is not defined in plugin.yml");
        }

        getLogger().info("StaffChat enabled.");
    }

    @Override
    public void onDisable() {
        if (discordRelayService != null) {
            discordRelayService.stopAutoRecovery();
            discordRelayService.unregisterInboundRelay();
        }
        getLogger().info("StaffChat disabled.");
    }
}
