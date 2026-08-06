package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientPacketHandler;
import net.dandare21.fracturedutils.ping.HudPing;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CSyncPingsPacket {
    private final List<HudPing> pings;

    public S2CSyncPingsPacket(List<HudPing> pings) {
        this.pings = pings != null ? pings : new ArrayList<>();
    }

    public S2CSyncPingsPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.pings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.pings.add(new HudPing(buf));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(pings.size());
        for (HudPing ping : pings) {
            ping.encode(buf);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncPings(pings));
        });
        ctx.setPacketHandled(true);
    }
}
