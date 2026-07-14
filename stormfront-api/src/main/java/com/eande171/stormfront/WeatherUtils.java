package com.eande171.stormfront;

import com.eande171.stormfront.api.WeatherCell;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

public final class WeatherUtils {

    private static final Random RANDOM = new Random();

    public static final float NORMAL_WALK_SPEED = 0.2f;

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

    // Nether terrain is cave-like, not open sky - checks local headroom rather than a clear ceiling
    private static final int NETHER_EXPOSURE_SCAN_HEIGHT = 8;

    // Clear view of the sky; MOTION_BLOCKING_NO_LEAVES so players under trees still count as exposed
    public static boolean isExposed(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        if (world.getEnvironment() == World.Environment.NETHER) {
            return isExposedNether(location, world);
        }

        int highestY = world.getHighestBlockYAt(
            location.getBlockX(),
            location.getBlockZ(),
            HeightMap.MOTION_BLOCKING_NO_LEAVES
        );
        return highestY < location.getBlockY();
    }

    // Bounded upward scan instead of a global column check, since Nether ceilings make that meaningless
    private static boolean isExposedNether(Location location, World world) {
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int startY = location.getBlockY() + 1;
        int maxY = Math.min(world.getMaxHeight() - 1, startY + NETHER_EXPOSURE_SCAN_HEIGHT);
        for (int y = startY; y <= maxY; y++) {
            if (!world.getBlockAt(x, y, z).getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    public static float distanceFactor(WeatherCell cell, Location location) {
        double distance = location.distance(cell.getCenter());
        return Math.max(0f, 1.0f - (float) (distance / cell.getRadius()));
    }

    public static Integer findGroundY(World world, int x, int playerY, int z) {
        for (int y = playerY; y >= playerY - 8; y--) {
            if (!world.getBlockAt(x, y, z).getType().isAir()) {
                return y;
            }
        }
        return null;
    }

    // Scatters particles at random angle/distance around the player, snapped to the ground below each point.
    // requireOpenSky additionally skips positions covered overhead (e.g. rain that shouldn't fall indoors).
    public static void spawnScatteredGroundField(Player player, Particle particle, int count,
                                                   double minDist, double maxDist, double heightOffset,
                                                   boolean requireOpenSky,
                                                   int particleCount, double spreadX, double spreadY, double spreadZ, double speed) {
        if (count <= 0) return;
        Location loc = player.getLocation();
        World world = loc.getWorld();

        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = minDist + RANDOM.nextDouble() * (maxDist - minDist);
            int x = (int) (loc.getX() + dist * Math.cos(angle));
            int z = (int) (loc.getZ() + dist * Math.sin(angle));

            Integer groundY = findGroundY(world, x, loc.getBlockY(), z);
            if (groundY == null) continue;
            if (requireOpenSky && world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) != groundY) continue;

            player.spawnParticle(particle, new Location(world, x + 0.5, groundY + heightOffset, z + 0.5),
                particleCount, spreadX, spreadY, spreadZ, speed);
        }
    }

    // Scatters particles at random angle/distance around the player, floating at a height relative to the
    // player rather than snapped to the ground - e.g. distant snowflakes or heat haze drifting in open air.
    public static void spawnScatteredAirField(Player player, Particle particle, int count,
                                                double minDist, double maxDist,
                                                double minHeightOffset, double maxHeightOffset,
                                                double spreadX, double spreadY, double spreadZ, double speed) {
        if (count <= 0) return;
        Location loc = player.getLocation();
        World world = loc.getWorld();

        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = minDist + RANDOM.nextDouble() * (maxDist - minDist);
            double x = loc.getX() + dist * Math.cos(angle);
            double y = loc.getY() + minHeightOffset + RANDOM.nextDouble() * (maxHeightOffset - minHeightOffset);
            double z = loc.getZ() + dist * Math.sin(angle);

            player.spawnParticle(particle, new Location(world, x, y, z), 1, spreadX, spreadY, spreadZ, speed);
        }
    }

    // Runs an addon-supplied action, logging and swallowing any exception instead of letting it abort the caller
    public static void safeRun(Logger logger, String description, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            logger.warning(description + ": " + e);
        }
    }

    // Same as safeRun but for a boolean-returning addon call; returns fallback if it throws
    public static boolean safeTest(Logger logger, String description, boolean fallback, BooleanSupplier test) {
        try {
            return test.getAsBoolean();
        } catch (Exception e) {
            logger.warning(description + ": " + e);
            return fallback;
        }
    }

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

    // Samples nearby fire/campfires and attempts to extinguish them; soul campfires resist more
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
