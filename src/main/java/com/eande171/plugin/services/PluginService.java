package com.eande171.plugin.services;

import com.eande171.plugin.PluginMain;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PluginService {

    private final PluginMain plugin;
    private final ConfigService configService;
    private final PlayerDataService playerDataService;
    private final MessageService messageService;

    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void enable() {
        configService.setActive(true);

        // Start Logic
    }

    public void disable() {
        configService.setActive(false);

        // Stop Logic
    }

    private void tick() {
        if (!configService.isActive()) { return; }

        // Tick Logic
    }
}
