package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CPrepareVideoPacket {
    private final String videoUrl;
    private final UUID cutsceneId;
    private final boolean allowSkip;
    private final boolean deleteAfter;
    private final String customName;

    public S2CPrepareVideoPacket(String videoUrl, UUID cutsceneId, boolean allowSkip, boolean deleteAfter, String customName) {
        this.videoUrl = videoUrl;
        this.cutsceneId = cutsceneId;
        this.allowSkip = allowSkip;
        this.deleteAfter = deleteAfter;
        this.customName = customName;
    }

    public S2CPrepareVideoPacket(String videoUrl, UUID cutsceneId, boolean allowSkip, boolean deleteAfter) {
        this(videoUrl, cutsceneId, allowSkip, deleteAfter, null);
    }

    public S2CPrepareVideoPacket(FriendlyByteBuf buf) {
        this.videoUrl = buf.readUtf();
        this.cutsceneId = buf.readUUID();
        this.allowSkip = buf.readBoolean();
        this.deleteAfter = buf.readBoolean();
        boolean hasName = buf.readBoolean();
        this.customName = hasName ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.videoUrl);
        buf.writeUUID(this.cutsceneId);
        buf.writeBoolean(this.allowSkip);
        buf.writeBoolean(this.deleteAfter);
        buf.writeBoolean(this.customName != null);
        if (this.customName != null) {
            buf.writeUtf(this.customName);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePrepareVideo(videoUrl, cutsceneId, allowSkip, deleteAfter, customName));
        });
        context.setPacketHandled(true);
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public UUID getCutsceneId() {
        return cutsceneId;
    }

    public boolean isAllowSkip() {
        return allowSkip;
    }

    public boolean isDeleteAfter() {
        return deleteAfter;
    }

    public String getCustomName() {
        return customName;
    }
}
