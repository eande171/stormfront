package com.eande171.stormfront;

import com.eande171.stormfront.services.ConfigService;
import com.eande171.stormfront.services.MessageService;
import com.eande171.stormfront.services.PluginService;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
public class PluginCommand {

    private final ConfigService configService;
    private final PluginService pluginService;
    private final MessageService messageService;

    public int onOn(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (configService.isActive()) {
            sender.sendMessage(messageService.alreadyOn());
        } else {
            pluginService.enable();
            sender.sendMessage(messageService.nowOn());
        }
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    public int onOff(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!configService.isActive()) {
            sender.sendMessage(messageService.alreadyOff());
        } else {
            pluginService.disable();
            sender.sendMessage(messageService.nowOff());
        }
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
