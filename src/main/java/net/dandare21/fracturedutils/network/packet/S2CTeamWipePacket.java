package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CTeamWipePacket {
    private final int durationSeconds;

    public S2CTeamWipePacket(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public S2CTeamWipePacket(FriendlyByteBuf buf) {
        this.durationSeconds = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(durationSeconds);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleTeamWipe(durationSeconds));
        });
        ctx.setPacketHandled(true);
    }
}
