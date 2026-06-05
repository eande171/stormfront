package com.eande171.stormfront.api.events;

import com.eande171.stormfront.api.WeatherCell;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public class WeatherCellMoveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final WeatherCell cell;
    private final Location previousCenter;

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
