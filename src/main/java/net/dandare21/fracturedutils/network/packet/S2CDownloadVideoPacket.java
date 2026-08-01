package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CDownloadVideoPacket {
    private final UUID downloadId;
    private final String customName;
    private final String videoUrl;

    public S2CDownloadVideoPacket(UUID downloadId, String customName, String videoUrl) {
        this.downloadId = downloadId;
        this.customName = customName;
        this.videoUrl = videoUrl;
    }

    public S2CDownloadVideoPacket(FriendlyByteBuf buf) {
        this.downloadId = buf.readUUID();
        this.customName = buf.readUtf();
        this.videoUrl = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.downloadId);
        buf.writeUtf(this.customName != null ? this.customName : "");
        buf.writeUtf(this.videoUrl != null ? this.videoUrl : "");
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDownloadVideo(downloadId, customName, videoUrl));
        });
        context.setPacketHandled(true);
    }

    public UUID getDownloadId() {
        return downloadId;
    }

    public String getCustomName() {
        return customName;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}
