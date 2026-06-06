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

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class StormfrontCommand {

    private final CellManager cellManager;

    public int onTest(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return 0;
        }

        Optional<WeatherType> type = StormfrontAPI.get().getRegistry().get("stormfront:rain");

        if (type.isEmpty()) {
            sender.sendMessage("RainfrontType is not registered.");
            return 0;
        }

        WeatherCell cell = WeatherCell.builder()
            .id(UUID.randomUUID())
            .type(type.get())
            .center(player.getLocation())
            .radius(50)
            .intensity(1.0f)
            .movementVector(new Vector(0, 0, 0))
            .startedAt(player.getWorld().getFullTime())
            .duration(1200L)
            .build();

        cellManager.addCell(cell);

        sender.sendMessage("Spawned a Rainfront cell at your location (radius 50, 60 seconds).");
        return 1;
    }
}
