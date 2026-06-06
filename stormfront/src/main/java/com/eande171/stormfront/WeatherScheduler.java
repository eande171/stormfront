package com.eande171.stormfront;

import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.events.PlayerEnterWeatherCellEvent;
import com.eande171.stormfront.api.events.PlayerExitWeatherCellEvent;
import com.eande171.stormfront.api.events.WeatherCellMoveEvent;
import com.eande171.stormfront.services.PlayerDataService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

@RequiredArgsConstructor
public class WeatherScheduler {

    private final PluginMain plugin;
    private final CellManager cellManager;
    private final PlayerDataService playerDataService;
    private final WeatherPacketService weatherPacketService;

    public void start(int intervalTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    private void tick() {
        // Snapshot active cells to avoid issues if cells are added or removed mid-tick
        Map<UUID, WeatherCell> cellMap = new HashMap<>();
        for (WeatherCell cell : cellManager.getActiveCells()) {
            cellMap.put(cell.getId(), cell);
        }

        // Determine which cells are expiring this tick.
        // startedAt is an epoch-ms timestamp; duration is in ticks (1 tick = 50 ms).
        Set<UUID> expiring = new HashSet<>();
        long nowMs = System.currentTimeMillis();
        for (WeatherCell cell : cellMap.values()) {
            if (!cell.isIndefinite()) {
                if (nowMs - cell.getStartedAt() >= cell.getDuration() * 50L) {
                    expiring.add(cell.getId());
                }
            }
        }

        try {
            // Process each online player
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID playerUUID = player.getUniqueId();
                Set<UUID> previous = playerDataService.getCellIds(playerUUID);
                Set<UUID> current = new HashSet<>();

                for (WeatherCell cell : cellMap.values()) {
                    if (expiring.contains(cell.getId())) continue;
                    if (!player.getWorld().equals(cell.getCenter().getWorld())) continue;
                    if (player.getLocation().distance(cell.getCenter()) <= cell.getRadius()) {
                        current.add(cell.getId());
                    }
                }

                // Fire enter events for newly entered cells
                for (UUID id : current) {
                    if (!previous.contains(id)) {
                        Bukkit.getPluginManager().callEvent(
                            new PlayerEnterWeatherCellEvent(player, cellMap.get(id)));
                    }
                }

                // Fire exit events - includes cells the player left and cells that are expiring
                for (UUID id : previous) {
                    if (!current.contains(id)) {
                        WeatherCell cell = cellMap.get(id);
                        if (cell != null) {
                            Bukkit.getPluginManager().callEvent(
                                new PlayerExitWeatherCellEvent(player, cell));
                        }
                    }
                }

                playerDataService.setCellIds(playerUUID, current);

                Optional<WeatherCell> priorityCell = current.stream()
                    .map(cellMap::get)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparingInt(c -> c.getType().getPriority()));

                priorityCell.ifPresent(cell -> cell.getType().onTick(cell, player));

                // Rain level uses a sqrt curve - sky greys quickly near the edge
                // Thunder level depth is driven by the type's thunder multiplier
                float targetRain = priorityCell.map(cell -> {
                    double distance = player.getLocation().distance(cell.getCenter());
                    float distanceFactor = Math.max(0f, 1.0f - (float) (distance / cell.getRadius()));
                    return cell.getIntensity() * (float) Math.sqrt(distanceFactor) * cell.getType().getRainMultiplier();
                }).orElse(0f);

                float targetThunder = priorityCell
                    .map(cell -> targetRain * cell.getType().getThunderMultiplier())
                    .orElse(0f);

                weatherPacketService.tick(player, targetRain, targetThunder);
            }

            // Process nearby entities for each active, non-expiring cell
            for (WeatherCell cell : cellMap.values()) {
                if (expiring.contains(cell.getId())) continue;
                Location center = cell.getCenter();
                if (center.getWorld() == null) continue;

                double r = cell.getRadius();
                for (Entity entity : center.getWorld().getNearbyEntities(center, r, r, r,
                        e -> e instanceof LivingEntity && !(e instanceof Player))) {
                    if (entity.getLocation().distance(center) <= r) {
                        cell.getType().onEntityTick(cell, (LivingEntity) entity);
                    }
                }
            }

            // Move non-expiring cells by their movement vector
            for (WeatherCell cell : cellMap.values()) {
                if (expiring.contains(cell.getId())) continue;
                Location previousCenter = cell.getCenter().clone();
                cell.setCenter(cell.getCenter().clone().add(cell.getMovementVector()));
                Bukkit.getPluginManager().callEvent(new WeatherCellMoveEvent(cell, previousCenter));
            }
        } finally {
            // Expire cells - guaranteed to run even if processing throws
            // CellManager handles onEnd and WeatherCellEndEvent
            expiring.forEach(cellManager::removeCell);
        }
    }
}
