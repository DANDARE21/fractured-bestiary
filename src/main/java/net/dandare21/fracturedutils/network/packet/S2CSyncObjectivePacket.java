package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientObjectiveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CSyncObjectivePacket {
    private final boolean active;
    private final String name;
    private final String description;
    private final String activeWaitText;
    private final String waitType;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final int remainingTicks;

    public S2CSyncObjectivePacket(boolean active, String name, String description, String activeWaitText, String waitType, double targetX, double targetY, double targetZ, int remainingTicks) {
        this.active = active;
        this.name = name != null ? name : "";
        this.description = description != null ? description : "";
        this.activeWaitText = activeWaitText != null ? activeWaitText : "";
        this.waitType = waitType != null ? waitType : "";
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.remainingTicks = remainingTicks;
    }

    public S2CSyncObjectivePacket(boolean active, String name, String description, String activeWaitText) {
        this(active, name, description, activeWaitText, "", 0.0, 0.0, 0.0, -1);
    }

    public S2CSyncObjectivePacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.name = buf.readUtf(32767);
        this.description = buf.readUtf(32767);
        this.activeWaitText = buf.readUtf(32767);
        this.waitType = buf.readUtf(32767);
        this.targetX = buf.readDouble();
        this.targetY = buf.readDouble();
        this.targetZ = buf.readDouble();
        this.remainingTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeUtf(name);
        buf.writeUtf(description);
        buf.writeUtf(activeWaitText);
        buf.writeUtf(waitType);
        buf.writeDouble(targetX);
        buf.writeDouble(targetY);
        buf.writeDouble(targetZ);
        buf.writeInt(remainingTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientObjectiveData.setObjective(active, name, description, activeWaitText, waitType, targetX, targetY, targetZ, remainingTicks));
        });
        ctx.setPacketHandled(true);
    }
}
