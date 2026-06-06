package com.eande171.stormfront;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.registry.StormfrontImpl;
import com.eande171.stormfront.services.ConfigService;
import com.eande171.stormfront.services.MessageService;
import com.eande171.stormfront.services.PlayerDataService;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginMain extends JavaPlugin {

    @Getter private ConfigService configService;
    @Getter private MessageService messageService;
    @Getter private PlayerDataService playerDataService;

    @Override
    public void onEnable() {
        StormfrontAPI.setInstance(new StormfrontImpl());

        configService = new ConfigService(this);
        configService.load();

        messageService = new MessageService(this);
        messageService.load();

        playerDataService = new PlayerDataService();

        registerListeners();
        suppressVanillaWeather();

        getLogger().info("Stormfront enabled! Version: " + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("Stormfront disabled! Version: " + getPluginMeta().getVersion());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PluginListener(playerDataService), this);
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
