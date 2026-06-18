package com.eande171.stormfront;

import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.events.PlayerEnterWeatherCellEvent;
import com.eande171.stormfront.api.events.PlayerExitWeatherCellEvent;
import com.eande171.stormfront.services.PlayerDataService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class EngineListener implements Listener {

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
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Reset walk speed in case it was persisted from a previous session
        event.getPlayer().setWalkSpeed(WeatherUtils.NORMAL_WALK_SPEED);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Set<UUID> trackedIds = playerDataService.getCellIds(player.getUniqueId());
        for (WeatherCell cell : cellManager.getActiveCells()) {
            if (trackedIds.contains(cell.getId())) {
                cell.getType().onPlayerExit(player);
            }
        }
        playerDataService.removePlayer(player.getUniqueId());
        weatherPacketService.reset(player);
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        // Covered entities are not in sunlight - no need to intervene
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
