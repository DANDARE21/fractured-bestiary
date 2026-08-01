package net.dandare21.fracturedutils.command;

import com.mojang.brigadier.CommandDispatcher;
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

public class DownloadCinematicCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("downloadcinematic")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("url", StringArgumentType.greedyString())
                                        .executes(ctx -> execute(ctx, EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "url"))))))
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("url", StringArgumentType.greedyString())
                                .executes(ctx -> execute(ctx, ctx.getSource().getServer().getPlayerList().getPlayers(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "url")))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String name, String url) {
        CommandSourceStack source = ctx.getSource();
        if (targets == null || targets.isEmpty()) {
            source.sendFailure(Component.literal("No target players found for cinematic download."));
            return 0;
        }

        ServerCutsceneManager.getInstance().startCinematicDownload(source.getServer(), targets, name, url);
        source.sendSuccess(() -> Component.literal("Initiated background download of cinematic '" + name + "' for " + targets.size() + " player(s).")
                .withStyle(ChatFormatting.GREEN), true);
        return targets.size();
    }
}
