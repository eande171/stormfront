package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.HeightMap;

public class ThunderstormType extends AbstractRainType {

    // At full intensity, roughly one strike every 10 seconds (scheduler runs every 4 ticks)
    private static final float LIGHTNING_CHANCE = 0.02f;

    @Override
    public String getId() { return "stormfront:thunderstorm"; }

    @Override
    public int getPriority() { return 2; }

    @Override
    public float getThunderMultiplier() { return 0.9f; }

    @Override
    public float getNaturalSpawnWeight() { return 0.6f; }

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
        maybeStrikeLightning(player, particleIntensity);
        WeatherUtils.extinguishNearbyFires(player, particleIntensity);
    }

    private void maybeStrikeLightning(Player player, float intensity) {
        if (intensity <= 0) return;
        if (RANDOM.nextFloat() >= intensity * LIGHTNING_CHANCE) return;

        // Strike at a random location within ~20 blocks - Gaussian so most land close
        double offsetX = RANDOM.nextGaussian() * 20;
        double offsetZ = RANDOM.nextGaussian() * 20;
        Location base = player.getLocation().add(offsetX, 0, offsetZ);
        int groundY = base.getWorld().getHighestBlockYAt(base.getBlockX(), base.getBlockZ(), HeightMap.MOTION_BLOCKING);
        Location strike = new Location(base.getWorld(), base.getX(), groundY, base.getZ());
        base.getWorld().strikeLightningEffect(strike);
    }
}
