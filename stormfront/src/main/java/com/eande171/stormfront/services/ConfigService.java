package com.eande171.stormfront.services;

import com.eande171.stormfront.PluginMain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigService {

    private final PluginMain plugin;

    @Getter private boolean active;

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        active = plugin.getConfig().getBoolean("active");

        plugin.getLogger().info("Loaded active: " + active);
    }

    public void save() {
        plugin.getConfig().set("active", active);
        plugin.saveConfig();
    }

    public void setActive(boolean active) { this.active = active; save(); }
}
