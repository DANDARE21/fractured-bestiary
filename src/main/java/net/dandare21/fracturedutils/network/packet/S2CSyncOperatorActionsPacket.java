package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientOperatorActionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSyncOperatorActionsPacket {
    private final Map<String, String> operatorActions;

    public S2CSyncOperatorActionsPacket(Map<String, String> operatorActions) {
        this.operatorActions = operatorActions != null ? new HashMap<>(operatorActions) : new HashMap<>();
    }

    public S2CSyncOperatorActionsPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.operatorActions = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String triggerId = buf.readUtf(32767);
            String label = buf.readUtf(32767);
            this.operatorActions.put(triggerId, label);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(operatorActions.size());
        for (Map.Entry<String, String> entry : operatorActions.entrySet()) {
            buf.writeUtf(entry.getKey(), 32767);
            buf.writeUtf(entry.getValue() != null ? entry.getValue() : "", 32767);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientOperatorActionHandler.updateActiveActions(operatorActions));
        });
        ctx.setPacketHandled(true);
    }
}
