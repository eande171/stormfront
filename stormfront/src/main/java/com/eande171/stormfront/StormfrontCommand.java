package com.eande171.stormfront;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class StormfrontCommand {

    private static final int DEFAULT_RADIUS = 50;
    private static final float DEFAULT_INTENSITY = 1.0f;
    private static final int DEFAULT_DURATION_SECONDS = -1; // indefinite

    private final CellManager cellManager;

    public int onSpawn(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return 0;
        }

        String typeId = ctx.getArgument("type", NamespacedKey.class).toString();
        int radius = getOrDefault(ctx, "radius", Integer.class, DEFAULT_RADIUS);
        float intensity = getOrDefault(ctx, "intensity", Float.class, DEFAULT_INTENSITY);
        int durationSeconds = getOrDefault(ctx, "duration", Integer.class, DEFAULT_DURATION_SECONDS);
        long durationTicks = durationSeconds == -1 ? -1L : durationSeconds * 20L;

        Optional<WeatherType> type = StormfrontAPI.get().getRegistry().get(typeId);
        if (type.isEmpty()) {
            sender.sendMessage("Unknown weather type: '" + typeId + "'.");
            return 0;
        }

        WeatherCell cell = WeatherCell.builder()
            .id(UUID.randomUUID())
            .type(type.get())
            .center(player.getLocation())
            .radius(radius)
            .intensity(intensity)
            .movementVector(new Vector(0, 0, 0))
            .startedAt(System.currentTimeMillis())
            .duration(durationTicks)
            .build();

        cellManager.addCell(cell);

        String durationStr = durationTicks == -1 ? "indefinite" : durationSeconds + "s";
        sender.sendMessage("Spawned '" + typeId + "' (radius=" + radius + ", intensity=" + intensity + ", duration=" + durationStr + ").");
        return 1;
    }

    public int onStop(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int count = cellManager.getActiveCells().size();
        new ArrayList<>(cellManager.getActiveCells())
            .forEach(cell -> cellManager.removeCell(cell.getId()));
        sender.sendMessage("Removed " + count + " active cell(s).");
        return count;
    }

    public int onList(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Collection<WeatherCell> cells = cellManager.getActiveCells();

        if (cells.isEmpty()) {
            sender.sendMessage("No active weather cells.");
            return 0;
        }

        long now = System.currentTimeMillis();
        sender.sendMessage("Active cells (" + cells.size() + "):");

        for (WeatherCell cell : cells) {
            Location c = cell.getCenter();
            String pos = (int) c.getX() + ", " + (int) c.getY() + ", " + (int) c.getZ();

            String expiry;
            if (cell.isIndefinite()) {
                expiry = "indefinite";
            } else {
                long remainingMs = (cell.getStartedAt() + cell.getDuration() * 50L) - now;
                long remainingSecs = Math.max(0, remainingMs / 1000);
                expiry = remainingSecs + "s remaining";
            }

            sender.sendMessage("  " + cell.getType().getId()
                + " | pos: " + pos
                + " | radius: " + cell.getRadius()
                + " | intensity: " + cell.getIntensity()
                + " | " + expiry);
        }

        return cells.size();
    }

    private <T> T getOrDefault(CommandContext<CommandSourceStack> ctx, String name, Class<T> type, T defaultValue) {
        try {
            return ctx.getArgument(name, type);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
