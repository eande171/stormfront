package com.eande171.stormfront.api;

import org.bukkit.plugin.java.JavaPlugin;

public final class StormfrontApiPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Stormfront API loaded. Version: " + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("Stormfront API unloaded.");
    }
}
