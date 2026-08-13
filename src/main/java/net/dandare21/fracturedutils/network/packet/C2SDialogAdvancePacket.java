package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.dialog.DialogManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SDialogAdvancePacket {
    public C2SDialogAdvancePacket() {
    }

    public C2SDialogAdvancePacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null && sender.getServer() != null) {
                DialogManager.getInstance().recordPlayerReady(sender, sender.getServer());
            }
        });
        ctx.setPacketHandled(true);
    }
}
