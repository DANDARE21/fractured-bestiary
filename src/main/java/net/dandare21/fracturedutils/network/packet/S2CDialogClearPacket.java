package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.gui.DialogHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CDialogClearPacket {
    public S2CDialogClearPacket() {
    }

    public S2CDialogClearPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DialogHudOverlay.clearActiveDialog());
        });
        ctx.setPacketHandled(true);
    }
}
