package com.eande171.stormfront.nether.weather;

import com.eande171.stormfront.api.engine.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class SoulStormType implements WeatherType {

    private static final Random RANDOM = new Random();

    @Override
    public String getId() { return "stormfrontnether:soulstorm"; }

    @Override
    public int getPriority() { return 2; }

    @Override
    public float getNaturalSpawnWeight() { return 0.2f; }

    @Override
    public float getRainMultiplier() { return 0f; }

    @Override
    public float getThunderMultiplier() { return 0f; }

    @Override
    public Set<NamespacedKey> getCompatibleBiomes() {
        return Set.of(NamespacedKey.minecraft("soul_sand_valley"));
    }

    @Override
    public boolean canNaturallySpawn(Player target) {
        World world = target.getWorld();
        return world.getEnvironment() == World.Environment.NETHER;
    }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        float intensity = cell.getIntensity() * WeatherUtils.distanceFactor(cell, player.getLocation());

        spawnSoulPlumes(player, intensity);
        playSoulWhispers(player, intensity);
        applyDarkness(player, intensity);
    }

    @Override
    public void onPlayerExit(Player player) {
        UUID uuid = player.getUniqueId();
        lastAmbientPlayedAt.remove(uuid);
        darknessExpiredAt.remove(uuid);
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }

    @Override
    public void onEntityTick(WeatherCell cell, LivingEntity entity) {
        buffUndead(entity);
    }

    // Static plumes rising from the ground - nearby ones dotted around the player, sparse distant ones for scale
    private static final int    NEARBY_PLUME_COUNT      = 3;
    private static final int    DISTANT_PLUME_COUNT     = 5;
    private static final double NEARBY_PLUME_MIN_DIST   = 0.0;
    private static final double NEARBY_PLUME_MAX_DIST   = 10.0;
    private static final double DISTANT_PLUME_MIN_DIST  = 25.0;
    private static final double DISTANT_PLUME_MAX_DIST  = 45.0;
    private static final double PLUME_HEIGHT_OFFSET     = 0.5;
    private static final int    NEAR_PLUME_PARTICLES    = 6;
    private static final double NEAR_PLUME_SPREAD_XZ    = 0.15;
    private static final double NEAR_PLUME_SPREAD_Y     = 0.6;
    private static final double NEAR_PLUME_SPEED        = 0.03;
    private static final int    FAR_PLUME_PARTICLES     = 4;
    private static final double FAR_PLUME_SPREAD_XZ     = 0.15;
    private static final double FAR_PLUME_SPREAD_Y      = 0.8;
    private static final double FAR_PLUME_SPEED         = 0.03;

    private void spawnSoulPlumes(Player player, float intensity) {
        if (intensity <= 0 || !WeatherUtils.isExposed(player.getLocation())) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();

        spawnPlumeRing(player, world, loc, NEARBY_PLUME_COUNT, intensity,
            NEARBY_PLUME_MIN_DIST, NEARBY_PLUME_MAX_DIST,
            NEAR_PLUME_PARTICLES, NEAR_PLUME_SPREAD_XZ, NEAR_PLUME_SPREAD_Y, NEAR_PLUME_SPREAD_XZ, NEAR_PLUME_SPEED);

        spawnPlumeRing(player, world, loc, DISTANT_PLUME_COUNT, intensity,
            DISTANT_PLUME_MIN_DIST, DISTANT_PLUME_MAX_DIST,
            FAR_PLUME_PARTICLES, FAR_PLUME_SPREAD_XZ, FAR_PLUME_SPREAD_Y, FAR_PLUME_SPREAD_XZ, FAR_PLUME_SPEED);
    }

    private void spawnPlumeRing(Player player, World world, Location loc, int baseCount, float intensity,
                                  double minDist, double maxDist,
                                  int particleCount, double spreadX, double spreadY, double spreadZ, double speed) {
        int count = Math.max(1, (int) (baseCount * intensity));
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = minDist + RANDOM.nextDouble() * (maxDist - minDist);
            double x = loc.getX() + dist * Math.cos(angle);
            double z = loc.getZ() + dist * Math.sin(angle);
            Integer groundY = WeatherUtils.findGroundY(world, (int) x, loc.getBlockY(), (int) z);
            if (groundY == null) continue;
            player.spawnParticle(Particle.SOUL,
                new Location(world, x + 0.5, groundY + PLUME_HEIGHT_OFFSET, z + 0.5),
                particleCount, spreadX, spreadY, spreadZ, speed);
        }
    }

    // Whisper cue re-triggers periodically below the darkness threshold; above it, synced to each pulse instead
    private static final long AMBIENT_INTERVAL_MS = 6000;

    private final Map<UUID, Long> lastAmbientPlayedAt = new HashMap<>();

    private void playSoulWhispers(Player player, float intensity) {
        if (intensity <= 0 || intensity >= DARKNESS_THRESHOLD) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastAmbientPlayedAt.get(uuid);
        if (last != null && now - last < AMBIENT_INTERVAL_MS) return;

        lastAmbientPlayedAt.put(uuid, now);
        playWhisperSound(player, intensity);
    }

    private void playWhisperSound(Player player, float intensity) {
        float volume = 0.6f + 0.4f * Math.min(1.0f, intensity);
        player.playSound(player.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD,
            SoundCategory.AMBIENT, volume, 1.0f);
    }

    // Makes nearby undead harder to kill rather than more numerous; refreshed each pass like Blizzard's freeze buildup
    private static final int BUFF_DURATION_TICKS = 100;

    private void buffUndead(LivingEntity entity) {
        if (!(entity instanceof AbstractSkeleton)) return;

        entity.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH, BUFF_DURATION_TICKS, 0, true, false, false));
        entity.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE, BUFF_DURATION_TICKS, 1, true, false, false));

        Location center = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        entity.getWorld().spawnParticle(Particle.SOUL, center, 4, 0.3, 0.4, 0.3, 0.01);
    }

    // Pulsed Darkness, reusing Miasma's proven pulse timing so it reads as a deliberate breathing rhythm
    private static final float DARKNESS_THRESHOLD = 0.5f;
    private static final int   DARKNESS_DURATION  = 70;   // 3.5 seconds
    private static final long  DARKNESS_GAP_MS    = 6500; // 6.5 second gap between pulses

    private final Map<UUID, Long> darknessExpiredAt = new HashMap<>();

    private void applyDarkness(Player player, float intensity) {
        UUID uuid = player.getUniqueId();
        if (intensity < DARKNESS_THRESHOLD) {
            player.removePotionEffect(PotionEffectType.DARKNESS);
            darknessExpiredAt.remove(uuid);
            return;
        }
        PotionEffect existing = player.getPotionEffect(PotionEffectType.DARKNESS);
        if (existing != null) return;
        long now = System.currentTimeMillis();
        if (!darknessExpiredAt.containsKey(uuid)) {
            darknessExpiredAt.put(uuid, now);
            return;
        }
        if (now - darknessExpiredAt.get(uuid) < DARKNESS_GAP_MS) return;
        darknessExpiredAt.remove(uuid);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, DARKNESS_DURATION, 0, false, false, false));
        playWhisperSound(player, intensity);
    }
}
