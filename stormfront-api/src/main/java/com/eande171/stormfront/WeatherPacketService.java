package com.eande171.stormfront;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WeatherPacketService {

    private static final float SPEED_UP   = 0.04f;  // ~5 seconds to full
    private static final float SPEED_DOWN = 0.013f; // ~15 seconds to clear
    private static final float SNAP_THRESHOLD = 0.01f;

    private final Map<UUID, Float> rainLevels    = new HashMap<>();
    private final Map<UUID, Float> thunderLevels = new HashMap<>();

    public void tick(Player player, float targetRain, float targetThunder) {
        UUID uuid = player.getUniqueId();

        float currentRain    = rainLevels.getOrDefault(uuid, 0f);
        float currentThunder = thunderLevels.getOrDefault(uuid, 0f);

        float newRain    = step(currentRain, targetRain);
        float newThunder = step(currentThunder, targetThunder);

        if (newRain != currentRain) {
            if (currentRain == 0 && newRain > 0) sendBeginRaining(player);
            rainLevels.put(uuid, newRain);
            sendRainLevel(player, newRain);
            if (newRain <= 0) sendEndRaining(player);
        }

        if (newThunder != currentThunder) {
            thunderLevels.put(uuid, newThunder);
            sendThunderLevel(player, newThunder);
        }
    }

    public void reset(Player player) {
        rainLevels.remove(player.getUniqueId());
        thunderLevels.remove(player.getUniqueId());
        sendEndRaining(player);
        sendThunderLevel(player, 0f);
    }

    private float step(float current, float target) {
        if (Math.abs(current - target) < SNAP_THRESHOLD) return target;
        if (current < target) return Math.min(current + SPEED_UP, target);
        return Math.max(current - SPEED_DOWN, target);
    }

    private void sendBeginRaining(Player player) {
        send(player, WrapperPlayServerChangeGameState.Reason.BEGIN_RAINING, 0f);
    }

    private void sendEndRaining(Player player) {
        send(player, WrapperPlayServerChangeGameState.Reason.END_RAINING, 0f);
    }

    private void sendRainLevel(Player player, float level) {
        send(player, WrapperPlayServerChangeGameState.Reason.RAIN_LEVEL_CHANGE, level);
    }

    private void sendThunderLevel(Player player, float level) {
        send(player, WrapperPlayServerChangeGameState.Reason.THUNDER_LEVEL_CHANGE, level);
    }

    private void send(Player player, WrapperPlayServerChangeGameState.Reason reason, float value) {
        WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(reason, value);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
