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

    protected void spawnRainImpacts(Player player, float intensity) {
        if (WeatherUtils.isDryBiome(player.getLocation())) return;
        int count = (int) (15 * intensity);
        if (count <= 0) return;

        Location feet = player.getLocation();
        World world = feet.getWorld();

        for (int i = 0; i < count; i++) {
            int x = (int) (feet.getX() + (RANDOM.nextDouble() - 0.5) * 6);
            int z = (int) (feet.getZ() + (RANDOM.nextDouble() - 0.5) * 6);

            Integer groundY = WeatherUtils.findGroundY(world, x, feet.getBlockY(), z);
            if (groundY == null) continue;

            // Skip if covered - no rain indoors
            if (world.getHighestBlockYAt(x, z) != groundY) continue;

            player.spawnParticle(Particle.SPLASH,
                new Location(world, x + 0.5, groundY + 1, z + 0.5),
                1, 0.2, 0, 0.2, 0);
        }

        // Distant impacts - sparse, large radius, appear stationary as the player approaches
        int distantCount = Math.max(1, (int) (3 * intensity));
        for (int i = 0; i < distantCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = 30 + RANDOM.nextDouble() * 20;
            int x = (int) (feet.getX() + dist * Math.cos(angle));
            int z = (int) (feet.getZ() + dist * Math.sin(angle));
            Integer groundY = WeatherUtils.findGroundY(world, x, feet.getBlockY(), z);
            if (groundY == null) continue;
            if (world.getHighestBlockYAt(x, z) != groundY) continue;
            player.spawnParticle(Particle.SPLASH,
                new Location(world, x + 0.5, groundY + 1, z + 0.5),
                1, 0.2, 0, 0.2, 0);
        }
    }

    @Override
    public void onPlayerExit(Player player) {
        WeatherUtils.resetWalkSpeed(player);
    }
}
