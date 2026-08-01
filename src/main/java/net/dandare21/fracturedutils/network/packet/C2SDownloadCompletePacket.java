package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.cutscene.ServerCutsceneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SDownloadCompletePacket {
    private final UUID downloadId;
    private final String customName;
    private final boolean success;

    public C2SDownloadCompletePacket(UUID downloadId, String customName, boolean success) {
        this.downloadId = downloadId;
        this.customName = customName;
        this.success = success;
    }

    public C2SDownloadCompletePacket(FriendlyByteBuf buf) {
        this.downloadId = buf.readUUID();
        this.customName = buf.readUtf();
        this.success = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.downloadId);
        buf.writeUtf(this.customName != null ? this.customName : "");
        buf.writeBoolean(this.success);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerCutsceneManager.getInstance().onClientDownloadComplete(player, downloadId, customName, success);
            }
        });
        context.setPacketHandled(true);
    }

    public UUID getDownloadId() {
        return downloadId;
    }

    public String getCustomName() {
        return customName;
    }

    public boolean isSuccess() {
        return success;
    }
}
