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

    @Override
    public float getNaturalSpawnWeight() { return 0.25f; }

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
        float intensity = cell.getIntensity() * WeatherUtils.distanceFactor(cell, player.getLocation());

        spawnHeatParticles(player, intensity);
        WeatherUtils.applyMovementPenalty(player, intensity, 0.1f, 0.15f, 0.05f);
        applyHungerDrain(player, intensity);
    }

    @Override
    public void onPlayerExit(Player player) {
        WeatherUtils.resetWalkSpeed(player);
    }

    private void spawnHeatParticles(Player player, float intensity) {
        if (intensity <= 0) return;
        if (!WeatherUtils.isExposed(player.getLocation())) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();

        // Ground shimmer - heat rising from the actual surface below each spawn point
        int groundCount = (int) (8 * intensity);
        for (int i = 0; i < groundCount; i++) {
            int x = (int) (loc.getX() + (RANDOM.nextDouble() - 0.5) * 8);
            int z = (int) (loc.getZ() + (RANDOM.nextDouble() - 0.5) * 8);
            Integer surface = WeatherUtils.findGroundY(world, x, loc.getBlockY(), z);
            double groundY = surface != null ? surface + 1.0 : loc.getY();
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                new Location(world, x + 0.5, groundY, z + 0.5),
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

        // Far haze - distant shimmer, 35-50 blocks out, appears stationary as player approaches
        int farCount = (int) (10 * intensity);
        for (int i = 0; i < farCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = 35 + RANDOM.nextDouble() * 15;
            double x = loc.getX() + dist * Math.cos(angle);
            double y = loc.getY() + (RANDOM.nextDouble() - 0.2) * 4;
            double z = loc.getZ() + dist * Math.sin(angle);
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                new Location(world, x, y, z),
                1, 0.4, 0.2, 0.4, 0.004);
        }
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
}
