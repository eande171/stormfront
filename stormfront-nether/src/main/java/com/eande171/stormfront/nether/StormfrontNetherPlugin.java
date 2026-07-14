package com.eande171.stormfront.nether;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.nether.weather.SoulStormType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StormfrontNetherPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerWeatherTypes();

        getLogger().info("Stormfront Nether enabled. Version: " + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("Stormfront Nether disabled. Version: " + getPluginMeta().getVersion());
    }

    private void registerWeatherTypes() {
        FileConfiguration config = getConfig();
        StormfrontAPI.registerIfEnabled(config, "types.soulstorm", new SoulStormType());
    }
}
