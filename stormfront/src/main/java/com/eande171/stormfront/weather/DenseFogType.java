package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

public class DenseFogType implements WeatherType {

    private static final Random RANDOM = new Random();

    @Override
    public String getId() { return "stormfront:fog"; }

    @Override
    public int getPriority() { return 1; }

    // Fog has its own visual identity - no rain sky or thunder darkening
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

        applyMovementPenalty(player, intensity);
        spawnFogParticles(player, intensity);
    }

    private void applyMovementPenalty(Player player, float intensity) {
        if (!WeatherUtils.isExposed(player.getLocation())) {
            player.setWalkSpeed(0.2f);
            return;
        }
        // Fog only causes disorientation at higher intensities - light fog is fine
        if (intensity < 0.5f) {
            player.setWalkSpeed(0.2f);
            return;
        }
        // Mild effect - fog slows you by caution, not by force
        float speed = Math.max(0.175f, 0.2f - 0.025f * intensity);
        player.setWalkSpeed(speed);
    }

    @Override
    public void onPlayerExit(Player player) {
        player.setWalkSpeed(0.2f);
    }

    private void spawnFogParticles(Player player, float intensity) {
        if (intensity <= 0) return;

        Location eyes = player.getEyeLocation();
        World world = eyes.getWorld();

        // Close layer - fills the immediate field of view, most obstructive
        int closeCount = (int) (12 * intensity);
        for (int i = 0; i < closeCount; i++) {
            double x = eyes.getX() + (RANDOM.nextDouble() - 0.5) * 5;
            double y = eyes.getY() + (RANDOM.nextDouble() - 0.5) * 3;
            double z = eyes.getZ() + (RANDOM.nextDouble() - 0.5) * 5;
            player.spawnParticle(Particle.CLOUD,
                new Location(world, x, y, z),
                1, 0.15, 0.05, 0.15, 0.001);
        }

        // Mid layer - ambient haze in the surrounding area
        int midCount = (int) (10 * intensity);
        for (int i = 0; i < midCount; i++) {
            double x = eyes.getX() + (RANDOM.nextDouble() - 0.5) * 12;
            double y = eyes.getY() + (RANDOM.nextDouble() - 0.4) * 5;
            double z = eyes.getZ() + (RANDOM.nextDouble() - 0.5) * 12;
            player.spawnParticle(Particle.CLOUD,
                new Location(world, x, y, z),
                1, 0.3, 0.1, 0.3, 0.003);
        }

        // Far layer - distant drifting wisps, gives the impression of smoke in the distance
        int farCount = (int) (6 * intensity);
        for (int i = 0; i < farCount; i++) {
            double x = eyes.getX() + (RANDOM.nextDouble() - 0.5) * 22;
            double y = eyes.getY() + (RANDOM.nextDouble() - 0.3) * 7;
            double z = eyes.getZ() + (RANDOM.nextDouble() - 0.5) * 22;
            player.spawnParticle(Particle.CLOUD,
                new Location(world, x, y, z),
                2, 0.5, 0.2, 0.5, 0.005);
        }

        // Beyond-far layer - campfire smoke at the edge of visibility, rising haze effect
        int smokeCount = (int) (5 * intensity);
        for (int i = 0; i < smokeCount; i++) {
            double x = eyes.getX() + (RANDOM.nextDouble() - 0.5) * 34;
            double y = eyes.getY() + (RANDOM.nextDouble() - 0.5) * 6;
            double z = eyes.getZ() + (RANDOM.nextDouble() - 0.5) * 34;
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                new Location(world, x, y, z),
                1, 0.4, 0.3, 0.4, 0.003);
        }
    }

    private float distanceFactor(WeatherCell cell, Location location) {
        double distance = location.distance(cell.getCenter());
        return Math.max(0f, 1.0f - (float) (distance / cell.getRadius()));
    }
}
