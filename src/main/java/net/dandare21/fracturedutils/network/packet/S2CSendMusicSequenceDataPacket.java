package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientMusicSequenceHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSendMusicSequenceDataPacket {
    private final Map<String, String> sequenceFiles;
    private final List<String> availableTracks;

    public S2CSendMusicSequenceDataPacket(Map<String, String> sequenceFiles, List<String> availableTracks) {
        this.sequenceFiles = sequenceFiles != null ? sequenceFiles : new HashMap<>();
        this.availableTracks = availableTracks != null ? availableTracks : new ArrayList<>();
    }

    public S2CSendMusicSequenceDataPacket(FriendlyByteBuf buf) {
        this.sequenceFiles = new HashMap<>();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf(32767);
            String content = buf.readUtf(262144);
            this.sequenceFiles.put(name, content);
        }

        this.availableTracks = new ArrayList<>();
        int trackCount = buf.readVarInt();
        for (int i = 0; i < trackCount; i++) {
            this.availableTracks.add(buf.readUtf(32767));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sequenceFiles.size());
        for (Map.Entry<String, String> entry : sequenceFiles.entrySet()) {
            buf.writeUtf(entry.getKey(), 32767);
            buf.writeUtf(entry.getValue(), 262144);
        }

        buf.writeVarInt(availableTracks.size());
        for (String track : availableTracks) {
            buf.writeUtf(track, 32767);
        }
    }

    public Map<String, String> getSequenceFiles() {
        return sequenceFiles;
    }

    public List<String> getAvailableTracks() {
        return availableTracks;
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMusicSequenceHandler.openScreen(this.sequenceFiles, this.availableTracks));
        });
        ctx.setPacketHandled(true);
    }
}
