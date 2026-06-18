package com.eande171.stormfront.api;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Set;

public interface WeatherType {

    String getId();

    void onTick(WeatherCell cell, Player player);

    default void onEntityTick(WeatherCell cell, LivingEntity entity) {}

    // Called when a player first enters this cell's radius
    default void onPlayerEnter(Player player) {}

    // Called when a player leaves this cell's radius, or when the cell expires
    default void onPlayerExit(Player player) {}

    // Multiplier applied to the sky rain level packet (0 = clear sky, 1 = full rain sky)
    default float getRainMultiplier() { return 1.0f; }

    // Multiplier applied to the sky thunder level packet (0 = no effect, 1 = full storm sky)
    default float getThunderMultiplier() { return 0.25f; }

    void onStart(WeatherCell cell);

    void onEnd(WeatherCell cell);

    Set<String> getCompatibleBiomes();

    int getPriority();

    // Relative chance this type is selected during natural generation (0.0 = never, 1.0 = full weight)
    default float getNaturalSpawnWeight() { return 1.0f; }

    // Additional spawn condition checked before weighted selection — return false to block natural spawning
    default boolean canNaturallySpawn(Player target) { return true; }
}
