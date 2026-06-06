package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;

public class RainfrontType extends AbstractRainType {

    @Override
    public String getId() { return "stormfront:rain"; }

    @Override
    public int getPriority() { return 1; }

    @Override
    public Set<String> getCompatibleBiomes() { return Collections.emptySet(); }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        // Particle intensity uses a squared curve - splashes only become heavy near centre
        float distanceFactor = distanceFactor(cell, player.getLocation());
        float particleIntensity = cell.getIntensity() * distanceFactor * distanceFactor;

        spawnRainImpacts(player, particleIntensity);
        applyMovementPenalty(player, particleIntensity);
        WeatherUtils.extinguishNearbyFires(player, particleIntensity);
    }

    @Override
    public void onEntityTick(WeatherCell cell, LivingEntity entity) {
        if (entity instanceof Enderman) {
            entity.damage(1.0);
        }
    }
}
