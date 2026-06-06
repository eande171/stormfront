package com.eande171.stormfront;

import com.eande171.stormfront.api.WeatherCell;
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataService.removePlayer(event.getPlayer().getUniqueId());
        weatherPacketService.reset(event.getPlayer());
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        for (WeatherCell cell : cellManager.getActiveCells()) {
            if (!entity.getWorld().equals(cell.getCenter().getWorld())) continue;
            if (entity.getLocation().distance(cell.getCenter()) <= cell.getRadius()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
