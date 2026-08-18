package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.event.EventAudioClientController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CAudioSyncTimePacket {

    private final String soundEventId;
    private final long serverStartTimeMs;
    private final int syncThresholdMs;
    private final boolean looping;

    public S2CAudioSyncTimePacket(String soundEventId, long serverStartTimeMs, int syncThresholdMs, boolean looping) {
        this.soundEventId = soundEventId != null ? soundEventId : "";
        this.serverStartTimeMs = serverStartTimeMs;
        this.syncThresholdMs = syncThresholdMs;
        this.looping = looping;
    }

    public S2CAudioSyncTimePacket(FriendlyByteBuf buf) {
        this.soundEventId = buf.readUtf();
        this.serverStartTimeMs = buf.readVarLong();
        this.syncThresholdMs = buf.readVarInt();
        this.looping = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.soundEventId);
        buf.writeVarLong(this.serverStartTimeMs);
        buf.writeVarInt(this.syncThresholdMs);
        buf.writeBoolean(this.looping);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                EventAudioClientController.getInstance().handleServerSyncHeartbeat(
                        soundEventId, serverStartTimeMs, syncThresholdMs, looping
                );
            });
        });
        return true;
    }

    public String getSoundEventId() { return soundEventId; }
    public long getServerStartTimeMs() { return serverStartTimeMs; }
    public int getSyncThresholdMs() { return syncThresholdMs; }
    public boolean isLooping() { return looping; }
}
