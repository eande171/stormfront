package com.eande171.stormfront.api.events;

import com.eande171.stormfront.api.WeatherCell;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public class WeatherCellEndEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final WeatherCell cell;

    // Not currently honored - the cell is already removed from CellManager by the time this fires
    private boolean cancelled;

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
