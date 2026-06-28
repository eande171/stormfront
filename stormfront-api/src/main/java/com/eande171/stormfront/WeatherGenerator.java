package com.eande171.stormfront;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import com.eande171.stormfront.services.ConfigService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class WeatherGenerator {

    private static final Random RANDOM = new Random();

    private final JavaPlugin plugin;
    private final CellManager cellManager;
    private final ConfigService configService;

    public void start() {
        long intervalTicks = (long) configService.getNaturalGenCheckIntervalSeconds() * 20L;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::attempt, intervalTicks, intervalTicks);
    }

    private void attempt() {
        if (!configService.isNaturalGenEnabled()) return;
        if (cellManager.getActiveCells().size() >= configService.getNaturalGenMaxActiveCells()) return;
        if (RANDOM.nextDouble() >= configService.getNaturalGenBaseSpawnChance()) return;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return;

        Player target = players.get(RANDOM.nextInt(players.size()));
        String biome = target.getLocation().getBlock().getBiome().getKey().toString();

        List<WeatherType> eligible = StormfrontAPI.get().getRegistry().getAll().stream()
            .filter(type -> type.getNaturalSpawnWeight() > 0)
            .filter(type -> type.getCompatibleBiomes().isEmpty() || type.getCompatibleBiomes().contains(biome))
            .filter(type -> type.canNaturallySpawn(target))
            .collect(Collectors.toList());

        if (eligible.isEmpty()) return;

        WeatherType selected = selectWeighted(eligible);

        double distance = configService.getNaturalGenSpawnDistanceMin()
            + RANDOM.nextDouble() * (configService.getNaturalGenSpawnDistanceMax() - configService.getNaturalGenSpawnDistanceMin());
        double angle = RANDOM.nextDouble() * Math.PI * 2;

        Location playerLoc = target.getLocation();
        double spawnX = playerLoc.getX() + Math.cos(angle) * distance;
        double spawnZ = playerLoc.getZ() + Math.sin(angle) * distance;
        Location spawnLoc = new Location(playerLoc.getWorld(), spawnX, playerLoc.getY(), spawnZ);

        // Movement vector pointing from spawn position toward the player
        double dx = playerLoc.getX() - spawnX;
        double dz = playerLoc.getZ() - spawnZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        double speed = configService.getNaturalGenMovementSpeedMin()
            + RANDOM.nextDouble() * (configService.getNaturalGenMovementSpeedMax() - configService.getNaturalGenMovementSpeedMin());
        Vector movementVector = new Vector((dx / len) * speed, 0, (dz / len) * speed);

        int radius = configService.getNaturalGenRadiusMin()
            + RANDOM.nextInt(configService.getNaturalGenRadiusMax() - configService.getNaturalGenRadiusMin() + 1);
        float intensity = configService.getNaturalGenIntensityMin()
            + (float) RANDOM.nextDouble() * (configService.getNaturalGenIntensityMax() - configService.getNaturalGenIntensityMin());
        int durationSeconds = configService.getNaturalGenDurationMinSeconds()
            + RANDOM.nextInt(configService.getNaturalGenDurationMaxSeconds() - configService.getNaturalGenDurationMinSeconds() + 1);

        WeatherCell cell = WeatherCell.builder()
            .id(UUID.randomUUID())
            .type(selected)
            .center(spawnLoc)
            .radius(radius)
            .intensity(intensity)
            .movementVector(movementVector)
            .startedAt(System.currentTimeMillis())
            .duration((long) durationSeconds * 20L)
            .build();

        cellManager.addCell(cell);
        plugin.getLogger().info("Naturally spawned " + selected.getId()
            + " (r=" + radius + ", i=" + String.format("%.2f", intensity)
            + ", " + durationSeconds + "s) near " + target.getName());
    }

    private WeatherType selectWeighted(List<WeatherType> types) {
        double totalWeight = types.stream().mapToDouble(WeatherType::getNaturalSpawnWeight).sum();
        double roll = RANDOM.nextDouble() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < types.size() - 1; i++) {
            cumulative += types.get(i).getNaturalSpawnWeight();
            if (roll < cumulative) return types.get(i);
        }
        return types.getLast();
    }
}
