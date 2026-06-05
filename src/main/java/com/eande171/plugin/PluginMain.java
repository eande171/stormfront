package com.eande171.plugin;

import com.eande171.plugin.constants.Permissions;
import com.eande171.plugin.services.ConfigService;
import com.eande171.plugin.services.MessageService;
import com.eande171.plugin.services.PlayerDataService;
import com.eande171.plugin.services.PluginService;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginMain extends JavaPlugin {

    @Getter private ConfigService configService;
    @Getter private PluginService pluginService;
    @Getter private PlayerDataService playerDataService;
    @Getter private MessageService messageService;

    @Override
    public void onEnable() {
        // Plugin startup logic
        configService = new ConfigService(this);
        configService.load();

        messageService = new MessageService(this);
        messageService.load();

        playerDataService = new PlayerDataService();

        pluginService = new PluginService(this, configService, playerDataService, messageService);
        pluginService.start();

        registerListeners();
        registerCommands();

        getLogger().info("Plugin Enabled! Version: " + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Plugin Disabled! Version: " + getPluginMeta().getVersion());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PluginListener(playerDataService), this);
    }

    private void registerCommands() {
        PluginCommand cmd = new PluginCommand(configService, pluginService, messageService);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                Commands.literal("template")
                    .requires(ctx -> ctx.getSender().hasPermission(Permissions.ADMIN))
                    .then(Commands.literal("on").executes(cmd::onOn))
                    .then(Commands.literal("off").executes(cmd::onOff))
                    .build()
            );
        });
    }
}
