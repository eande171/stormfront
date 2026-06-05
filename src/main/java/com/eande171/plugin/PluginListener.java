package com.eande171.plugin;

import com.eande171.plugin.services.PlayerDataService;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public class PluginListener implements Listener {

    private final PlayerDataService playerDataService;

}
