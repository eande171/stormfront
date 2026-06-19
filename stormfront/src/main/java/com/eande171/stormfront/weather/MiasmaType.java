package com.eande171.stormfront.weather;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.Set;

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
    public Set<String> getCompatibleBiomes() {
        return Set.of("minecraft:swamp", "minecraft:mangrove_swamp");
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
    }

    private void applyNausea(Player player, float intensity) {
        if (intensity < NAUSEA_THRESHOLD || !WeatherUtils.isExposed(player.getLocation())) {
            player.removePotionEffect(PotionEffectType.NAUSEA);
            return;
        }
        // No icon, no ambient particles - just the screen warp
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, false, false));
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
            Integer groundY = WeatherUtils.findGroundY(world, (int) x, feet.getBlockY(), (int) z);
            if (groundY == null) continue;
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                new Location(world, x, groundY + 0.1, z),
                1, 0.1, 0.05, 0.1, 0.003);
        }

        // Green gas peeking through the smoke
        int greenCount = (int) (5 * intensity);
        for (int i = 0; i < greenCount; i++) {
            double x = feet.getX() + (RANDOM.nextDouble() - 0.5) * 14;
            double z = feet.getZ() + (RANDOM.nextDouble() - 0.5) * 14;
            Integer groundY = WeatherUtils.findGroundY(world, (int) x, feet.getBlockY(), (int) z);
            if (groundY == null) continue;
            player.spawnParticle(Particle.DUST,
                new Location(world, x, groundY + 0.1 + RANDOM.nextDouble() * 1.5, z),
                1, 0.05, 0.1, 0.05, 0, GAS_GREENS[RANDOM.nextInt(GAS_GREENS.length)]);
        }

        // Bubbles floating and popping in the gas cloud
        int bubbleCount = (int) (5 * intensity);
        for (int i = 0; i < bubbleCount; i++) {
            double x = feet.getX() + (RANDOM.nextDouble() - 0.5) * 12;
            double z = feet.getZ() + (RANDOM.nextDouble() - 0.5) * 12;
            Integer groundY = WeatherUtils.findGroundY(world, (int) x, feet.getBlockY(), (int) z);
            if (groundY == null) continue;
            double bubbleY = groundY + 0.5 + RANDOM.nextDouble() * 4;
            player.spawnParticle(Particle.BUBBLE_POP,
                new Location(world, x, bubbleY, z),
                1, 0.05, 0.05, 0.05, 0.01);
        }

        // Mid-range plumes - fills the gap and makes the gas visible before you're in it
        int midCount = (int) (6 * intensity);
        for (int i = 0; i < midCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = 14 + RANDOM.nextDouble() * 10;
            double x = feet.getX() + dist * Math.cos(angle);
            double z = feet.getZ() + dist * Math.sin(angle);
            Integer groundY = WeatherUtils.findGroundY(world, (int) x, feet.getBlockY(), (int) z);
            if (groundY == null) continue;
            player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                new Location(world, x, groundY + 0.5, z),
                1, 0.1, 0.05, 0.1, 0.003);
        }

        // Distant mist - sparse, 35-50 blocks out, appears stationary as player approaches
        int distantCount = Math.max(2, (int) (5 * intensity));
        for (int i = 0; i < distantCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = 35 + RANDOM.nextDouble() * 15;
            double x = feet.getX() + dist * Math.cos(angle);
            double z = feet.getZ() + dist * Math.sin(angle);
            Integer groundY = WeatherUtils.findGroundY(world, (int) x, feet.getBlockY(), (int) z);
            if (groundY == null) continue;
            player.spawnParticle(Particle.WHITE_ASH,
                new Location(world, x, groundY + 1.0, z),
                1, 0.2, 0.3, 0.2, 0.005);
        }
    }
}
