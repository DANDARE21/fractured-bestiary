package net.dandare21.fracturedutils.maintenance;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceManager {
    private static final MaintenanceManager INSTANCE = new MaintenanceManager();

    public static MaintenanceManager getInstance() {
        return INSTANCE;
    }

    public int startMaintenance(MinecraftServer server, String reason) {
        MaintenanceSavedData data = MaintenanceSavedData.get(server);
        data.setActive(true);
        data.setReason(reason);

        int kickedCount = 0;
        List<ServerPlayer> playersToKick = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!server.getPlayerList().isOp(player.getGameProfile())) {
                playersToKick.add(player);
            }
        }

        for (ServerPlayer player : playersToKick) {
            player.connection.disconnect(Component.literal(reason));
            kickedCount++;
        }

        return kickedCount;
    }

    public void stopMaintenance(MinecraftServer server) {
        MaintenanceSavedData data = MaintenanceSavedData.get(server);
        data.setActive(false);
    }

    public boolean isMaintenanceActive(MinecraftServer server) {
        return MaintenanceSavedData.get(server).isActive();
    }

    public String getMaintenanceReason(MinecraftServer server) {
        return MaintenanceSavedData.get(server).getReason();
    }

    public void checkAndKickOnJoin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        MaintenanceSavedData data = MaintenanceSavedData.get(server);
        if (data.isActive() && !server.getPlayerList().isOp(player.getGameProfile())) {
            player.connection.disconnect(Component.literal(data.getReason()));
        }
    }
}
