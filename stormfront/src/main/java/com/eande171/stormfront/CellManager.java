package com.eande171.stormfront;

import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.events.WeatherCellEndEvent;
import com.eande171.stormfront.api.events.WeatherCellStartEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class CellManager {

    private final PluginMain plugin;
    private final Map<UUID, WeatherCell> activeCells = new HashMap<>();

    public void addCell(WeatherCell cell) {
        activeCells.put(cell.getId(), cell);
        cell.getType().onStart(cell);
        Bukkit.getPluginManager().callEvent(new WeatherCellStartEvent(cell));
    }

    public void removeCell(UUID id) {
        WeatherCell cell = activeCells.remove(id);
        if (cell == null) return;
        cell.getType().onEnd(cell);
        Bukkit.getPluginManager().callEvent(new WeatherCellEndEvent(cell));
    }

    public Collection<WeatherCell> getActiveCells() {
        return Collections.unmodifiableCollection(activeCells.values());
    }
}
