package com.eande171.stormfront.services;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigService {

    private final JavaPlugin plugin;

    @Getter private int schedulerIntervalTicks;
    @Getter private boolean suppressVanillaWeather;

    @Getter private boolean naturalGenEnabled;
    @Getter private int naturalGenCheckIntervalSeconds;
    @Getter private double naturalGenBaseSpawnChance;
    @Getter private int naturalGenMaxActiveCells;
    @Getter private double naturalGenSpawnDistanceMin;
    @Getter private double naturalGenSpawnDistanceMax;
    @Getter private int naturalGenRadiusMin;
    @Getter private int naturalGenRadiusMax;
    @Getter private float naturalGenIntensityMin;
    @Getter private float naturalGenIntensityMax;
    @Getter private int naturalGenDurationMinSeconds;
    @Getter private int naturalGenDurationMaxSeconds;
    @Getter private double naturalGenMovementSpeedMin;
    @Getter private double naturalGenMovementSpeedMax;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        schedulerIntervalTicks  = plugin.getConfig().getInt("scheduler-interval-ticks");
        suppressVanillaWeather  = plugin.getConfig().getBoolean("suppress-vanilla-weather");

        naturalGenEnabled               = plugin.getConfig().getBoolean("natural-generation.enabled");
        naturalGenCheckIntervalSeconds  = plugin.getConfig().getInt("natural-generation.check-interval-seconds");
        naturalGenBaseSpawnChance       = plugin.getConfig().getDouble("natural-generation.base-spawn-chance");
        naturalGenMaxActiveCells        = plugin.getConfig().getInt("natural-generation.max-active-cells");
        naturalGenSpawnDistanceMin      = plugin.getConfig().getDouble("natural-generation.spawn-distance-min");
        naturalGenSpawnDistanceMax      = plugin.getConfig().getDouble("natural-generation.spawn-distance-max");
        naturalGenRadiusMin             = plugin.getConfig().getInt("natural-generation.radius-min");
        naturalGenRadiusMax             = plugin.getConfig().getInt("natural-generation.radius-max");
        naturalGenIntensityMin          = (float) plugin.getConfig().getDouble("natural-generation.intensity-min");
        naturalGenIntensityMax          = (float) plugin.getConfig().getDouble("natural-generation.intensity-max");
        naturalGenDurationMinSeconds    = plugin.getConfig().getInt("natural-generation.duration-min-seconds");
        naturalGenDurationMaxSeconds    = plugin.getConfig().getInt("natural-generation.duration-max-seconds");
        naturalGenMovementSpeedMin      = plugin.getConfig().getDouble("natural-generation.movement-speed-min");
        naturalGenMovementSpeedMax      = plugin.getConfig().getDouble("natural-generation.movement-speed-max");

        plugin.getLogger().info("Config loaded.");
    }
}
