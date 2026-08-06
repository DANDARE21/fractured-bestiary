package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CSyncDownedPacket {
    private final boolean downed;
    private final boolean revivingOther;
    private final float reviveProgress;

    public S2CSyncDownedPacket(boolean downed, boolean revivingOther, float reviveProgress) {
        this.downed = downed;
        this.revivingOther = revivingOther;
        this.reviveProgress = reviveProgress;
    }

    public S2CSyncDownedPacket(FriendlyByteBuf buf) {
        this.downed = buf.readBoolean();
        this.revivingOther = buf.readBoolean();
        this.reviveProgress = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(downed);
        buf.writeBoolean(revivingOther);
        buf.writeFloat(reviveProgress);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncDowned(downed, revivingOther, reviveProgress));
        });
        ctx.setPacketHandled(true);
    }
}
