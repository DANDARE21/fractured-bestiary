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

    public SyncWaitingRoomStateS2CPacket(boolean active, String roomTitle, List<String> playerNames, List<UUID> playerUUIDs, List<Boolean> playerReadyStates) {
        this.active = active;
        this.roomTitle = roomTitle;
        this.playerNames = playerNames;
        this.playerUUIDs = playerUUIDs;
        this.playerReadyStates = playerReadyStates;
    }

    public SyncWaitingRoomStateS2CPacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.roomTitle = buf.readUtf();
        int size = buf.readVarInt();
        this.playerNames = new ArrayList<>(size);
        this.playerUUIDs = new ArrayList<>(size);
        this.playerReadyStates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.playerNames.add(buf.readUtf());
            this.playerUUIDs.add(buf.readUUID());
            this.playerReadyStates.add(buf.readBoolean());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.active);
        buf.writeUtf(this.roomTitle != null ? this.roomTitle : "Starting Soon...");
        buf.writeVarInt(this.playerNames.size());
        for (int i = 0; i < this.playerNames.size(); i++) {
            buf.writeUtf(this.playerNames.get(i));
            buf.writeUUID(this.playerUUIDs.get(i));
            buf.writeBoolean(this.playerReadyStates.get(i));
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncState(active, roomTitle, playerNames, playerUUIDs, playerReadyStates));
        });
        context.setPacketHandled(true);
    }
}
