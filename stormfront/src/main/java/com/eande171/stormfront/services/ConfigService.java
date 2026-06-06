package com.eande171.stormfront.services;

import com.eande171.stormfront.PluginMain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigService {

    private final PluginMain plugin;

    @Getter private int maxParticlesPerPlayer;
    @Getter private int schedulerIntervalTicks;

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        maxParticlesPerPlayer = plugin.getConfig().getInt("max-particles-per-player");
        schedulerIntervalTicks = plugin.getConfig().getInt("scheduler-interval-ticks");

        plugin.getLogger().info("Config loaded.");
    }
}
