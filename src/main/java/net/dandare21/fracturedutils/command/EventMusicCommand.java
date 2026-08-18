package net.dandare21.fracturedutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.dandare21.fracturedutils.network.packet.S2CPlayEventAudioPacket.PlaybackMode;
import net.dandare21.fracturedutils.sound.ModSoundSources;
import net.dandare21.fracturedutils.sound.event.EventAudioManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class EventMusicCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eventmusic")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("play")
                        .then(Commands.argument("track", StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(EventAudioManager.getInstance().getAvailableTrackSuggestions(), builder))
                                .executes(ctx -> playTrack(ctx.getSource(), StringArgumentType.getString(ctx, "track"), "eventmusic", PlaybackMode.SERVER_CONTROLLED, false, 1.0f, 1.0f, 1000, 500))
                                .then(Commands.argument("category", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("eventmusic", "eventambience"), builder))
                                        .executes(ctx -> playTrack(ctx.getSource(), StringArgumentType.getString(ctx, "track"), StringArgumentType.getString(ctx, "category"), PlaybackMode.SERVER_CONTROLLED, false, 1.0f, 1.0f, 1000, 500))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("server_controlled", "fire_and_forget"), builder))
                                                .executes(ctx -> playTrack(ctx.getSource(), StringArgumentType.getString(ctx, "track"), StringArgumentType.getString(ctx, "category"), parseMode(StringArgumentType.getString(ctx, "mode")), false, 1.0f, 1.0f, 1000, 500))
                                                .then(Commands.argument("looping", BoolArgumentType.bool())
                                                        .executes(ctx -> playTrack(ctx.getSource(), StringArgumentType.getString(ctx, "track"), StringArgumentType.getString(ctx, "category"), parseMode(StringArgumentType.getString(ctx, "mode")), BoolArgumentType.getBool(ctx, "looping"), 1.0f, 1.0f, 1000, 500))
                                                        .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 10.0f))
                                                                .executes(ctx -> playTrack(ctx.getSource(), StringArgumentType.getString(ctx, "track"), StringArgumentType.getString(ctx, "category"), parseMode(StringArgumentType.getString(ctx, "mode")), BoolArgumentType.getBool(ctx, "looping"), FloatArgumentType.getFloat(ctx, "volume"), 1.0f, 1000, 500))
                                                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.1f, 2.0f))
                                                                        .executes(ctx -> playTrack(ctx.getSource(), StringArgumentType.getString(ctx, "track"), StringArgumentType.getString(ctx, "category"), parseMode(StringArgumentType.getString(ctx, "mode")), BoolArgumentType.getBool(ctx, "looping"), FloatArgumentType.getFloat(ctx, "volume"), FloatArgumentType.getFloat(ctx, "pitch"), 1000, 500))
                                                                        .then(Commands.argument("fadeDurationMs", IntegerArgumentType.integer(0, 60000))
                                                                                .executes(ctx -> playTrack(ctx.getSource(), StringArgumentType.getString(ctx, "track"), StringArgumentType.getString(ctx, "category"), parseMode(StringArgumentType.getString(ctx, "mode")), BoolArgumentType.getBool(ctx, "looping"), FloatArgumentType.getFloat(ctx, "volume"), FloatArgumentType.getFloat(ctx, "pitch"), IntegerArgumentType.getInteger(ctx, "fadeDurationMs"), 500))
                                                                                .then(Commands.argument("syncThresholdMs", IntegerArgumentType.integer(50, 10000))
                                                                                        .executes(ctx -> playTrack(ctx.getSource(), StringArgumentType.getString(ctx, "track"), StringArgumentType.getString(ctx, "category"), parseMode(StringArgumentType.getString(ctx, "mode")), BoolArgumentType.getBool(ctx, "looping"), FloatArgumentType.getFloat(ctx, "volume"), FloatArgumentType.getFloat(ctx, "pitch"), IntegerArgumentType.getInteger(ctx, "fadeDurationMs"), IntegerArgumentType.getInteger(ctx, "syncThresholdMs")))
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("stop")
                        .executes(ctx -> stopAudio(ctx.getSource(), 1000))
                        .then(Commands.argument("fadeDurationMs", IntegerArgumentType.integer(0, 60000))
                                .executes(ctx -> stopAudio(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "fadeDurationMs")))
                        )
                )
                .then(Commands.literal("reload")
                        .executes(ctx -> reloadPacks(ctx.getSource()))
                )
                .then(Commands.literal("status")
                        .executes(ctx -> showStatus(ctx.getSource(), null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> showStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                        )
                )
                .then(Commands.literal("require")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setRequired(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))
                        )
                )
        );
    }

    private static PlaybackMode parseMode(String modeStr) {
        if ("fire_and_forget".equalsIgnoreCase(modeStr) || "fireandforget".equalsIgnoreCase(modeStr)) {
            return PlaybackMode.FIRE_AND_FORGET;
        }
        return PlaybackMode.SERVER_CONTROLLED;
    }

    private static int playTrack(CommandSourceStack source, String track, String categoryStr, PlaybackMode mode, boolean looping, float volume, float pitch, int fadeDurationMs, int syncThresholdMs) {
        var category = ModSoundSources.parseCategory(categoryStr);
        EventAudioManager.getInstance().playAudio(source.getServer(), track, category, null, volume, pitch, fadeDurationMs, mode, looping, syncThresholdMs);
        source.sendSuccess(() -> Component.literal("§aPlaying event track '" + track + "' (channel: " + category.getName() + ", mode: " + mode + ", loop: " + looping + ") for all players."), true);
        return 1;
    }

    private static int stopAudio(CommandSourceStack source, int fadeDurationMs) {
        EventAudioManager.getInstance().stopAudio(source.getServer(), null, fadeDurationMs);
        source.sendSuccess(() -> Component.literal("§eStopped event audio for all players (fade: " + fadeDurationMs + "ms)."), true);
        return 1;
    }

    private static int reloadPacks(CommandSourceStack source) {
        EventAudioManager.getInstance().reloadPacks(source.getServer());
        String sha1 = EventAudioManager.getInstance().getPackBuilder().getSha1Hex();
        source.sendSuccess(() -> Component.literal("§aReloaded event music tracks! Local SHA1: §f" + sha1), true);
        return 1;
    }

    private static int showStatus(CommandSourceStack source, ServerPlayer targetPlayer) {
        if (targetPlayer != null) {
            EventAudioManager.PackStatus status = EventAudioManager.getInstance().getPlayerStatus(targetPlayer.getUUID());
            source.sendSuccess(() -> Component.literal("§bPlayer " + targetPlayer.getScoreboardName() + " Audio Pack Status: §f" + status), false);
        } else {
            source.sendSuccess(() -> Component.literal("§b=== Event Audio Pack Player Statuses ==="), false);
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                EventAudioManager.PackStatus status = EventAudioManager.getInstance().getPlayerStatus(player.getUUID());
                ChatFormatting color = status == EventAudioManager.PackStatus.READY_FOR_EVENT ? ChatFormatting.GREEN : ChatFormatting.RED;
                source.sendSuccess(() -> Component.literal("- " + player.getScoreboardName() + ": ").append(Component.literal(status.name()).withStyle(color)), false);
            }
        }
        return 1;
    }

    private static int setRequired(CommandSourceStack source, boolean enabled) {
        net.dandare21.fracturedutils.config.ServerConfig.setEventAudioRequirePack(enabled);
        source.sendSuccess(() -> Component.literal("§aEvent audio resource pack mandatory requirement set to: §f" + enabled), true);
        return 1;
    }
}
