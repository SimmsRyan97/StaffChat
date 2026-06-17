package com.whiteiverson.staffchat;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public final class DiscordRelayService {
    private static final String RECEIVE_PERMISSION = "staffchat.receive";

    private final StaffChatPlugin plugin;
    private final StaffChatSettings settings;

    private boolean warnedDiscordSrvUnavailable;
    private boolean warnedEssentialsUnavailable;
    private boolean warnedDiscordSrvListenerFailed;

    private Object discordSrvInboundListener;
    private Listener essentialsInboundListener;

    public DiscordRelayService(StaffChatPlugin plugin, StaffChatSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void relayStaffMessage(Player sender, String content) {
        if (sender == null || content == null || content.isBlank()) {
            return;
        }

        if (!settings.isDiscordEnabled() || !settings.isDiscordOutboundEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String provider = settings.getDiscordProvider().toUpperCase(Locale.ROOT);
            switch (provider) {
                case "DISCORDSRV":
                    sendViaDiscordSrv(sender, content);
                    break;
                case "ESSENTIALS":
                    sendViaEssentials(sender, content);
                    break;
                default:
                    if (isPluginEnabled("DiscordSRV")) {
                        sendViaDiscordSrv(sender, content);
                    } else {
                        sendViaEssentials(sender, content);
                    }
                    break;
            }
        });
    }

    private void sendViaDiscordSrv(Player sender, String content) {
        if (!isPluginEnabled("DiscordSRV")) {
            if (!warnedDiscordSrvUnavailable) {
                warnedDiscordSrvUnavailable = true;
                plugin.getLogger().warning("StaffChat: discord.provider is DISCORDSRV but DiscordSRV is not loaded.");
            }
            return;
        }

        String gameChannel = settings.getDiscordChannelName();
        if (gameChannel.isBlank()) {
            gameChannel = "global";
        }

        try {
            // Uses DiscordSRV's own Minecraft->Discord formatting pipeline.
            DiscordSRV.getPlugin().processChatMessage(sender, content, gameChannel, false, null);
        } catch (Exception exception) {
            plugin.getLogger().warning("StaffChat: DiscordSRV outbound relay failed: " + exception.getMessage());
        }
    }

    private void sendViaEssentials(Player sender, String content) {
        if (!isPluginEnabled("EssentialsDiscord") && !isPluginEnabled("EssentialsXDiscord")) {
            if (!warnedEssentialsUnavailable) {
                warnedEssentialsUnavailable = true;
                plugin.getLogger()
                        .warning("StaffChat: discord.provider is ESSENTIALS but EssentialsDiscord is not loaded.");
            }
            return;
        }

        try {
            Class<?> discordServiceClass = Class.forName("net.essentialsx.api.v2.services.discord.DiscordService");
            @SuppressWarnings("unchecked")
            Class<Object> serviceType = (Class<Object>) discordServiceClass;
            Object service = Bukkit.getServicesManager().load(serviceType);
            if (service == null) {
                plugin.getLogger()
                        .warning("StaffChat: Essentials DiscordService is unavailable. Is EssentialsDiscord enabled?");
                return;
            }

            // Uses EssentialsDiscord predefined player chat formatting.
            Method sendChatMessage = discordServiceClass.getMethod("sendChatMessage", Player.class, String.class);
            sendChatMessage.invoke(service, sender, content);
        } catch (Exception exception) {
            plugin.getLogger().warning("StaffChat: Essentials outbound relay failed: " + exception.getMessage());
        }
    }

    public void registerInboundRelay() {
        unregisterInboundRelay();

        if (!settings.isDiscordEnabled() || !settings.isDiscordInboundEnabled()) {
            return;
        }

        String provider = settings.getDiscordProvider().toUpperCase(Locale.ROOT);

        if (provider.equals("AUTO") || provider.equals("DISCORDSRV")) {
            if (isPluginEnabled("DiscordSRV")) {
                registerDiscordSrvInbound();
                return;
            }
            if (provider.equals("DISCORDSRV") && !warnedDiscordSrvUnavailable) {
                warnedDiscordSrvUnavailable = true;
                plugin.getLogger().warning("StaffChat: Discord inbound relay requested, but DiscordSRV is not loaded.");
            }
        }

        if (provider.equals("AUTO") || provider.equals("ESSENTIALS")) {
            registerEssentialsInbound();
        }
    }

    private void registerDiscordSrvInbound() {
        if (discordSrvInboundListener != null) {
            return;
        }

        try {
            discordSrvInboundListener = new Object() {
                @Subscribe
                public void onDiscordGuildMessagePostProcess(DiscordGuildMessagePostProcessEvent event) {
                    if (event == null || event.getChannel() == null) {
                        return;
                    }

                    if (!isConfiguredDiscordChannel(event.getChannel().getId(), event.getChannel().getName())) {
                        return;
                    }

                    event.setCancelled(true);

                    String message = event.getProcessedMessage();
                    if (message == null || message.isBlank()) {
                        return;
                    }

                    String outbound = message;
                    if (settings.isDiscordInboundAddPrefix()) {
                        String prefix = settings.getPluginPrefix();
                        if (!prefix.isBlank()) {
                            outbound = prefix + " " + message;
                        }
                    }

                    String finalOutbound = outbound;
                    Bukkit.getScheduler().runTask(plugin, () -> sendInboundMessageToStaff(finalOutbound));
                }
            };

            DiscordSRV.api.subscribe(discordSrvInboundListener);
            plugin.getLogger().info("StaffChat: DiscordSRV inbound relay enabled using provider formatting.");
        } catch (Exception exception) {
            discordSrvInboundListener = null;
            if (!warnedDiscordSrvListenerFailed) {
                warnedDiscordSrvListenerFailed = true;
                plugin.getLogger()
                        .warning("StaffChat: Failed to attach DiscordSRV inbound listener: " + exception.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerEssentialsInbound() {
        if (essentialsInboundListener != null) {
            return;
        }

        try {
            if (!isPluginEnabled("EssentialsDiscord") && !isPluginEnabled("EssentialsXDiscord")) {
                if (!warnedEssentialsUnavailable) {
                    warnedEssentialsUnavailable = true;
                    plugin.getLogger().warning(
                            "StaffChat: Discord inbound relay requested, but EssentialsDiscord is not loaded.");
                }
                return;
            }

            Class<?> relayEventClass = Class.forName("net.essentialsx.api.v2.events.discord.DiscordRelayEvent");
            if (!Event.class.isAssignableFrom(relayEventClass)) {
                plugin.getLogger().warning("StaffChat: Essentials DiscordRelayEvent type is not a Bukkit Event.");
                return;
            }

            essentialsInboundListener = new Listener() {
            };

            Bukkit.getPluginManager().registerEvent(
                    (Class<? extends Event>) relayEventClass,
                    essentialsInboundListener,
                    EventPriority.HIGHEST,
                    (listener, event) -> handleEssentialsInboundRelayEvent(event),
                    plugin,
                    true);

            plugin.getLogger().info("StaffChat: Essentials inbound relay enabled using provider formatting.");
        } catch (Exception exception) {
            essentialsInboundListener = null;
            plugin.getLogger()
                    .warning("StaffChat: Failed to attach Essentials inbound listener: " + exception.getMessage());
        }
    }

    public void unregisterInboundRelay() {
        if (discordSrvInboundListener != null) {
            try {
                DiscordSRV.api.unsubscribe(discordSrvInboundListener);
            } catch (Exception ignored) {
                // Safe best-effort cleanup
            } finally {
                discordSrvInboundListener = null;
            }
        }

        if (essentialsInboundListener != null) {
            HandlerList.unregisterAll(essentialsInboundListener);
            essentialsInboundListener = null;
        }
    }

    private void handleEssentialsInboundRelayEvent(Event event) {
        if (event == null || !settings.isDiscordEnabled() || !settings.isDiscordInboundEnabled()) {
            return;
        }

        if (!isEssentialsInboundModeEnabled()) {
            return;
        }

        try {
            Object channel = event.getClass().getMethod("getChannel").invoke(event);
            if (channel == null) {
                return;
            }

            String channelId = invokeStringNoThrow(channel, "getId");
            String channelName = invokeStringNoThrow(channel, "getName");
            if (!isConfiguredDiscordChannel(channelId, channelName)) {
                return;
            }

            String formattedMessage = invokeStringNoThrow(event, "getFormattedMessage");
            if (formattedMessage.isBlank()) {
                return;
            }

            if (settings.isDiscordInboundAddPrefix()) {
                String prefix = settings.getPluginPrefix();
                if (!prefix.isBlank()) {
                    formattedMessage = prefix + " " + formattedMessage;
                }
            }

            Method setFormatted = event.getClass().getMethod("setFormattedMessage", String.class);
            setFormatted.invoke(event, formattedMessage);

            Object viewersObj = event.getClass().getMethod("getViewers").invoke(event);
            if (viewersObj instanceof List) {
                @SuppressWarnings("rawtypes")
                List viewers = (List) viewersObj;
                viewers.removeIf(viewer -> !isEssentialsViewerAllowed(viewer));
            }
        } catch (Exception exception) {
            plugin.getLogger()
                    .warning("StaffChat: Failed to process Essentials inbound relay event: " + exception.getMessage());
        }
    }

    private boolean isEssentialsViewerAllowed(Object viewer) {
        if (viewer == null) {
            return false;
        }

        try {
            Method isAuthorized = viewer.getClass().getMethod("isAuthorized", String.class);
            Object value = isAuthorized.invoke(viewer, RECEIVE_PERMISSION);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (Exception ignored) {
            // Fall through to player permission check
        }

        try {
            Method getBase = viewer.getClass().getMethod("getBase");
            Object base = getBase.invoke(viewer);
            if (base instanceof Player) {
                return ((Player) base).hasPermission(RECEIVE_PERMISSION);
            }
        } catch (Exception ignored) {
            // No supported viewer type
        }

        return false;
    }

    private String invokeStringNoThrow(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof String ? (String) value : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isEssentialsInboundModeEnabled() {
        String provider = settings.getDiscordProvider().toUpperCase(Locale.ROOT);
        if (provider.equals("ESSENTIALS")) {
            return true;
        }
        return provider.equals("AUTO") && !isPluginEnabled("DiscordSRV");
    }

    private boolean isConfiguredDiscordChannel(String incomingId, String incomingName) {
        String configuredId = settings.getDiscordChannelId();
        if (!configuredId.isBlank()) {
            return configuredId.equals(incomingId);
        }

        String configuredName = settings.getDiscordChannelName();
        if (!configuredName.isBlank()) {
            return configuredName.equalsIgnoreCase(incomingName);
        }

        return false;
    }

    private void sendInboundMessageToStaff(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(RECEIVE_PERMISSION)) {
                onlinePlayer.sendMessage(message);
            }
        }
    }

    private boolean isPluginEnabled(String pluginName) {
        Plugin loadedPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return loadedPlugin != null && loadedPlugin.isEnabled();
    }
}
