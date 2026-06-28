package com.eande171.stormfront.api;

import com.eande171.stormfront.CellManager;
import com.eande171.stormfront.EngineListener;
import com.eande171.stormfront.StormfrontCommand;
import com.eande171.stormfront.WeatherGenerator;
import com.eande171.stormfront.WeatherPacketService;
import com.eande171.stormfront.WeatherScheduler;
import com.eande171.stormfront.constants.Permissions;
import com.eande171.stormfront.registry.StormfrontImpl;
import com.eande171.stormfront.services.ConfigService;
import com.eande171.stormfront.services.PlayerDataService;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class StormfrontApiPlugin extends JavaPlugin {

    private WeatherPacketService weatherPacketService;

    @Override
    public void onEnable() {
        ConfigService configService = new ConfigService(this);
        configService.load();

        PlayerDataService playerDataService = new PlayerDataService();
        CellManager cellManager = new CellManager(playerDataService);

        StormfrontAPI.setInstance(new StormfrontImpl(cellManager));

        weatherPacketService = new WeatherPacketService();
        WeatherScheduler weatherScheduler = new WeatherScheduler(this, cellManager, playerDataService, weatherPacketService);
        weatherScheduler.start(configService.getSchedulerIntervalTicks());

        WeatherGenerator weatherGenerator = new WeatherGenerator(this, cellManager, configService);
        weatherGenerator.start();

        getServer().getPluginManager().registerEvents(
            new EngineListener(playerDataService, weatherPacketService, cellManager), this);

        if (configService.isSuppressVanillaWeather()) {
            suppressVanillaWeather();
        }

        registerCommands();

        getLogger().info("Stormfront API enabled. Version: " + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        if (weatherPacketService != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                weatherPacketService.reset(player);
            }
        }
        getLogger().info("Stormfront API disabled. Version: " + getPluginMeta().getVersion());
    }

    private void registerCommands() {
        StormfrontCommand cmd = new StormfrontCommand();

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
