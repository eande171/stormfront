package com.eande171.stormfront.api.engine;

import com.eande171.stormfront.api.CellManager;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.events.PlayerEnterWeatherCellEvent;
import com.eande171.stormfront.api.events.PlayerExitWeatherCellEvent;
import com.eande171.stormfront.api.events.WeatherCellMoveEvent;
import com.eande171.stormfront.api.services.PlayerDataService;
import com.eande171.stormfront.api.services.WeatherPacketService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class WeatherScheduler {

    private static final Logger LOGGER = Logger.getLogger(WeatherScheduler.class.getName());

    private final JavaPlugin plugin;
    private final CellManager cellManager;
    private final PlayerDataService playerDataService;
    private final WeatherPacketService weatherPacketService;

    public void start(int intervalTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    private void tick() {
        Map<UUID, WeatherCell> cellMap = snapshotActiveCells();
        Set<UUID> expiring = computeExpiringCells(cellMap);

        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tickPlayer(player, cellMap, expiring);
            }
            tickEntities(cellMap, expiring);
            moveCells(cellMap, expiring);
        } finally {
            // Guaranteed to run even if processing throws; CellManager handles onEnd/WeatherCellEndEvent
            expiring.forEach(cellManager::removeCell);
        }
    }

    // Snapshot active cells to avoid issues if cells are added or removed mid-tick
    private Map<UUID, WeatherCell> snapshotActiveCells() {
        Map<UUID, WeatherCell> cellMap = new HashMap<>();
        for (WeatherCell cell : cellManager.getActiveCells()) {
            cellMap.put(cell.getId(), cell);
        }
        return cellMap;
    }

    // startedAt is an epoch-ms timestamp; duration is in ticks (1 tick = 50 ms).
    private Set<UUID> computeExpiringCells(Map<UUID, WeatherCell> cellMap) {
        Set<UUID> expiring = new HashSet<>();
        long nowMs = System.currentTimeMillis();
        for (WeatherCell cell : cellMap.values()) {
            if (!cell.isIndefinite() && nowMs - cell.getStartedAt() >= cell.getDuration() * 50L) {
                expiring.add(cell.getId());
            }
        }
        return expiring;
    }

    private void tickPlayer(Player player, Map<UUID, WeatherCell> cellMap, Set<UUID> expiring) {
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

        Set<UUID> cancelledEnters = new HashSet<>();
        for (UUID id : current) {
            if (!previous.contains(id)) {
                PlayerEnterWeatherCellEvent event = new PlayerEnterWeatherCellEvent(player, cellMap.get(id));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) cancelledEnters.add(id);
            }
        }
        current.removeAll(cancelledEnters);

        // Fire exit events - includes cells the player left and cells that are expiring
        for (UUID id : previous) {
            if (!current.contains(id)) {
                WeatherCell cell = cellMap.get(id);
                if (cell != null) {
                    PlayerExitWeatherCellEvent event = new PlayerExitWeatherCellEvent(player, cell);
                    Bukkit.getPluginManager().callEvent(event);
                    // Cancelling keeps the player tracked one more tick; not honored for expiring cells, which are removed regardless
                    if (event.isCancelled() && !expiring.contains(id)) {
                        current.add(id);
                    }
                }
            }
        }

        // Must run before the finally block, or removeCell fires a duplicate exit event for expiring cells
        playerDataService.setCellIds(playerUUID, current);

        Optional<WeatherCell> priorityCell = current.stream()
            .map(cellMap::get)
            .filter(Objects::nonNull)
            .max(Comparator.comparingInt(c -> c.getType().getPriority()));

        priorityCell.ifPresent(cell -> WeatherUtils.safeRun(LOGGER,
            "Weather type " + cell.getType().getId() + " threw in onTick for " + player.getName(),
            () -> cell.getType().onTick(cell, player)));

        // Sqrt curve so the sky greys quickly near the cell's edge
        float targetRain = priorityCell.map(cell -> {
            float distanceFactor = WeatherUtils.distanceFactor(cell, player.getLocation());
            return cell.getIntensity() * (float) Math.sqrt(distanceFactor) * cell.getType().getRainMultiplier();
        }).orElse(0f);

        float targetThunder = priorityCell
            .map(cell -> targetRain * cell.getType().getThunderMultiplier())
            .orElse(0f);

        weatherPacketService.tick(player, targetRain, targetThunder);
    }

    private void tickEntities(Map<UUID, WeatherCell> cellMap, Set<UUID> expiring) {
        for (WeatherCell cell : cellMap.values()) {
            if (expiring.contains(cell.getId())) continue;
            Location center = cell.getCenter();
            if (center.getWorld() == null) continue;

            double r = cell.getRadius();
            for (Entity entity : center.getWorld().getNearbyEntities(center, r, r, r,
                    e -> e instanceof LivingEntity && !(e instanceof Player))) {
                if (entity.getLocation().distance(center) <= r) {
                    WeatherUtils.safeRun(LOGGER, "Weather type " + cell.getType().getId() + " threw in onEntityTick",
                        () -> cell.getType().onEntityTick(cell, (LivingEntity) entity));
                }
            }
        }
    }

    private void moveCells(Map<UUID, WeatherCell> cellMap, Set<UUID> expiring) {
        for (WeatherCell cell : cellMap.values()) {
            if (expiring.contains(cell.getId())) continue;
            Location previousCenter = cell.getCenter().clone();
            Location newCenter = previousCenter.clone().add(cell.getMovementVector());
            WeatherCellMoveEvent event = new WeatherCellMoveEvent(cell, previousCenter);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                cell.setCenter(newCenter);
            }
        }
    }
}
