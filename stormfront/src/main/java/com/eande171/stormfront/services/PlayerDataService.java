package com.eande171.stormfront.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerDataService {

    private final Map<UUID, Set<UUID>> activeCells = new HashMap<>();

    public Set<UUID> getCellIds(UUID playerUUID) {
        return activeCells.getOrDefault(playerUUID, Collections.emptySet());
    }

    public void setCellIds(UUID playerUUID, Set<UUID> cellIds) {
        activeCells.put(playerUUID, cellIds);
    }

    public void removePlayer(UUID playerUUID) {
        activeCells.remove(playerUUID);
    }
}
