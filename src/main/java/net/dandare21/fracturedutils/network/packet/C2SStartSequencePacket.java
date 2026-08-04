package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SStartSequencePacket {
    private final String fileName;
    private final int startIndex;

    public C2SStartSequencePacket(String fileName, int startIndex) {
        this.fileName = fileName != null ? fileName : "";
        this.startIndex = Math.max(0, startIndex);
    }

    public C2SStartSequencePacket(FriendlyByteBuf buf) {
        this.fileName = buf.readUtf(256);
        this.startIndex = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.fileName);
        buf.writeVarInt(this.startIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
                OrchestratorManager.getInstance().startSequence(fileName, player.getScoreboardName(), startIndex);
            }
        });
        context.setPacketHandled(true);
    }
}
