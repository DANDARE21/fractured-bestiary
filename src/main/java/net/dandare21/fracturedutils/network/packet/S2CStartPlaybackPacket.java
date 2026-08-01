package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CStartPlaybackPacket {
    private final UUID cutsceneId;
    private final long scheduledStartTimeMs;

    public S2CStartPlaybackPacket(UUID cutsceneId, long scheduledStartTimeMs) {
        this.cutsceneId = cutsceneId;
        this.scheduledStartTimeMs = scheduledStartTimeMs;
    }

    public S2CStartPlaybackPacket(FriendlyByteBuf buf) {
        this.cutsceneId = buf.readUUID();
        this.scheduledStartTimeMs = buf.readLong();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.cutsceneId);
        buf.writeLong(this.scheduledStartTimeMs);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStartPlayback(cutsceneId, scheduledStartTimeMs));
        });
        context.setPacketHandled(true);
    }

    public UUID getCutsceneId() {
        return cutsceneId;
    }

    public long getScheduledStartTimeMs() {
        return scheduledStartTimeMs;
    }
}
