package com.eande171.stormfront;

import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.events.PlayerEnterWeatherCellEvent;
import com.eande171.stormfront.api.events.PlayerExitWeatherCellEvent;
import com.eande171.stormfront.api.events.WeatherCellMoveEvent;
import com.eande171.stormfront.services.PlayerDataService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@RequiredArgsConstructor
public class WeatherScheduler {

    private final PluginMain plugin;
    private final CellManager cellManager;
    private final PlayerDataService playerDataService;

    public void start(int intervalTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    private void tick() {
        // Snapshot active cells to avoid issues if cells are added or removed mid-tick
        Map<UUID, WeatherCell> cellMap = new HashMap<>();
        for (WeatherCell cell : cellManager.getActiveCells()) {
            cellMap.put(cell.getId(), cell);
        }

        // Collect expired cells and remove them from the working snapshot
        List<UUID> toExpire = new ArrayList<>();
        for (WeatherCell cell : cellMap.values()) {
            if (!cell.isIndefinite()) {
                long currentTick = cell.getCenter().getWorld().getFullTime();
                if (currentTick - cell.getStartedAt() >= cell.getDuration()) {
                    toExpire.add(cell.getId());
                }
            }
        }
        toExpire.forEach(cellMap::remove);

        // Process each online player
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            Set<UUID> previous = playerDataService.getCellIds(playerUUID);
            Set<UUID> current = new HashSet<>();

            for (WeatherCell cell : cellMap.values()) {
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

            // Fire exit events for cells the player has left
            for (UUID id : previous) {
                if (!current.contains(id) && cellMap.containsKey(id)) {
                    Bukkit.getPluginManager().callEvent(
                        new PlayerExitWeatherCellEvent(player, cellMap.get(id)));
                }
            }

            playerDataService.setCellIds(playerUUID, current);

            // Call onTick for the highest priority cell the player is currently in
            current.stream()
                .map(cellMap::get)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(c -> c.getType().getPriority()))
                .ifPresent(cell -> cell.getType().onTick(cell, player));
        }

        // Move each cell by its movement vector
        for (WeatherCell cell : cellMap.values()) {
            Location previousCenter = cell.getCenter().clone();
            cell.setCenter(cell.getCenter().clone().add(cell.getMovementVector()));
            Bukkit.getPluginManager().callEvent(new WeatherCellMoveEvent(cell, previousCenter));
        }

        // Expire cells — CellManager handles onEnd and WeatherCellEndEvent
        toExpire.forEach(cellManager::removeCell);
    }
}
