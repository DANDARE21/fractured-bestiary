package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientOpMonitorData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CSyncSequenceTelemetryPacket {
    public static class ActionInfo {
        private final String type;
        private final String details;

        public ActionInfo(String type, String details) {
            this.type = type != null ? type : "";
            this.details = details != null ? details : "";
        }

        public String getType() {
            return type;
        }

        public String getDetails() {
            return details;
        }
    }

    public static class SequenceTelemetryData {
        private final String sequenceName;
        private final String targetPlayerName;
        private final int currentIndex;
        private final String state;
        private final List<ActionInfo> actions;

        public SequenceTelemetryData(String sequenceName, String targetPlayerName, int currentIndex, String state, List<ActionInfo> actions) {
            this.sequenceName = sequenceName != null ? sequenceName : "";
            this.targetPlayerName = targetPlayerName != null ? targetPlayerName : "";
            this.currentIndex = currentIndex;
            this.state = state != null ? state : "";
            this.actions = actions != null ? actions : new ArrayList<>();
        }

        public String getSequenceName() {
            return sequenceName;
        }

        public String getTargetPlayerName() {
            return targetPlayerName;
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public String getState() {
            return state;
        }

        public List<ActionInfo> getActions() {
            return actions;
        }
    }

    private final List<SequenceTelemetryData> telemetryList;

    public S2CSyncSequenceTelemetryPacket(List<SequenceTelemetryData> telemetryList) {
        this.telemetryList = telemetryList != null ? telemetryList : new ArrayList<>();
    }

    public S2CSyncSequenceTelemetryPacket(FriendlyByteBuf buf) {
        this.telemetryList = new ArrayList<>();
        int seqCount = buf.readVarInt();
        for (int i = 0; i < seqCount; i++) {
            String seqName = buf.readUtf(32767);
            String targetPlayer = buf.readUtf(32767);
            int curIndex = buf.readVarInt();
            String state = buf.readUtf(32767);
            int actionCount = buf.readVarInt();

            List<ActionInfo> actionInfos = new ArrayList<>();
            for (int a = 0; a < actionCount; a++) {
                String type = buf.readUtf(32767);
                String details = buf.readUtf(32767);
                actionInfos.add(new ActionInfo(type, details));
            }
            telemetryList.add(new SequenceTelemetryData(seqName, targetPlayer, curIndex, state, actionInfos));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(telemetryList.size());
        for (SequenceTelemetryData data : telemetryList) {
            buf.writeUtf(data.getSequenceName(), 32767);
            buf.writeUtf(data.getTargetPlayerName(), 32767);
            buf.writeVarInt(data.getCurrentIndex());
            buf.writeUtf(data.getState(), 32767);
            buf.writeVarInt(data.getActions().size());
            for (ActionInfo info : data.getActions()) {
                buf.writeUtf(info.getType(), 32767);
                buf.writeUtf(info.getDetails(), 32767);
            }
        }
    }

    public List<SequenceTelemetryData> getTelemetryList() {
        return telemetryList;
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientOpMonitorData.updateTelemetry(this.telemetryList));
        });
        ctx.setPacketHandled(true);
    }
}
