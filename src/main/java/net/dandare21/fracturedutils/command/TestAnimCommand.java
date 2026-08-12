package net.dandare21.fracturedutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.TestAnimPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TestAnimCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testanim")
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ModMessages.sendToPlayer(new TestAnimPacket(name), player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Triggered test animation '" + name + "' on client"), false);
                    return 1;
                })
            )
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ModMessages.sendToPlayer(new TestAnimPacket("startDown"), player);
                ctx.getSource().sendSuccess(() -> Component.literal("Triggered default test animation 'startDown' on client"), false);
                return 1;
            })
        );
    }
}
