package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.dialog.DialogManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SStartDialogSequencePacket {
    private final String fileName;

    public C2SStartDialogSequencePacket(String fileName) {
        this.fileName = fileName != null ? fileName : "";
    }

    public C2SStartDialogSequencePacket(FriendlyByteBuf buf) {
        this.fileName = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.fileName);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
                boolean started = DialogManager.getInstance().startSequence(fileName);
                if (started) {
                    player.sendSystemMessage(Component.literal("Started dialog sequence '" + fileName + "' for all players.")
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    player.sendSystemMessage(Component.literal("Failed to start dialog sequence '" + fileName + "'. File missing or invalid.")
                            .withStyle(ChatFormatting.RED));
                }
            }
        });
        context.setPacketHandled(true);
    }
}
