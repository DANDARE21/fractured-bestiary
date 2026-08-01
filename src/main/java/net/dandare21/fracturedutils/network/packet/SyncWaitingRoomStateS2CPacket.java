package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncWaitingRoomStateS2CPacket {
    private final boolean active;
    private final String roomTitle;
    private final List<String> playerNames;
    private final List<UUID> playerUUIDs;
    private final List<Boolean> playerReadyStates;
    private final List<Integer> finishedDownloads;
    private final List<Integer> remainingDownloads;
    private final long elapsedSeconds;
    private final boolean isCountingDown;
    private final long countdownRemainingSeconds;

    public SyncWaitingRoomStateS2CPacket(boolean active, String roomTitle, List<String> playerNames, List<UUID> playerUUIDs, List<Boolean> playerReadyStates, List<Integer> finishedDownloads, List<Integer> remainingDownloads, long elapsedSeconds, boolean isCountingDown, long countdownRemainingSeconds) {
        this.active = active;
        this.roomTitle = roomTitle;
        this.playerNames = playerNames;
        this.playerUUIDs = playerUUIDs;
        this.playerReadyStates = playerReadyStates;
        this.finishedDownloads = finishedDownloads;
        this.remainingDownloads = remainingDownloads;
        this.elapsedSeconds = elapsedSeconds;
        this.isCountingDown = isCountingDown;
        this.countdownRemainingSeconds = countdownRemainingSeconds;
    }

    public SyncWaitingRoomStateS2CPacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.roomTitle = buf.readUtf();
        this.elapsedSeconds = buf.readVarLong();
        this.isCountingDown = buf.readBoolean();
        this.countdownRemainingSeconds = buf.readVarLong();
        int size = buf.readVarInt();
        this.playerNames = new ArrayList<>(size);
        this.playerUUIDs = new ArrayList<>(size);
        this.playerReadyStates = new ArrayList<>(size);
        this.finishedDownloads = new ArrayList<>(size);
        this.remainingDownloads = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.playerNames.add(buf.readUtf());
            this.playerUUIDs.add(buf.readUUID());
            this.playerReadyStates.add(buf.readBoolean());
            this.finishedDownloads.add(buf.readVarInt());
            this.remainingDownloads.add(buf.readVarInt());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.active);
        buf.writeUtf(this.roomTitle != null ? this.roomTitle : "Starting Soon...");
        buf.writeVarLong(this.elapsedSeconds);
        buf.writeBoolean(this.isCountingDown);
        buf.writeVarLong(this.countdownRemainingSeconds);
        buf.writeVarInt(this.playerNames.size());
        for (int i = 0; i < this.playerNames.size(); i++) {
            buf.writeUtf(this.playerNames.get(i));
            buf.writeUUID(this.playerUUIDs.get(i));
            buf.writeBoolean(this.playerReadyStates.get(i));
            buf.writeVarInt(i < this.finishedDownloads.size() ? this.finishedDownloads.get(i) : 0);
            buf.writeVarInt(i < this.remainingDownloads.size() ? this.remainingDownloads.get(i) : 0);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncState(active, roomTitle, playerNames, playerUUIDs, playerReadyStates, finishedDownloads, remainingDownloads, elapsedSeconds, isCountingDown, countdownRemainingSeconds));
        });
        context.setPacketHandled(true);
    }
}
