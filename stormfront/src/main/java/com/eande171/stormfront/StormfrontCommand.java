package com.eande171.stormfront;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class StormfrontCommand {

    private final CellManager cellManager;

    public int onTestRain(CommandContext<CommandSourceStack> ctx) {
        return spawnTestCell(ctx, "stormfront:rain");
    }

    public int onTestThunder(CommandContext<CommandSourceStack> ctx) {
        return spawnTestCell(ctx, "stormfront:thunderstorm");
    }

    public int onTestFog(CommandContext<CommandSourceStack> ctx) {
        return spawnTestCell(ctx, "stormfront:fog");
    }

    public int onTestBlizzard(CommandContext<CommandSourceStack> ctx) {
        return spawnTestCell(ctx, "stormfront:blizzard");
    }

    public int onStop(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int count = cellManager.getActiveCells().size();
        new ArrayList<>(cellManager.getActiveCells())
            .forEach(cell -> cellManager.removeCell(cell.getId()));
        sender.sendMessage("Removed " + count + " active cell(s).");
        return count;
    }

    private int spawnTestCell(CommandContext<CommandSourceStack> ctx, String typeId) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return 0;
        }

        Optional<WeatherType> type = StormfrontAPI.get().getRegistry().get(typeId);

        if (type.isEmpty()) {
            sender.sendMessage("Weather type '" + typeId + "' is not registered.");
            return 0;
        }

        WeatherCell cell = WeatherCell.builder()
            .id(UUID.randomUUID())
            .type(type.get())
            .center(player.getLocation())
            .radius(50)
            .intensity(1.0f)
            .movementVector(new Vector(0, 0, 0))
            .startedAt(System.currentTimeMillis())
            .duration(1200L)
            .build();

        cellManager.addCell(cell);

        sender.sendMessage("Spawned '" + typeId + "' at your location (radius 50, 60 seconds).");
        return 1;
    }
}
