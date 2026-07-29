package net.dandare21.fracturedutils.event;

import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.command.WaitingRoomCommands;
import net.dandare21.fracturedutils.waitingroom.WaitingRoomManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.dandare21.fracturedutils.command.MaintenanceCommands;
import net.dandare21.fracturedutils.maintenance.MaintenanceManager;

@Mod.EventBusSubscriber(modid = FracturedUtils.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            WaitingRoomManager.getInstance().tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        WaitingRoomCommands.register(event.getDispatcher());
        MaintenanceCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaintenanceManager.getInstance().checkAndKickOnJoin(player);

            WaitingRoomManager mgr = WaitingRoomManager.getInstance();
            if (mgr.isActive()) {
                mgr.syncToPlayer(player);
                mgr.joinPlayer(player);
            } else {
                mgr.syncToPlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WaitingRoomManager.getInstance().removePlayerByUUID(player.getServer(), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (WaitingRoomManager.getInstance().isPlayerJoined(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (WaitingRoomManager.getInstance().isPlayerJoined(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }
}
