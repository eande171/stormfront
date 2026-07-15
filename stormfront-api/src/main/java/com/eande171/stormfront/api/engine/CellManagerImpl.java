package com.eande171.stormfront.api.engine;

import com.eande171.stormfront.api.CellManager;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.events.PlayerExitWeatherCellEvent;
import com.eande171.stormfront.api.events.WeatherCellEndEvent;
import com.eande171.stormfront.api.events.WeatherCellStartEvent;
import com.eande171.stormfront.api.services.PlayerDataService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class CellManagerImpl implements CellManager {

    private static final Logger LOGGER = Logger.getLogger(CellManagerImpl.class.getName());

    private final PlayerDataService playerDataService;
    private final Map<UUID, WeatherCell> activeCells = new HashMap<>();

    @Override
    public void addCell(WeatherCell cell) {
        if (cell.getRadius() <= 0) {
            throw new IllegalArgumentException("WeatherCell radius must be positive, got " + cell.getRadius());
        }
        if (cell.getDuration() < -1) {
            throw new IllegalArgumentException("WeatherCell duration must be -1 (indefinite) or >= 0, got " + cell.getDuration());
        }

        WeatherCellStartEvent event = new WeatherCellStartEvent(cell);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        activeCells.put(cell.getId(), cell);
        WeatherUtils.safeRun(LOGGER, "Weather type " + cell.getType().getId() + " threw in onStart",
            () -> cell.getType().onStart(cell));
    }

    @Override
    public void removeCell(UUID id) {
        WeatherCell cell = activeCells.remove(id);
        if (cell == null) return;
        // Fire exit events here since the scheduler's diff logic won't catch it once the cell is gone; cancelling is a no-op, the cell is already removed
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<UUID> tracked = playerDataService.getCellIds(player.getUniqueId());
            if (tracked.contains(id)) {
                Bukkit.getPluginManager().callEvent(new PlayerExitWeatherCellEvent(player, cell));
                Set<UUID> updated = new HashSet<>(tracked);
                updated.remove(id);
                playerDataService.setCellIds(player.getUniqueId(), updated);
            }
        }
        WeatherUtils.safeRun(LOGGER, "Weather type " + cell.getType().getId() + " threw in onEnd",
            () -> cell.getType().onEnd(cell));
        Bukkit.getPluginManager().callEvent(new WeatherCellEndEvent(cell));
    }

    @Override
    public Collection<WeatherCell> getActiveCells() {
        return Collections.unmodifiableCollection(activeCells.values());
    }
}
