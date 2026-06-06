package com.eande171.stormfront.api;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Set;

public interface WeatherType {

    String getId();

    void onTick(WeatherCell cell, Player player);

    default void onEntityTick(WeatherCell cell, LivingEntity entity) {}

    void onStart(WeatherCell cell);

    void onEnd(WeatherCell cell);

    Set<String> getCompatibleBiomes();

    int getPriority();
}
