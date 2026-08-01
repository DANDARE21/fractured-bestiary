package net.dandare21.fracturedutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.dandare21.fracturedutils.cutscene.ServerCutsceneManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class PlayCinematicCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playcinematic")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("url", StringArgumentType.string())
                                .executes(ctx -> execute(ctx, EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "url"), false, true))
                                .then(Commands.argument("allowSkip", BoolArgumentType.bool())
                                        .executes(ctx -> execute(ctx, EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "url"), BoolArgumentType.getBool(ctx, "allowSkip"), true))
                                        .then(Commands.argument("deleteAfter", BoolArgumentType.bool())
                                                .executes(ctx -> execute(ctx, EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "url"), BoolArgumentType.getBool(ctx, "allowSkip"), BoolArgumentType.getBool(ctx, "deleteAfter"))))))
                        .then(Commands.argument("url", StringArgumentType.greedyString())
                                .executes(ctx -> execute(ctx, EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "url"), false, true))))
                .then(Commands.argument("url", StringArgumentType.string())
                        .executes(ctx -> execute(ctx, ctx.getSource().getServer().getPlayerList().getPlayers(), StringArgumentType.getString(ctx, "url"), false, true))
                        .then(Commands.argument("allowSkip", BoolArgumentType.bool())
                                .executes(ctx -> execute(ctx, ctx.getSource().getServer().getPlayerList().getPlayers(), StringArgumentType.getString(ctx, "url"), BoolArgumentType.getBool(ctx, "allowSkip"), true))
                                .then(Commands.argument("deleteAfter", BoolArgumentType.bool())
                                        .executes(ctx -> execute(ctx, ctx.getSource().getServer().getPlayerList().getPlayers(), StringArgumentType.getString(ctx, "url"), BoolArgumentType.getBool(ctx, "allowSkip"), BoolArgumentType.getBool(ctx, "deleteAfter"))))))
                .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(ctx -> execute(ctx, ctx.getSource().getServer().getPlayerList().getPlayers(), StringArgumentType.getString(ctx, "url"), false, true)))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String url, boolean allowSkip, boolean deleteAfter) {
        CommandSourceStack source = ctx.getSource();
        if (targets == null || targets.isEmpty()) {
            source.sendFailure(Component.literal("No target players found for cinematic playback."));
            return 0;
        }

        ServerCutsceneManager.getInstance().startCinematicPreparation(source.getServer(), targets, url, allowSkip, deleteAfter);
        source.sendSuccess(() -> Component.literal("Initiated cinematic playback preparation for " + targets.size() + " player(s) (allowSkip=" + allowSkip + ", deleteAfter=" + deleteAfter + ").")
                .withStyle(ChatFormatting.GREEN), true);
        return targets.size();
    }
}
