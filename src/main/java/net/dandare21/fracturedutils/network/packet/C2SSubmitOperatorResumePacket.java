package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SSubmitOperatorResumePacket {
    private final String triggerId;

    public C2SSubmitOperatorResumePacket(String triggerId) {
        this.triggerId = triggerId != null ? triggerId : "";
    }

    public C2SSubmitOperatorResumePacket(FriendlyByteBuf buf) {
        this.triggerId = buf.readUtf(32767);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(triggerId, 32767);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                OrchestratorManager.getInstance().resumeTrigger(triggerId);
                OrchestratorManager.getInstance().unregisterOperatorAction(player.getServer(), triggerId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
