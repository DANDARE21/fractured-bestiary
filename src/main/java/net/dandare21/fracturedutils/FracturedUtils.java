package net.dandare21.fracturedutils;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FracturedUtils.MOD_ID)
public class FracturedUtils
{
    public static final String MOD_ID = "fractured_utils";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FracturedUtils(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        net.dandare21.fracturedutils.particle.ModParticles.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            net.dandare21.fracturedutils.network.ModMessages.register();
            net.dandare21.fracturedutils.config.ServerConfig.load();
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {
                net.dandare21.fracturedutils.client.animation.PlayerAnimationManager.init();
            });
        }

        @SubscribeEvent
        public static void registerParticleProviders(net.minecraftforge.client.event.RegisterParticleProvidersEvent event)
        {
            event.registerSpriteSet(net.dandare21.fracturedutils.particle.ModParticles.MARKER_PARTICLE.get(), net.dandare21.fracturedutils.particle.MarkerParticle.Provider::new);
            event.registerSpriteSet(net.dandare21.fracturedutils.particle.ModParticles.DOWNED_MARKER.get(), net.dandare21.fracturedutils.particle.MarkerParticle.Provider::new);
        }
    }
}
