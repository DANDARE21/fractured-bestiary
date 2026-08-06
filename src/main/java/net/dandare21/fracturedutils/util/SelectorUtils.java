package net.dandare21.fracturedutils.util;

import com.mojang.brigadier.StringReader;
import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectorUtils {

    /**
     * Resolves a target selector string (e.g. "@a", "@a[team=TEAM1]", "@a[tag=dungeon]")
     * into a list of matching online ServerPlayers.
     */
    public static List<ServerPlayer> getTargetPlayers(MinecraftServer server, String selectorStr) {
        if (server == null) return Collections.emptyList();
        
        List<ServerPlayer> allPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        if (selectorStr == null || selectorStr.isBlank() || selectorStr.trim().equalsIgnoreCase("@a")) {
            return allPlayers;
        }

        try {
            CommandSourceStack source = server.createCommandSourceStack();
            StringReader reader = new StringReader(selectorStr.trim());
            EntitySelectorParser parser = new EntitySelectorParser(reader);
            EntitySelector selector = parser.parse();
            return selector.findPlayers(source);
        } catch (Exception e) {
            FracturedUtils.LOGGER.warn("[SelectorUtils] Failed to parse target selector '{}', falling back to all players: {}", selectorStr, e.getMessage());
            return allPlayers;
        }
    }

    /**
     * Checks if a specific player matches the target selector string.
     */
    public static boolean isPlayerMatching(MinecraftServer server, ServerPlayer player, String selectorStr) {
        if (server == null || player == null) return false;
        if (selectorStr == null || selectorStr.isBlank() || selectorStr.trim().equalsIgnoreCase("@a")) {
            return true;
        }
        List<ServerPlayer> matching = getTargetPlayers(server, selectorStr);
        return matching.contains(player);
    }
}
