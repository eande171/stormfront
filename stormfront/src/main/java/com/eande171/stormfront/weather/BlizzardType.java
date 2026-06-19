package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class BlizzardType implements WeatherType {

    private static final Random RANDOM = new Random();

    private static final Set<String> COLD_BIOMES = Set.of(
        "minecraft:snowy_plains",
        "minecraft:snowy_taiga",
        "minecraft:ice_spikes",
        "minecraft:frozen_ocean",
        "minecraft:deep_frozen_ocean",
        "minecraft:frozen_river",
        "minecraft:snowy_beach",
        "minecraft:jagged_peaks",
        "minecraft:frozen_peaks",
        "minecraft:snowy_slopes",
        "minecraft:grove",
        "minecraft:cold_ocean",
        "minecraft:deep_cold_ocean",
        "minecraft:windswept_gravelly_hills"
    );

    @Override
    public String getId() { return "stormfront:blizzard"; }

    @Override
    public int getPriority() { return 2; }

    @Override
    public float getNaturalSpawnWeight() { return 0.25f; }

    // Full precipitation - shows vanilla snow in cold biomes, fades in/out via WeatherPacketService
    @Override
    public float getRainMultiplier() { return 1.0f; }

    // Subtle darkening - blizzard sky is white-grey, not stormy black
    @Override
    public float getThunderMultiplier() { return 0.15f; }

    @Override
    public Set<String> getCompatibleBiomes() { return COLD_BIOMES; }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        float intensity = cell.getIntensity() * WeatherUtils.distanceFactor(cell, player.getLocation());

        spawnSnowflakes(player, intensity);
        spawnGroundDrift(player, intensity);
        spawnWindParticles(player, intensity);
        applyFreezeEffect(player, intensity);
        WeatherUtils.applyMovementPenalty(player, intensity, 0.1f, 0.15f, 0.05f);
        WeatherUtils.extinguishNearbyFires(player, intensity);
    }

    @Override
    public void onPlayerExit(Player player) {
        UUID uuid = player.getUniqueId();
        freezeLevels.remove(uuid);
        windAngles.remove(uuid);
        windTimestamps.remove(uuid);
        windStreaks.remove(uuid);
        player.setFreezeTicks(0);
        WeatherUtils.resetWalkSpeed(player);
    }

    @Override
    public void onEntityTick(WeatherCell cell, LivingEntity entity) {
        // Gradually freeze nearby mobs - increment rather than set so it builds up naturally
        int target = Math.min(entity.getFreezeTicks() + 10, entity.getMaxFreezeTicks());
        entity.setFreezeTicks(target);
    }

    private void spawnSnowflakes(Player player, float intensity) {
        if (!WeatherUtils.isExposed(player.getLocation())) return;
        int count = (int) (20 * intensity);
        if (count <= 0) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();

        for (int i = 0; i < count; i++) {
            double x = loc.getX() + (RANDOM.nextDouble() - 0.5) * 8;
            double y = loc.getY() + 3 + RANDOM.nextDouble() * 4;
            double z = loc.getZ() + (RANDOM.nextDouble() - 0.5) * 8;
            // Small horizontal offset simulates wind-driven snow
            player.spawnParticle(Particle.SNOWFLAKE,
                new Location(world, x, y, z),
                1, 0.15, 0, 0.15, 0.02);
        }

        // Distant snowflakes - sparse, appear fixed in the air as player walks into them
        int distantCount = Math.max(1, (int) (5 * intensity));
        for (int i = 0; i < distantCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = 30 + RANDOM.nextDouble() * 20;
            double x = loc.getX() + dist * Math.cos(angle);
            double y = loc.getY() + 1 + RANDOM.nextDouble() * 8;
            double z = loc.getZ() + dist * Math.sin(angle);
            player.spawnParticle(Particle.SNOWFLAKE,
                new Location(world, x, y, z),
                1, 0.1, 0, 0.1, 0.01);
        }
    }

    private void spawnGroundDrift(Player player, float intensity) {
        if (!WeatherUtils.isExposed(player.getLocation())) return;
        int count = (int) (12 * intensity);
        if (count <= 0) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();

        for (int i = 0; i < count; i++) {
            int x = (int) (loc.getX() + (RANDOM.nextDouble() - 0.5) * 10);
            int z = (int) (loc.getZ() + (RANDOM.nextDouble() - 0.5) * 10);
            Integer surface = WeatherUtils.findGroundY(world, x, loc.getBlockY(), z);
            double groundY = surface != null ? surface + 1.0 : loc.getY();
            // Higher speed than fog drift - ground snow is blown around by wind
            player.spawnParticle(Particle.CLOUD,
                new Location(world, x + 0.5, groundY, z + 0.5),
                1, 0.3, 0.05, 0.3, 0.025);
        }
    }

    // Wind - sine wave streaks of debris blown in a consistent direction
    private static final long WIND_REFRESH_MS = 30_000;
    private static final long STREAK_LIFE_MS   = 2500;
    private static final int  MAX_STREAKS       = 4;
    private static final double STREAK_LENGTH   = 16.0;
    private static final double WAVE_AMPLITUDE  = 1.2;
    private static final double WAVE_FREQUENCY  = 0.25; // cycles per block
    private static final double WAVE_SPEED      = 5.0;  // blocks per second
    private static final int    POINTS_PER_STREAK = 10;

    private final Map<UUID, Long>          windTimestamps = new HashMap<>();
    private final Map<UUID, Double>        windAngles     = new HashMap<>();
    private final Map<UUID, List<double[]>> windStreaks   = new HashMap<>();

    private void spawnWindParticles(Player player, float intensity) {
        if (intensity < 0.3f || !WeatherUtils.isExposed(player.getLocation())) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (!windTimestamps.containsKey(uuid) || now - windTimestamps.get(uuid) > WIND_REFRESH_MS) {
            windAngles.put(uuid, RANDOM.nextDouble() * Math.PI * 2);
            windTimestamps.put(uuid, now);
            windStreaks.remove(uuid);
        }

        double wAngle = windAngles.get(uuid);
        double windX = Math.cos(wAngle);
        double windZ = Math.sin(wAngle);
        double perpX = -windZ;
        double perpZ =  windX;

        Location loc = player.getLocation();
        World world = loc.getWorld();
        double k     = 2 * Math.PI * WAVE_FREQUENCY;
        double omega = k * WAVE_SPEED;

        List<double[]> streaks = windStreaks.computeIfAbsent(uuid, id -> new ArrayList<>());
        streaks.removeIf(s -> now - s[3] > STREAK_LIFE_MS);

        // One new streak per tick until cap - they trickle in rather than all appearing at once
        if (streaks.size() < MAX_STREAKS) {
            double upstream = 6 + RANDOM.nextDouble() * 10;
            double sideways = (RANDOM.nextDouble() - 0.5) * 14;
            double sx = loc.getX() - windX * upstream + perpX * sideways;
            double sy = loc.getY() + 0.5 + RANDOM.nextDouble() * 4.5;
            double sz = loc.getZ() - windZ * upstream + perpZ * sideways;
            streaks.add(new double[]{sx, sy, sz, now});
        }

        // Render each streak as a travelling sine wave of WHITE_ASH
        for (double[] streak : streaks) {
            double age = (now - streak[3]) / 1000.0;
            for (int p = 0; p < POINTS_PER_STREAK; p++) {
                double t    = (p / (double)(POINTS_PER_STREAK - 1)) * STREAK_LENGTH;
                double wave = Math.sin(k * t - omega * age);
                double x = streak[0] + t * windX + perpX * WAVE_AMPLITUDE * wave;
                double y = streak[1] + 0.15 * wave;
                double z = streak[2] + t * windZ + perpZ * WAVE_AMPLITUDE * wave;
                // count=0: particle spawns at this position and flies in the wind direction
                player.spawnParticle(Particle.WHITE_ASH,
                    new Location(world, x, y, z),
                    0, windX, 0, windZ, 0.35);
            }
        }
    }

    // Ticks added to our tracker per scheduler tick - full freeze in ~12s at 4-tick interval
    private static final int FREEZE_RATE = 3;
    private final Map<UUID, Integer> freezeLevels = new HashMap<>();

    private void applyFreezeEffect(Player player, float intensity) {
        UUID uuid = player.getUniqueId();

        if (intensity <= 0) {
            freezeLevels.remove(uuid);
            return;
        }

        if (!WeatherUtils.isExposed(player.getLocation())) {
            // Sync tracker to vanilla drain so buildup resumes from the right level when exposed again
            freezeLevels.put(uuid, player.getFreezeTicks());
            return;
        }

        int target = (int) (player.getMaxFreezeTicks() * intensity);
        int current = freezeLevels.getOrDefault(uuid, player.getFreezeTicks());
        int next = Math.min(current + FREEZE_RATE, target);
        freezeLevels.put(uuid, next);
        player.setFreezeTicks(next);
    }
}
