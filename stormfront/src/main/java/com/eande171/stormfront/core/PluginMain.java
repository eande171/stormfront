package com.eande171.stormfront.core;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.core.weather.BlizzardType;
import com.eande171.stormfront.core.weather.HeatwaveType;
import com.eande171.stormfront.core.weather.MiasmaType;
import com.eande171.stormfront.core.weather.RainfrontType;
import com.eande171.stormfront.core.weather.ThunderstormType;
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
        StormfrontAPI.registerIfEnabled(config, "types.rainfront",    new RainfrontType());
        StormfrontAPI.registerIfEnabled(config, "types.thunderstorm", new ThunderstormType());
        StormfrontAPI.registerIfEnabled(config, "types.miasma",       new MiasmaType());
        StormfrontAPI.registerIfEnabled(config, "types.blizzard",     new BlizzardType());
        StormfrontAPI.registerIfEnabled(config, "types.heatwave",     new HeatwaveType());
    }
}
