package com.eande171.stormfront;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.constants.Permissions;
import com.eande171.stormfront.registry.StormfrontImpl;
import com.eande171.stormfront.services.ConfigService;
import com.eande171.stormfront.services.MessageService;
import com.eande171.stormfront.services.PlayerDataService;
import com.eande171.stormfront.weather.DenseFogType;
import com.eande171.stormfront.weather.RainfrontType;
import com.eande171.stormfront.weather.ThunderstormType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginMain extends JavaPlugin {

    @Getter private ConfigService configService;
    @Getter private MessageService messageService;
    @Getter private PlayerDataService playerDataService;
    @Getter private CellManager cellManager;
    @Getter private WeatherPacketService weatherPacketService;
    @Getter private WeatherScheduler weatherScheduler;

    @Override
    public void onEnable() {
        StormfrontAPI.setInstance(new StormfrontImpl());

        configService = new ConfigService(this);
        configService.load();

        messageService = new MessageService(this);
        messageService.load();

        playerDataService = new PlayerDataService();
        cellManager = new CellManager(this);
        weatherPacketService = new WeatherPacketService();
        weatherScheduler = new WeatherScheduler(this, cellManager, playerDataService, weatherPacketService);
        weatherScheduler.start(configService.getSchedulerIntervalTicks());

        registerWeatherTypes();
        registerListeners();
        registerCommands();
        suppressVanillaWeather();

        getLogger().info("Stormfront enabled! Version: " + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("Stormfront disabled! Version: " + getPluginMeta().getVersion());
    }

    private void registerWeatherTypes() {
        StormfrontAPI.get().getRegistry().register(new RainfrontType());
        StormfrontAPI.get().getRegistry().register(new ThunderstormType());
        StormfrontAPI.get().getRegistry().register(new DenseFogType());
    }

    private void registerCommands() {
        StormfrontCommand cmd = new StormfrontCommand(cellManager);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                Commands.literal("stormfront")
                    .requires(ctx -> ctx.getSender().hasPermission(Permissions.ADMIN))
                    .then(Commands.literal("test")
                        .then(Commands.literal("rain").executes(cmd::onTestRain))
                        .then(Commands.literal("thunder").executes(cmd::onTestThunder))
                        .then(Commands.literal("fog").executes(cmd::onTestFog)))
                    .then(Commands.literal("stop").executes(cmd::onStop))
                    .build()
            );
        });
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
            new PluginListener(playerDataService, weatherPacketService, cellManager), this);
    }

    private void suppressVanillaWeather() {
        for (World world : getServer().getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
        }

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (World world : getServer().getWorlds()) {
                world.setStorm(false);
                world.setThundering(false);
            }
        }, 100L, 100L);
    }
}
