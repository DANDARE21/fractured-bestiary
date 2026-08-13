package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.gui.DialogHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class S2CDialogReadinessPacket {
    private final List<UUID> readyPlayerUUIDs;

    public S2CDialogReadinessPacket(List<UUID> readyPlayerUUIDs) {
        this.readyPlayerUUIDs = readyPlayerUUIDs != null ? readyPlayerUUIDs : new ArrayList<>();
    }

    public S2CDialogReadinessPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.readyPlayerUUIDs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            this.readyPlayerUUIDs.add(buf.readUUID());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(readyPlayerUUIDs.size());
        for (UUID uuid : readyPlayerUUIDs) {
            buf.writeUUID(uuid);
        }
    }

    public List<UUID> getReadyPlayerUUIDs() {
        return readyPlayerUUIDs;
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DialogHudOverlay.updateReadyPlayers(readyPlayerUUIDs));
        });
        ctx.setPacketHandled(true);
    }
}
