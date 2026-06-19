package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import org.bukkit.Location;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.HeightMap;

import java.util.Collections;
import java.util.Set;

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
    public Set<String> getCompatibleBiomes() { return Collections.emptySet(); }

    @Override
    public boolean canNaturallySpawn(Player target) {
        return !WeatherUtils.isDryBiome(target.getLocation());
    }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        float distanceFactor = WeatherUtils.distanceFactor(cell, player.getLocation());
        float particleIntensity = cell.getIntensity() * distanceFactor * distanceFactor;

        spawnRainImpacts(player, particleIntensity);
        WeatherUtils.applyMovementPenalty(player, particleIntensity, 0.1f, 0.185f, 0.015f);
        maybeStrikeLightning(player, particleIntensity);
        WeatherUtils.extinguishNearbyFires(player, particleIntensity);
    }

    @Override
    public void onPlayerExit(Player player) {
        super.onPlayerExit(player);
    }

    @Override
    public void onEntityTick(WeatherCell cell, LivingEntity entity) {
        if (entity instanceof Enderman && !WeatherUtils.isDryBiome(entity.getLocation())) {
            entity.damage(1.0);
        }
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
