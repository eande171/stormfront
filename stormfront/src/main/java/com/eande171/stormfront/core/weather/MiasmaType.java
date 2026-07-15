package com.eande171.stormfront.core.weather;

import com.eande171.stormfront.api.engine.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MiasmaType implements WeatherType {

    private static final Random RANDOM = new Random();
    private static final float NAUSEA_THRESHOLD = 0.6f;

    private static final Particle.DustOptions[] GAS_GREENS = {
        new Particle.DustOptions(Color.fromRGB(75, 170, 50), 1.2f),
        new Particle.DustOptions(Color.fromRGB(95, 205, 65), 1.0f),
        new Particle.DustOptions(Color.fromRGB(55, 140, 35), 1.4f),
    };

    @Override
    public String getId() { return "stormfront:miasma"; }

    @Override
    public int getPriority() { return 1; }

    @Override
    public float getNaturalSpawnWeight() { return 1.0f; }

    @Override
    public float getRainMultiplier() { return 0f; }

    @Override
    public float getThunderMultiplier() { return 0.35f; }

    @Override
    public Set<NamespacedKey> getCompatibleBiomes() {
        return Set.of(NamespacedKey.minecraft("swamp"), NamespacedKey.minecraft("mangrove_swamp"));
    }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        float intensity = cell.getIntensity() * WeatherUtils.distanceFactor(cell, player.getLocation());
        spawnMiasmaParticles(player, intensity);
        applyNausea(player, intensity);
    }

    @Override
    public void onPlayerExit(Player player) {
        player.removePotionEffect(PotionEffectType.NAUSEA);
        nauseaExpiredAt.remove(player.getUniqueId());
    }

    private static final int  NAUSEA_DURATION  = 130; // 6.5 seconds
    private static final long NAUSEA_GAP_MS    = 3500; // 3.5 second gap between pulses

    private final Map<UUID, Long> nauseaExpiredAt = new HashMap<>();

    private void applyNausea(Player player, float intensity) {
        UUID uuid = player.getUniqueId();
        if (intensity < NAUSEA_THRESHOLD) {
            player.removePotionEffect(PotionEffectType.NAUSEA);
            nauseaExpiredAt.remove(uuid);
            return;
        }
        PotionEffect existing = player.getPotionEffect(PotionEffectType.NAUSEA);
        if (existing != null) return;
        // Nausea just expired - record the time and start the gap
        long now = System.currentTimeMillis();
        if (!nauseaExpiredAt.containsKey(uuid)) {
            nauseaExpiredAt.put(uuid, now);
            return;
        }
        if (now - nauseaExpiredAt.get(uuid) < NAUSEA_GAP_MS) return;
        nauseaExpiredAt.remove(uuid);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, NAUSEA_DURATION, 0, false, false, false));
    }

    private Location groundNear(World world, double x, int refY, double z, double yOffset) {
        Integer groundY = WeatherUtils.findGroundY(world, (int) x, refY, (int) z);
        if (groundY == null) return null;
        return new Location(world, x, groundY + yOffset, z);
    }

    private void spawnMiasmaParticles(Player player, float intensity) {
        if (intensity <= 0 || !WeatherUtils.isExposed(player.getLocation())) return;

        Location feet = player.getLocation();
        World world = feet.getWorld();

        // Swamp gas - campfire plumes pinned to the ground, wide radius
        int cloudCount = (int) (18 * intensity);
        for (int i = 0; i < cloudCount; i++) {
            double x = feet.getX() + (RANDOM.nextDouble() - 0.5) * 16;
            double z = feet.getZ() + (RANDOM.nextDouble() - 0.5) * 16;
            Location loc = groundNear(world, x, feet.getBlockY(), z, 0.1);
            if (loc == null) continue;
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 1, 0.1, 0.05, 0.1, 0.003);
        }

        // Green gas peeking through the smoke
        int greenCount = (int) (5 * intensity);
        for (int i = 0; i < greenCount; i++) {
            double x = feet.getX() + (RANDOM.nextDouble() - 0.5) * 14;
            double z = feet.getZ() + (RANDOM.nextDouble() - 0.5) * 14;
            Location loc = groundNear(world, x, feet.getBlockY(), z, 0.1 + RANDOM.nextDouble() * 1.5);
            if (loc == null) continue;
            player.spawnParticle(Particle.DUST, loc, 1, 0.05, 0.1, 0.05, 0, GAS_GREENS[RANDOM.nextInt(GAS_GREENS.length)]);
        }

        // Bubbles floating and popping in the gas cloud
        int bubbleCount = (int) (5 * intensity);
        for (int i = 0; i < bubbleCount; i++) {
            double x = feet.getX() + (RANDOM.nextDouble() - 0.5) * 12;
            double z = feet.getZ() + (RANDOM.nextDouble() - 0.5) * 12;
            Location loc = groundNear(world, x, feet.getBlockY(), z, 0.5 + RANDOM.nextDouble() * 4);
            if (loc == null) continue;
            player.spawnParticle(Particle.BUBBLE_POP, loc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Mid-range plumes - fills the gap and makes the gas visible before you're in it
        int midCount = (int) (6 * intensity);
        WeatherUtils.spawnScatteredGroundField(player, Particle.CAMPFIRE_COSY_SMOKE, midCount,
            14, 24, 0.5, false, 1, 0.1, 0.05, 0.1, 0.003);

        // Spore blossom drift - wide radius, spawned above head height so they fall through the gas
        int sporeCount = (int) (7 * intensity);
        for (int i = 0; i < sporeCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = 10 + RANDOM.nextDouble() * 10;
            double x = feet.getX() + dist * Math.cos(angle);
            double y = feet.getY() + 3 + RANDOM.nextDouble() * 2;
            double z = feet.getZ() + dist * Math.sin(angle);
            player.spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                new Location(world, x, y, z),
                1, 0.2, 0.1, 0.2, 0);
        }

        // Distant mist - sparse, 35-50 blocks out, appears stationary as player approaches
        int distantCount = Math.max(2, (int) (5 * intensity));
        WeatherUtils.spawnScatteredGroundField(player, Particle.WHITE_ASH, distantCount,
            35, 50, 1.0, false, 1, 0.2, 0.3, 0.2, 0.005);
    }
}
