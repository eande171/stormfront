package com.eande171.stormfront.api;

import org.bukkit.configuration.file.FileConfiguration;

public abstract class StormfrontAPI {

    private static volatile StormfrontAPI instance;

    public static StormfrontAPI get() {
        if (instance == null) {
            throw new IllegalStateException("StormfrontAPI has not been initialized.");
        }
        return instance;
    }

    public static void setInstance(StormfrontAPI api) {
        if (instance != null) {
            throw new IllegalStateException("StormfrontAPI is already initialized.");
        }
        instance = api;
    }

    // Allows re-initialization on plugin disable/re-enable rather than permanently locking the singleton
    public static void clearInstance() {
        instance = null;
    }

    public abstract WeatherRegistry getRegistry();

    public abstract CellManager getCellManager();

    // Registers a type only if its config.yml flag is true (default true); shared by core and addon plugins
    public static void registerIfEnabled(FileConfiguration config, String key, WeatherType type) {
        if (config.getBoolean(key, true)) {
            get().getRegistry().register(type);
        }
    }
}
