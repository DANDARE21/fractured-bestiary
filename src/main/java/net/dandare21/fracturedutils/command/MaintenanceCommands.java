package net.dandare21.fracturedutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.dandare21.fracturedutils.maintenance.MaintenanceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class MaintenanceCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maintenance")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(ctx -> startMaintenance(ctx, "Server is currently under maintenance."))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> startMaintenance(ctx, StringArgumentType.getString(ctx, "reason")))))
                .then(Commands.literal("enable")
                        .executes(ctx -> startMaintenance(ctx, "Server is currently under maintenance."))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> startMaintenance(ctx, StringArgumentType.getString(ctx, "reason")))))
                .then(Commands.literal("stop")
                        .executes(MaintenanceCommands::stopMaintenance))
                .then(Commands.literal("disable")
                        .executes(MaintenanceCommands::stopMaintenance))
                .then(Commands.literal("status")
                        .executes(MaintenanceCommands::showStatus))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> startMaintenance(ctx, StringArgumentType.getString(ctx, "reason"))))
        );

        dispatcher.register(Commands.literal("maint")
                .redirect(dispatcher.getRoot().getChild("maintenance")));
    }

    private static int startMaintenance(CommandContext<CommandSourceStack> ctx, String reason) {
        CommandSourceStack src = ctx.getSource();
        int kicked = MaintenanceManager.getInstance().startMaintenance(src.getServer(), reason);
        src.sendSuccess(() -> Component.literal("Maintenance mode ACTIVATED. Reason: '" + reason + "'. Kicked " + kicked + " non-OP player(s).")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        return 1;
    }

    private static int stopMaintenance(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        MaintenanceManager.getInstance().stopMaintenance(src.getServer());
        src.sendSuccess(() -> Component.literal("Maintenance mode DEACTIVATED. Non-OP players can now join.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        MaintenanceManager mgr = MaintenanceManager.getInstance();
        boolean active = mgr.isMaintenanceActive(src.getServer());
        if (active) {
            String reason = mgr.getMaintenanceReason(src.getServer());
            src.sendSuccess(() -> Component.literal("Maintenance Mode: ACTIVE ('" + reason + "')")
                    .withStyle(ChatFormatting.RED), false);
        } else {
            src.sendSuccess(() -> Component.literal("Maintenance Mode: INACTIVE")
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return 1;
    }
}
