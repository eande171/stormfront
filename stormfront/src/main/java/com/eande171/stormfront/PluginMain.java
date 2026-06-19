package com.eande171.stormfront;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.api.WeatherType;
import com.eande171.stormfront.weather.BlizzardType;
import com.eande171.stormfront.weather.HeatwaveType;
import com.eande171.stormfront.weather.MiasmaType;
import com.eande171.stormfront.weather.RainfrontType;
import com.eande171.stormfront.weather.ThunderstormType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginMain extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerWeatherTypes();

        getLogger().info("Stormfront enabled. Version: " + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("Stormfront disabled. Version: " + getPluginMeta().getVersion());
    }

    private void registerWeatherTypes() {
        FileConfiguration config = getConfig();
        maybeRegister(config, "types.rainfront",    new RainfrontType());
        maybeRegister(config, "types.thunderstorm", new ThunderstormType());
        maybeRegister(config, "types.miasma",        new MiasmaType());
        maybeRegister(config, "types.blizzard",     new BlizzardType());
        maybeRegister(config, "types.heatwave",     new HeatwaveType());
    }

    private void maybeRegister(FileConfiguration config, String key, WeatherType type) {
        if (config.getBoolean(key, true)) {
            StormfrontAPI.get().getRegistry().register(type);
        }
    }
}
