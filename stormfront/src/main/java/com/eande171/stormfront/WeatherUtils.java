package com.eande171.stormfront;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;

import java.util.Random;

public final class WeatherUtils {

    private static final Random RANDOM = new Random();

    // Normal campfire goes out fairly quickly in heavy rain
    private static final float CAMPFIRE_EXTINGUISH_CHANCE = 0.15f;

    // Soul fire is supernatural - rain barely touches it
    private static final float SOUL_CAMPFIRE_EXTINGUISH_CHANCE = 0.03f;

    // Loose fire blocks are snuffed out faster than a sheltered flame
    private static final float FIRE_EXTINGUISH_CHANCE = 0.20f;

    private static final int FIRE_SCAN_RADIUS = 6;

    private WeatherUtils() {}

    /**
     * Returns true if the location has a clear view of the sky.
     * Uses MOTION_BLOCKING_NO_LEAVES so players under trees still count as exposed.
     */
    public static boolean isExposed(Location location) {
        World world = location.getWorld();
        if (world == null) return false;
        int highestY = world.getHighestBlockYAt(
            location.getBlockX(),
            location.getBlockZ(),
            HeightMap.MOTION_BLOCKING_NO_LEAVES
        );
        return highestY <= location.getBlockY();
    }

    /**
     * Samples random block positions near the player and attempts to extinguish
     * exposed campfires and fire blocks. Soul campfires resist significantly more.
     * Scales the number of attempts with intensity so heavy rain acts faster.
     */
    public static void extinguishNearbyFires(Player player, float intensity) {
        if (intensity <= 0) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        int attempts = 1 + (int) (intensity * 2);

        for (int i = 0; i < attempts; i++) {
            int x = loc.getBlockX() + RANDOM.nextInt(FIRE_SCAN_RADIUS * 2 + 1) - FIRE_SCAN_RADIUS;
            int z = loc.getBlockZ() + RANDOM.nextInt(FIRE_SCAN_RADIUS * 2 + 1) - FIRE_SCAN_RADIUS;
            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);

            // Check at highest exposed surface and one block above for raised campfires
            tryExtinguish(world.getBlockAt(x, y, z));
            tryExtinguish(world.getBlockAt(x, y + 1, z));
        }
    }

    private static void tryExtinguish(Block block) {
        Material type = block.getType();

        if (type == Material.FIRE) {
            if (RANDOM.nextFloat() < FIRE_EXTINGUISH_CHANCE) {
                block.setType(Material.AIR);
            }
            return;
        }

        if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
            if (!(block.getBlockData() instanceof Lightable lightable) || !lightable.isLit()) return;
            float chance = type == Material.SOUL_CAMPFIRE
                ? SOUL_CAMPFIRE_EXTINGUISH_CHANCE
                : CAMPFIRE_EXTINGUISH_CHANCE;
            if (RANDOM.nextFloat() < chance) {
                lightable.setLit(false);
                block.setBlockData(lightable);
            }
        }
    }
}
