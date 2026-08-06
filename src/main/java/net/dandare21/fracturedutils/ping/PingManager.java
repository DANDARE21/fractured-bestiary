package net.dandare21.fracturedutils.ping;

import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.S2CSyncPingsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PingManager {
    private static final PingManager INSTANCE = new PingManager();

    private final Map<String, HudPing> pings = new ConcurrentHashMap<>();

    private PingManager() {}

    public static PingManager getInstance() {
        return INSTANCE;
    }

    public synchronized void addPing(MinecraftServer server, HudPing ping) {
        if (ping != null && ping.getId() != null) {
            pings.put(ping.getId().toLowerCase(), ping);
            if (server != null) {
                syncToAll(server);
            }
        }
    }

    public synchronized boolean removePing(MinecraftServer server, String id) {
        if (id == null) return false;
        HudPing removed = pings.remove(id.toLowerCase());
        if (removed != null) {
            if (server != null) {
                syncToAll(server);
            }
            return true;
        }
        return false;
    }

    public synchronized void clearPings(MinecraftServer server) {
        pings.clear();
        if (server != null) {
            syncToAll(server);
        }
    }

    public List<HudPing> getPings() {
        return new ArrayList<>(pings.values());
    }

    public HudPing getPing(String id) {
        if (id == null) return null;
        return pings.get(id.toLowerCase());
    }

    public void syncToAll(MinecraftServer server) {
        if (server == null) return;
        S2CSyncPingsPacket packet = new S2CSyncPingsPacket(getPings());
        ModMessages.sendToAllPlayers(packet, server);
    }

    public void syncToPlayer(ServerPlayer player) {
        if (player == null) return;
        S2CSyncPingsPacket packet = new S2CSyncPingsPacket(getPings());
        ModMessages.sendToPlayer(packet, player);
    }
}
