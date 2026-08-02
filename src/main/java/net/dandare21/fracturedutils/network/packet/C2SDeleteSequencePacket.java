package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.util.function.Supplier;

public class C2SDeleteSequencePacket {
    private final String fileName;

    public C2SDeleteSequencePacket(String fileName) {
        this.fileName = fileName != null ? fileName : "";
    }

    public C2SDeleteSequencePacket(FriendlyByteBuf buf) {
        this.fileName = buf.readUtf(32767);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fileName, 32767);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                String cleanName = OrchestratorManager.getInstance().sanitizeFileName(fileName);
                File file = new File(OrchestratorManager.getInstance().getDirectory(), cleanName);
                if (file.exists()) {
                    file.delete();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
