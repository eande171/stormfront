package com.eande171.stormfront;

import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.events.PlayerExitWeatherCellEvent;
import com.eande171.stormfront.api.events.WeatherCellEndEvent;
import com.eande171.stormfront.api.events.WeatherCellStartEvent;
import com.eande171.stormfront.services.PlayerDataService;
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

@RequiredArgsConstructor
public class CellManager {

    private final PluginMain plugin;
    private final PlayerDataService playerDataService;
    private final Map<UUID, WeatherCell> activeCells = new HashMap<>();

    public void addCell(WeatherCell cell) {
        activeCells.put(cell.getId(), cell);
        cell.getType().onStart(cell);
        Bukkit.getPluginManager().callEvent(new WeatherCellStartEvent(cell));
    }

    public void removeCell(UUID id) {
        WeatherCell cell = activeCells.remove(id);
        if (cell == null) return;
        // Fire exit events for any player currently tracked as being inside this cell.
        // The scheduler's diff logic can't do this because by its next tick the cell is gone.
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<UUID> tracked = playerDataService.getCellIds(player.getUniqueId());
            if (tracked.contains(id)) {
                Bukkit.getPluginManager().callEvent(new PlayerExitWeatherCellEvent(player, cell));
                Set<UUID> updated = new HashSet<>(tracked);
                updated.remove(id);
                playerDataService.setCellIds(player.getUniqueId(), updated);
            }
        }
        cell.getType().onEnd(cell);
        Bukkit.getPluginManager().callEvent(new WeatherCellEndEvent(cell));
    }

    public Collection<WeatherCell> getActiveCells() {
        return Collections.unmodifiableCollection(activeCells.values());
    }
}
