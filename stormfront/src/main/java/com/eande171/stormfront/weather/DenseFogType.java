package com.eande171.stormfront.weather;

import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

public class DenseFogType implements WeatherType {

    private static final Random RANDOM = new Random();

    @Override
    public String getId() { return "stormfront:fog"; }

    @Override
    public int getPriority() { return 1; }

    // Fog has its own visual identity — no rain sky or thunder darkening
    @Override
    public float getRainMultiplier() { return 0f; }

    @Override
    public float getThunderMultiplier() { return 0f; }

    @Override
    public Set<String> getCompatibleBiomes() { return Collections.emptySet(); }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        float distanceFactor = distanceFactor(cell, player.getLocation());
        float intensity = cell.getIntensity() * distanceFactor;

        applySlowness(player, intensity);
        spawnFogParticles(player, intensity);
    }

    private void applySlowness(Player player, float intensity) {
        if (intensity < 0.3f) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, true, false));
    }

    private void spawnFogParticles(Player player, float intensity) {
        int count = (int) (8 * intensity);
        if (count <= 0) return;

        Location eyes = player.getEyeLocation();
        World world = eyes.getWorld();

        for (int i = 0; i < count; i++) {
            double x = eyes.getX() + (RANDOM.nextDouble() - 0.5) * 14;
            double y = eyes.getY() + (RANDOM.nextDouble() - 0.3) * 5;
            double z = eyes.getZ() + (RANDOM.nextDouble() - 0.5) * 14;
            // Very slow drift speed so particles linger as wisps rather than shooting off
            player.spawnParticle(Particle.CLOUD,
                new Location(world, x, y, z),
                1, 0.3, 0.1, 0.3, 0.003);
        }
    }

    private float distanceFactor(WeatherCell cell, Location location) {
        double distance = location.distance(cell.getCenter());
        return Math.max(0f, 1.0f - (float) (distance / cell.getRadius()));
    }
}
