package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientDialogHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSendDialogSequenceDataPacket {
    private final Map<String, String> sequenceFiles;

    public S2CSendDialogSequenceDataPacket(Map<String, String> sequenceFiles) {
        this.sequenceFiles = sequenceFiles != null ? sequenceFiles : new HashMap<>();
    }

    public S2CSendDialogSequenceDataPacket(FriendlyByteBuf buf) {
        this.sequenceFiles = new HashMap<>();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf(32767);
            String content = buf.readUtf(262144);
            this.sequenceFiles.put(name, content);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sequenceFiles.size());
        for (Map.Entry<String, String> entry : sequenceFiles.entrySet()) {
            buf.writeUtf(entry.getKey(), 32767);
            buf.writeUtf(entry.getValue(), 262144);
        }
    }

    public Map<String, String> getSequenceFiles() {
        return sequenceFiles;
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDialogHandler.openScreen(this.sequenceFiles));
        });
        ctx.setPacketHandled(true);
    }
}
