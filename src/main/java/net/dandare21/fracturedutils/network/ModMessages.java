package net.dandare21.fracturedutils.network;

import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.network.packet.JoinWaitingRoomC2SPacket;
import net.dandare21.fracturedutils.network.packet.OpenWaitingRoomScreenS2CPacket;
import net.dandare21.fracturedutils.network.packet.SyncWaitingRoomStateS2CPacket;
import net.dandare21.fracturedutils.network.packet.ToggleReadyC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(FracturedUtils.MOD_ID + ":main"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(SyncWaitingRoomStateS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncWaitingRoomStateS2CPacket::new)
                .encoder(SyncWaitingRoomStateS2CPacket::encode)
                .consumerMainThread(SyncWaitingRoomStateS2CPacket::handle)
                .add();

        net.messageBuilder(OpenWaitingRoomScreenS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenWaitingRoomScreenS2CPacket::new)
                .encoder(OpenWaitingRoomScreenS2CPacket::encode)
                .consumerMainThread(OpenWaitingRoomScreenS2CPacket::handle)
                .add();

        net.messageBuilder(JoinWaitingRoomC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(JoinWaitingRoomC2SPacket::new)
                .encoder(JoinWaitingRoomC2SPacket::encode)
                .consumerMainThread(JoinWaitingRoomC2SPacket::handle)
                .add();

        net.messageBuilder(ToggleReadyC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleReadyC2SPacket::new)
                .encoder(ToggleReadyC2SPacket::encode)
                .consumerMainThread(ToggleReadyC2SPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllPlayers(MSG message, MinecraftServer server) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
