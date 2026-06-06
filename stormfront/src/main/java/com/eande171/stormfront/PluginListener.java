package com.eande171.stormfront;

import com.eande171.stormfront.WeatherUtils;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.events.PlayerEnterWeatherCellEvent;
import com.eande171.stormfront.api.events.PlayerExitWeatherCellEvent;
import com.eande171.stormfront.services.PlayerDataService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class PluginListener implements Listener {

    private final PlayerDataService playerDataService;
    private final WeatherPacketService weatherPacketService;
    private final CellManager cellManager;

    @EventHandler
    public void onPlayerEnterCell(PlayerEnterWeatherCellEvent event) {
        event.getCell().getType().onPlayerEnter(event.getPlayer());
    }

    @EventHandler
    public void onPlayerExitCell(PlayerExitWeatherCellEvent event) {
        event.getCell().getType().onPlayerExit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataService.removePlayer(event.getPlayer().getUniqueId());
        weatherPacketService.reset(event.getPlayer());
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        // Covered entities aren't in sunlight - no need to intervene
        if (!WeatherUtils.isExposed(entity.getLocation())) return;
        for (WeatherCell cell : cellManager.getActiveCells()) {
            if (!entity.getWorld().equals(cell.getCenter().getWorld())) continue;
            if (entity.getLocation().distance(cell.getCenter()) <= cell.getRadius()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
