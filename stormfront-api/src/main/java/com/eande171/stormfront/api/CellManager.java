package com.eande171.stormfront.api;

import java.util.Collection;
import java.util.UUID;

public interface CellManager {

    void addCell(WeatherCell cell);

    void removeCell(UUID id);

    Collection<WeatherCell> getActiveCells();
}
