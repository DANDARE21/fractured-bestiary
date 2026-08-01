package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.cutscene.ServerCutsceneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SCutsceneEndPacket {
    private final UUID cutsceneId;

    public C2SCutsceneEndPacket(UUID cutsceneId) {
        this.cutsceneId = cutsceneId;
    }

    public C2SCutsceneEndPacket(FriendlyByteBuf buf) {
        this.cutsceneId = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.cutsceneId != null ? this.cutsceneId : new UUID(0L, 0L));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ServerCutsceneManager.getInstance().onClientCutsceneEnd(sender, cutsceneId);
            }
        });
        context.setPacketHandled(true);
    }

    public UUID getCutsceneId() {
        return cutsceneId;
    }
}
