package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.event.EventAudioManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SResourcePackStatusPacket {

    private final ServerboundResourcePackPacket.Action action;

    public C2SResourcePackStatusPacket(ServerboundResourcePackPacket.Action action) {
        this.action = action != null ? action : ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD;
    }

    public C2SResourcePackStatusPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(ServerboundResourcePackPacket.Action.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                EventAudioManager.getInstance().updatePlayerPackStatus(player, action);
            }
        });
        return true;
    }

    public ServerboundResourcePackPacket.Action getAction() { return action; }
}
