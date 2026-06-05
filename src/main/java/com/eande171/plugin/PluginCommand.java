package com.eande171.plugin;

import com.eande171.plugin.services.ConfigService;
import com.eande171.plugin.services.MessageService;
import com.eande171.plugin.services.PluginService;
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

    // Arguments
    /*public int onSetGrace(CommandContext<CommandSourceStack> ctx) {
        long seconds = com.mojang.brigadier.arguments.LongArgumentType.getLong(ctx, "seconds");

        configService.setGracePeriod(seconds * 1000L);
        ctx.getSource().getSender().sendMessage(messageService.graceSet(seconds));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    public int onSetDamage(CommandContext<CommandSourceStack> ctx) {
        double value = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "value");

        configService.setDamage(value);
        ctx.getSource().getSender().sendMessage(messageService.damageSet(value));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }*/
}
