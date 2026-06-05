package com.eande171.stormfront;

import com.eande171.stormfront.services.PlayerDataService;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public class PluginListener implements Listener {

    private final PlayerDataService playerDataService;

}
