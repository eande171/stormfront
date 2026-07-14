package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import org.bukkit.entity.Player;

public class RainfrontType extends AbstractRainType {

    @Override
    public String getId() { return "stormfront:rain"; }

    @Override
    public int getPriority() { return 1; }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        // Particle intensity uses a squared curve - splashes only become heavy near centre
        float distanceFactor = WeatherUtils.distanceFactor(cell, player.getLocation());
        float particleIntensity = cell.getIntensity() * distanceFactor * distanceFactor;

        spawnRainImpacts(player, particleIntensity);
        WeatherUtils.applyMovementPenalty(player, particleIntensity, 0.1f, 0.185f, 0.015f);
        WeatherUtils.extinguishNearbyFires(player, particleIntensity);
    }
}
