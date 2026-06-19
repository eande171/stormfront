package com.eande171.stormfront;

import com.eande171.stormfront.api.WeatherCell;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.Set;

public final class WeatherUtils {

    private static final Random RANDOM = new Random();

    public static final float NORMAL_WALK_SPEED = 0.2f;

    // Biomes where precipitation does not occur - rain effects should not apply here
    private static final Set<String> DRY_BIOMES = Set.of(
        "minecraft:desert",
        "minecraft:badlands",
        "minecraft:wooded_badlands",
        "minecraft:eroded_badlands",
        "minecraft:savanna",
        "minecraft:savanna_plateau",
        "minecraft:windswept_savanna"
    );

    public static boolean isDryBiome(Location location) {
        return DRY_BIOMES.contains(location.getBlock().getBiome().getKey().toString());
    }

    private static final float CAMPFIRE_EXTINGUISH_CHANCE = 0.15f;
    private static final float SOUL_CAMPFIRE_EXTINGUISH_CHANCE = 0.03f;
    private static final float FIRE_EXTINGUISH_CHANCE = 0.20f;
    private static final int FIRE_SCAN_RADIUS = 6;

    private WeatherUtils() {}

    // Returns true if the location has a clear view of the sky.
    // Uses MOTION_BLOCKING_NO_LEAVES so players under trees still count as exposed.
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

    public static float distanceFactor(WeatherCell cell, Location location) {
        double distance = location.distance(cell.getCenter());
        return Math.max(0f, 1.0f - (float) (distance / cell.getRadius()));
    }

    // Scans downward from playerY and returns the Y of the highest solid block found,
    // or null if nothing is found within 8 blocks.
    public static Integer findGroundY(World world, int x, int playerY, int z) {
        for (int y = playerY; y >= playerY - 8; y--) {
            if (!world.getBlockAt(x, y, z).getType().isAir()) {
                return y;
            }
        }
        return null;
    }

    // Applies a walk speed penalty scaled by intensity. Only affects exposed players.
    // Restores normal speed if the player is indoors or below the threshold.
    public static void applyMovementPenalty(Player player, float intensity, float threshold, float minSpeed, float slope) {
        if (!isExposed(player.getLocation())) {
            player.setWalkSpeed(NORMAL_WALK_SPEED);
            return;
        }
        if (intensity < threshold) {
            player.setWalkSpeed(NORMAL_WALK_SPEED);
            return;
        }
        player.setWalkSpeed(Math.max(minSpeed, NORMAL_WALK_SPEED - slope * intensity));
    }

    public static void resetWalkSpeed(Player player) {
        player.setWalkSpeed(NORMAL_WALK_SPEED);
    }

    // Samples random block positions near the player and attempts to extinguish
    // exposed campfires and fire blocks. Soul campfires resist significantly more.
    // Scales the number of attempts with intensity so heavy rain acts faster.
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
