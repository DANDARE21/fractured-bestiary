package net.dandare21.fracturedutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.dandare21.fracturedutils.waitingroom.WaitingRoomManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WaitingRoomCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("waitingroom")
                .then(Commands.literal("start")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> startWaitingRoom(ctx, "Starting Soon..."))
                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                .executes(ctx -> startWaitingRoom(ctx, StringArgumentType.getString(ctx, "title")))))
                .then(Commands.literal("stop")
                        .requires(s -> s.hasPermission(2))
                        .executes(WaitingRoomCommands::stopWaitingRoom))
                .then(Commands.literal("join")
                        .executes(WaitingRoomCommands::joinWaitingRoom))
                .then(Commands.literal("leave")
                        .requires(s -> s.hasPermission(2))
                        .executes(WaitingRoomCommands::leaveWaitingRoom))
                .then(Commands.literal("status")
                        .executes(WaitingRoomCommands::showStatus))
        );

        dispatcher.register(Commands.literal("wr")
                .redirect(dispatcher.getRoot().getChild("waitingroom")));
    }

    private static int startWaitingRoom(CommandContext<CommandSourceStack> ctx, String title) {
        WaitingRoomManager.getInstance().start(ctx.getSource().getServer(), title);
        ctx.getSource().sendSuccess(() -> Component.literal("Event waiting room started with title: " + title).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int stopWaitingRoom(CommandContext<CommandSourceStack> ctx) {
        WaitingRoomManager.getInstance().stop(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("Event waiting room stopped.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int joinWaitingRoom(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!WaitingRoomManager.getInstance().isActive()) {
            src.sendFailure(Component.literal("No event waiting room is currently active."));
            return 0;
        }
        ServerPlayer player = src.getPlayer();
        if (player != null) {
            WaitingRoomManager.getInstance().joinPlayer(player);
            src.sendSuccess(() -> Component.literal("You joined the waiting room.").withStyle(ChatFormatting.GREEN), false);
        }
        return 1;
    }

    private static int leaveWaitingRoom(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!WaitingRoomManager.getInstance().isActive()) {
            src.sendFailure(Component.literal("No event waiting room is currently active."));
            return 0;
        }
        ServerPlayer player = src.getPlayer();
        if (player != null) {
            WaitingRoomManager.getInstance().leavePlayer(player);
            src.sendSuccess(() -> Component.literal("You left the waiting room.").withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        WaitingRoomManager mgr = WaitingRoomManager.getInstance();
        if (!mgr.isActive()) {
            src.sendSuccess(() -> Component.literal("Waiting Room Status: INACTIVE").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        int count = mgr.getJoinedPlayers().size();
        src.sendSuccess(() -> Component.literal("Waiting Room Status: ACTIVE ('" + mgr.getRoomTitle() + "') - " + count + " player(s) joined.")
                .withStyle(ChatFormatting.GOLD), false);

        return 1;
    }
}
