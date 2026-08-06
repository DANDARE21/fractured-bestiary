package net.dandare21.fracturedutils.particle;

import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FracturedUtils.MOD_ID);

    public static final RegistryObject<SimpleParticleType> MARKER_PARTICLE =
            PARTICLES.register("marker_particle", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> DOWNED_MARKER =
            PARTICLES.register("downed_marker", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
