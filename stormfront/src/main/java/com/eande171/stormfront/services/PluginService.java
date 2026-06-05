package com.eande171.stormfront.services;

import com.eande171.stormfront.PluginMain;
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
    }

    public void disable() {
        configService.setActive(false);
    }

    private void tick() {
        if (!configService.isActive()) { return; }
    }
}
