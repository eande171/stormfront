package com.eande171.stormfront;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.constants.Permissions;
import com.eande171.stormfront.registry.StormfrontImpl;
import com.eande171.stormfront.services.ConfigService;
import com.eande171.stormfront.services.PlayerDataService;
import com.eande171.stormfront.weather.BlizzardType;
import com.eande171.stormfront.weather.DenseFogType;
import com.eande171.stormfront.weather.HeatwaveType;
import com.eande171.stormfront.weather.RainfrontType;
import com.eande171.stormfront.weather.ThunderstormType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginMain extends JavaPlugin {

    @Getter private ConfigService configService;
    @Getter private PlayerDataService playerDataService;
    @Getter private CellManager cellManager;
    @Getter private WeatherPacketService weatherPacketService;
    @Getter private WeatherScheduler weatherScheduler;
    @Getter private WeatherGenerator weatherGenerator;

    @Override
    public void onEnable() {
        StormfrontAPI.setInstance(new StormfrontImpl());

        configService = new ConfigService(this);
        configService.load();

        playerDataService = new PlayerDataService();
        cellManager = new CellManager(this, playerDataService);
        weatherPacketService = new WeatherPacketService();
        weatherScheduler = new WeatherScheduler(this, cellManager, playerDataService, weatherPacketService);
        weatherScheduler.start(configService.getSchedulerIntervalTicks());

        weatherGenerator = new WeatherGenerator(this, cellManager, configService);
        weatherGenerator.start();

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
        StormfrontAPI.get().getRegistry().register(new BlizzardType());
        StormfrontAPI.get().getRegistry().register(new HeatwaveType());
    }

    private void registerCommands() {
        StormfrontCommand cmd = new StormfrontCommand(cellManager);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                Commands.literal("stormfront")
                    .requires(ctx -> ctx.getSender().hasPermission(Permissions.ADMIN))
                    .then(Commands.literal("spawn")
                        .then(Commands.argument("type", ArgumentTypes.namespacedKey())
                            .suggests((ctx, builder) -> {
                                StormfrontAPI.get().getRegistry().getAll()
                                    .forEach(t -> builder.suggest(t.getId()));
                                return builder.buildFuture();
                            })
                            .executes(cmd::onSpawn)
                            .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1000))
                                .executes(cmd::onSpawn)
                                .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.1f, 1.0f))
                                    .executes(cmd::onSpawn)
                                    .then(Commands.argument("duration", IntegerArgumentType.integer(-1))
                                        .executes(cmd::onSpawn))))))
                    .then(Commands.literal("stop").executes(cmd::onStop))
                    .then(Commands.literal("list").executes(cmd::onList))
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
