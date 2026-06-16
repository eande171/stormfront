package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.Set;

public class HeatwaveType implements WeatherType {

    private static final Random RANDOM = new Random();
    private static final float NORMAL_WALK_SPEED = 0.2f;
    // ~1 hunger point every 20 seconds at full intensity (scheduler default: 4 ticks)
    private static final float HUNGER_DRAIN_CHANCE = 0.01f;

    private static final Set<String> HOT_BIOMES = Set.of(
        "minecraft:desert",
        "minecraft:savanna",
        "minecraft:savanna_plateau",
        "minecraft:windswept_savanna",
        "minecraft:badlands",
        "minecraft:wooded_badlands",
        "minecraft:eroded_badlands",
        "minecraft:warm_ocean",
        "minecraft:jungle",
        "minecraft:sparse_jungle",
        "minecraft:bamboo_jungle"
    );

    @Override
    public String getId() { return "stormfront:heatwave"; }

    @Override
    public int getPriority() { return 2; }

    // Clear oppressive sky - no rain or thunder visuals
    @Override
    public float getRainMultiplier() { return 0f; }

    @Override
    public float getThunderMultiplier() { return 0f; }

    @Override
    public Set<String> getCompatibleBiomes() { return HOT_BIOMES; }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        float distanceFactor = distanceFactor(cell, player.getLocation());
        float intensity = cell.getIntensity() * distanceFactor;

        spawnHeatParticles(player, intensity);
        applyMovementPenalty(player, intensity);
        applyHungerDrain(player, intensity);
    }

    @Override
    public void onPlayerExit(Player player) {
        player.setWalkSpeed(NORMAL_WALK_SPEED);
    }

    private void spawnHeatParticles(Player player, float intensity) {
        if (intensity <= 0) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();

        // Ground shimmer - heat rising from hot ground, hugging the surface
        int groundCount = (int) (8 * intensity);
        for (int i = 0; i < groundCount; i++) {
            double x = loc.getX() + (RANDOM.nextDouble() - 0.5) * 8;
            double y = loc.getY() + RANDOM.nextDouble() * 0.5;
            double z = loc.getZ() + (RANDOM.nextDouble() - 0.5) * 8;
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                new Location(world, x, y, z),
                1, 0.1, 0, 0.1, 0.003);
        }

        // Dust haze - dry particles drifting through the air at eye level
        int dustCount = (int) (12 * intensity);
        for (int i = 0; i < dustCount; i++) {
            double x = loc.getX() + (RANDOM.nextDouble() - 0.5) * 14;
            double y = loc.getY() + 1 + (RANDOM.nextDouble() - 0.5) * 3;
            double z = loc.getZ() + (RANDOM.nextDouble() - 0.5) * 14;
            player.spawnParticle(Particle.WHITE_ASH,
                new Location(world, x, y, z),
                1, 0.3, 0.1, 0.3, 0.005);
        }

        // Far haze - distant shimmer at the edge of visibility
        int farCount = (int) (5 * intensity);
        for (int i = 0; i < farCount; i++) {
            double x = loc.getX() + (RANDOM.nextDouble() - 0.5) * 24;
            double y = loc.getY() + (RANDOM.nextDouble() - 0.2) * 4;
            double z = loc.getZ() + (RANDOM.nextDouble() - 0.5) * 24;
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                new Location(world, x, y, z),
                1, 0.4, 0.2, 0.4, 0.004);
        }
    }

    private void applyMovementPenalty(Player player, float intensity) {
        if (!WeatherUtils.isExposed(player.getLocation())) {
            player.setWalkSpeed(NORMAL_WALK_SPEED);
            return;
        }
        if (intensity < 0.1f) {
            player.setWalkSpeed(NORMAL_WALK_SPEED);
            return;
        }
        // Heat exhaustion - same drag as a blizzard, different cause
        float speed = Math.max(0.15f, NORMAL_WALK_SPEED - 0.05f * intensity);
        player.setWalkSpeed(speed);
    }

    private void applyHungerDrain(Player player, float intensity) {
        if (!WeatherUtils.isExposed(player.getLocation())) return;
        // Light heatwaves are uncomfortable, not dehydrating
        if (intensity < 0.3f) return;
        if (RANDOM.nextFloat() >= intensity * HUNGER_DRAIN_CHANCE) return;
        int current = player.getFoodLevel();
        if (current > 0) {
            player.setFoodLevel(current - 1);
        }
    }

    private float distanceFactor(WeatherCell cell, Location location) {
        double distance = location.distance(cell.getCenter());
        return Math.max(0f, 1.0f - (float) (distance / cell.getRadius()));
    }
}
