package net.dandare21.fracturedutils.sound.event;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;

import java.util.UUID;

public class ResourcePackStatusInterceptor extends ChannelInboundHandlerAdapter {

    private final UUID playerUuid;

    public ResourcePackStatusInterceptor(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServerboundResourcePackPacket packPacket) {
            ServerboundResourcePackPacket.Action action = packPacket.getAction();
            EventAudioManager.getInstance().updatePlayerPackStatus(playerUuid, action);
        }
        super.channelRead(ctx, msg);
    }
}
