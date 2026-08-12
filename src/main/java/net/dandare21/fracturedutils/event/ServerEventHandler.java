package net.dandare21.fracturedutils.event;

import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.command.DownloadCinematicCommand;
import net.dandare21.fracturedutils.command.MaintenanceCommands;
import net.dandare21.fracturedutils.command.PlayCinematicCommand;
import net.dandare21.fracturedutils.command.WaitingRoomCommands;
import net.dandare21.fracturedutils.cutscene.ServerCutsceneManager;
import net.dandare21.fracturedutils.maintenance.MaintenanceManager;
import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.dandare21.fracturedutils.waitingroom.WaitingRoomManager;
import net.dandare21.fracturedutils.config.ServerConfig;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FracturedUtils.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        WaitingRoomCommands.register(event.getDispatcher());
        MaintenanceCommands.register(event.getDispatcher());
        PlayCinematicCommand.register(event.getDispatcher());
        DownloadCinematicCommand.register(event.getDispatcher());
        net.dandare21.fracturedutils.command.OrchestratorCommand.register(event.getDispatcher());
        net.dandare21.fracturedutils.command.PingCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            OrchestratorManager.getInstance().tick(event.getServer());
            WaitingRoomManager.getInstance().tick(event.getServer());
            net.dandare21.fracturedutils.checkpoint.CheckpointManager.getInstance().tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            net.dandare21.fracturedutils.checkpoint.CheckpointManager mgr = net.dandare21.fracturedutils.checkpoint.CheckpointManager.getInstance();
            if (mgr.isPlayerDowned(player.getUUID())) {
                event.setCanceled(true);
                return;
            }

            if (mgr.hasActiveCheckpoint() && !player.isSpectator() && mgr.isPlayerMatchingCheckpoint(player.getServer(), player)) {
                if (player.getHealth() - event.getAmount() <= 0.0f) {
                    event.setCanceled(true);
                    mgr.setPlayerDowned(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            net.dandare21.fracturedutils.checkpoint.CheckpointManager mgr = net.dandare21.fracturedutils.checkpoint.CheckpointManager.getInstance();
            if (mgr.hasActiveCheckpoint() && !player.isSpectator() && mgr.isPlayerMatchingCheckpoint(player.getServer(), player)) {
                event.setCanceled(true);
                mgr.setPlayerDowned(player);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer reviver) {
            net.dandare21.fracturedutils.checkpoint.CheckpointManager mgr = net.dandare21.fracturedutils.checkpoint.CheckpointManager.getInstance();
            if (mgr.isPlayerDowned(targetPlayer.getUUID())) {
                mgr.recordReviveAttempt(reviver, targetPlayer);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && ServerConfig.isKeepInventoryNoXpEnabled()) {
            if (event.getEntity() instanceof ServerPlayer newPlayer) {
                newPlayer.experienceLevel = 0;
                newPlayer.totalExperience = 0;
                newPlayer.experienceProgress = 0.0F;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (ServerConfig.isKeepInventoryNoXpEnabled() && event.getEntity() instanceof ServerPlayer player) {
            player.experienceLevel = 0;
            player.totalExperience = 0;
            player.experienceProgress = 0.0F;
            if (player.connection != null) {
                player.connection.send(new ClientboundSetExperiencePacket(0.0F, 0, 0));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaintenanceManager.getInstance().checkAndKickOnJoin(player);
            net.dandare21.fracturedutils.ping.PingManager.getInstance().syncToPlayer(player);

            WaitingRoomManager mgr = WaitingRoomManager.getInstance();
            if (mgr.isActive()) {
                mgr.syncToPlayer(player);
                mgr.joinPlayer(player);
            } else {
                mgr.syncToPlayer(player);
            }

            if (player.hasPermissions(2)) {
                OrchestratorManager.getInstance().syncActiveSequenceTelemetryToOps(player.getServer());
                OrchestratorManager.getInstance().syncOperatorActionsToOps(player.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WaitingRoomManager.getInstance().removePlayerByUUID(player.getServer(), player.getUUID());
            ServerCutsceneManager.getInstance().onPlayerLoggedOut(player);
            OrchestratorManager.getInstance().onPlayerLoggedOut(player);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (WaitingRoomManager.getInstance().isPlayerJoined(player.getUUID()) || ServerCutsceneManager.getInstance().isPlayerInCutscene(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }
}
