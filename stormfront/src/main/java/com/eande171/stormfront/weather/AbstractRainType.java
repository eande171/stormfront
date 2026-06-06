package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Random;

public abstract class AbstractRainType implements WeatherType {

    protected static final Random RANDOM = new Random();
    private static final int GROUND_SCAN_DEPTH = 5;
    private static final float NORMAL_WALK_SPEED = 0.2f;
    private static final float MIN_WALK_SPEED = 0.185f;

    protected void spawnRainImpacts(Player player, float intensity) {
        int count = (int) (15 * intensity);
        if (count <= 0) return;

        Location feet = player.getLocation();
        World world = feet.getWorld();

        for (int i = 0; i < count; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 6;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 6;

            int x = (int) (feet.getX() + offsetX);
            int z = (int) (feet.getZ() + offsetZ);

            Integer groundY = findGroundY(world, x, feet.getBlockY(), z);
            if (groundY == null) continue;

            // Skip if covered - no rain indoors
            if (world.getHighestBlockYAt(x, z) != groundY) continue;

            player.spawnParticle(Particle.SPLASH,
                new Location(world, x + 0.5, groundY + 1, z + 0.5),
                1, 0.2, 0, 0.2, 0);
        }
    }

    protected void applyMovementPenalty(Player player, float intensity) {
        if (!WeatherUtils.isExposed(player.getLocation())) {
            player.setWalkSpeed(NORMAL_WALK_SPEED);
            return;
        }
        if (intensity < 0.1f) {
            player.setWalkSpeed(NORMAL_WALK_SPEED);
            return;
        }
        float speed = Math.max(MIN_WALK_SPEED, NORMAL_WALK_SPEED - 0.015f * intensity);
        player.setWalkSpeed(speed);
    }

    @Override
    public void onPlayerExit(Player player) {
        player.setWalkSpeed(NORMAL_WALK_SPEED);
    }

    protected float distanceFactor(WeatherCell cell, Location location) {
        double distance = location.distance(cell.getCenter());
        return Math.max(0f, 1.0f - (float) (distance / cell.getRadius()));
    }

    private Integer findGroundY(World world, int x, int startY, int z) {
        for (int y = startY; y >= startY - GROUND_SCAN_DEPTH; y--) {
            if (!world.getBlockAt(x, y, z).getType().isAir()) {
                return y;
            }
        }
        return null;
    }
}
