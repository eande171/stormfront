package com.eande171.stormfront;

import com.eande171.stormfront.api.events.PlayerExitWeatherCellEvent;
import com.eande171.stormfront.services.PlayerDataService;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class PluginListener implements Listener {

    private final PlayerDataService playerDataService;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataService.removePlayer(event.getPlayer().getUniqueId());
        event.getPlayer().resetPlayerWeather();
    }

    @EventHandler
    public void onPlayerExitCell(PlayerExitWeatherCellEvent event) {
        event.getPlayer().resetPlayerWeather();
    }
}
