package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.event.EventAudioClientController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CStopEventAudioPacket {

    private final int fadeDurationMs;

    public S2CStopEventAudioPacket(int fadeDurationMs) {
        this.fadeDurationMs = fadeDurationMs;
    }

    public S2CStopEventAudioPacket(FriendlyByteBuf buf) {
        this.fadeDurationMs = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.fadeDurationMs);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                EventAudioClientController.getInstance().stopAudio(fadeDurationMs);
            });
        });
        return true;
    }

    public int getFadeDurationMs() { return fadeDurationMs; }
}
