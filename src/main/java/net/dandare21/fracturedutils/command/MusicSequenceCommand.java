package net.dandare21.fracturedutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.dandare21.fracturedutils.sound.sequence.MusicSequenceManager;
import net.dandare21.fracturedutils.sound.event.EventAudioManager;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.S2CSendMusicSequenceDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MusicSequenceCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MUSIC_SEQUENCES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(MusicSequenceManager.getInstance().getSequenceFileNames(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("musicsequence")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("run")
                        .then(Commands.argument("file_name", StringArgumentType.string())
                                .suggests(SUGGEST_MUSIC_SEQUENCES)
                                .executes(ctx -> executeRun(ctx, StringArgumentType.getString(ctx, "file_name"), null))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> executeRun(ctx, StringArgumentType.getString(ctx, "file_name"), EntityArgument.getPlayers(ctx, "targets"))))))
                .then(Commands.literal("stop")
                        .executes(MusicSequenceCommand::executeStop))
                .then(Commands.literal("cancel")
                        .executes(MusicSequenceCommand::executeStop))
                .then(Commands.literal("list")
                        .executes(MusicSequenceCommand::executeList))
                .then(Commands.literal("ui")
                        .executes(MusicSequenceCommand::openUi))
        );
    }

    private static int executeRun(CommandContext<CommandSourceStack> ctx, String fileName, Collection<ServerPlayer> targets) {
        CommandSourceStack source = ctx.getSource();
        boolean started = MusicSequenceManager.getInstance().startSequence(fileName, targets);
        if (started) {
            int targetCount = targets != null ? targets.size() : source.getServer().getPlayerList().getPlayers().size();
            source.sendSuccess(() -> Component.literal("▶ Started music sequence '" + fileName + "' for " + targetCount + " player(s).")
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("❌ Failed to start music sequence '" + fileName + "'. File missing or invalid JSON.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int executeStop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MusicSequenceManager.getInstance().stopAllSequences(source.getServer());
        source.sendSuccess(() -> Component.literal("⏹ Stopped all active music sequences.")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> files = MusicSequenceManager.getInstance().getSequenceFileNames();
        if (files.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No music sequence files found in config/music_sequences.")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.literal("Available music sequences (" + files.size() + "): " + String.join(", ", files))
                    .withStyle(ChatFormatting.AQUA), false);
        }
        return files.size();
    }

    private static int openUi(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            Map<String, String> files = MusicSequenceManager.getInstance().getAllSequenceFiles();
            List<String> trackSuggestions = EventAudioManager.getInstance().getAvailableTrackSuggestions();
            ModMessages.sendToPlayer(new S2CSendMusicSequenceDataPacket(files, trackSuggestions), player);
            return 1;
        } else {
            source.sendFailure(Component.literal("This command must be executed by a player."));
            return 0;
        }
    }
}
