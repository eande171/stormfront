package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
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
        applyFreezeEffect(player, intensity);
        WeatherUtils.applyMovementPenalty(player, intensity, 0.1f, 0.15f, 0.05f);
        WeatherUtils.extinguishNearbyFires(player, intensity);
    }

    @Override
    public void onPlayerExit(Player player) {
        freezeLevels.remove(player.getUniqueId());
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
