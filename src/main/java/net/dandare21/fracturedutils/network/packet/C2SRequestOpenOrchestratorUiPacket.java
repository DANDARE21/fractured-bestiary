package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class C2SRequestOpenOrchestratorUiPacket {

    public C2SRequestOpenOrchestratorUiPacket() {
    }

    public C2SRequestOpenOrchestratorUiPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                Map<String, String> files = OrchestratorManager.getInstance().getAllSequenceFiles();
                ModMessages.sendToPlayer(new S2CSendSequenceDataPacket(files), player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
