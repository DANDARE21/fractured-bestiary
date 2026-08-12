package net.dandare21.fracturedutils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TestAnimPacket {
    private final String animName;

    public TestAnimPacket(String animName) {
        this.animName = animName;
    }

    public TestAnimPacket(FriendlyByteBuf buf) {
        this.animName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(animName);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(animName));
        });
        ctx.setPacketHandled(true);
    }

    private static class ClientHandler {
        private static void handle(String animName) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                net.dandare21.fracturedutils.FracturedUtils.LOGGER.info("[TestAnim] Client executing /testanim command for animation: '{}'", animName);
                net.dandare21.fracturedutils.client.animation.PlayerAnimationManager.playAnimation(mc.player, animName, true);
            }
        }
    }
}
