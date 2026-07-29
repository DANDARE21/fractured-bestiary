package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.waitingroom.WaitingRoomManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class JoinWaitingRoomC2SPacket {
    public JoinWaitingRoomC2SPacket() {
    }

    public JoinWaitingRoomC2SPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                WaitingRoomManager.getInstance().joinPlayer(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
