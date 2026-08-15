package net.dandare21.fracturedutils.sound;

import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FracturedUtils.MOD_ID);

    public static final RegistryObject<SoundEvent> BLIP_DEFAULT = registerSoundEvent("dialog.blip_default");
    public static final RegistryObject<SoundEvent> BLIP_LOW = registerSoundEvent("dialog.blip_low");
    public static final RegistryObject<SoundEvent> BLIP_HIGH = registerSoundEvent("dialog.blip_high");
    public static final RegistryObject<SoundEvent> BLIP_SANS = registerSoundEvent("dialog.blip_sans");
    public static final RegistryObject<SoundEvent> BLIP_PAPYRUS = registerSoundEvent("dialog.blip_papyrus");
    public static final RegistryObject<SoundEvent> BLIP_ROBOT = registerSoundEvent("dialog.blip_robot");
    public static final RegistryObject<SoundEvent> BLIP_TYPING = registerSoundEvent("dialog.blip_typing");
    public static final RegistryObject<SoundEvent> BLIP_MONSTER = registerSoundEvent("dialog.blip_monster");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FracturedUtils.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    /**
     * Fetches a SoundEvent strictly from the resourcepack / Forge sound registry.
     */
    public static SoundEvent resolveSound(String soundId) {
        if (soundId == null || soundId.trim().isEmpty()) return null;
        String id = soundId.trim();

        try {
            ResourceLocation loc = id.contains(":") ? new ResourceLocation(id) : new ResourceLocation(FracturedUtils.MOD_ID, id);
            SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(loc);
            if (soundEvent != null) return soundEvent;

            // Check alternate namespace variants (fracturedutils vs fractured_utils)
            if (id.startsWith("fracturedutils:")) {
                ResourceLocation altLoc = new ResourceLocation("fractured_utils", id.substring(15));
                SoundEvent altSound = ForgeRegistries.SOUND_EVENTS.getValue(altLoc);
                if (altSound != null) return altSound;
            } else if (id.startsWith("fractured_utils:")) {
                ResourceLocation altLoc = new ResourceLocation("fracturedutils", id.substring(16));
                SoundEvent altSound = ForgeRegistries.SOUND_EVENTS.getValue(altLoc);
                if (altSound != null) return altSound;
            }

            return SoundEvent.createVariableRangeEvent(loc);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifies whether a given sound resource exists on registry.
     */
    public static boolean isSoundRegistered(String soundId) {
        if (soundId == null || soundId.trim().isEmpty()) return true;
        try {
            ResourceLocation loc = soundId.contains(":") ? new ResourceLocation(soundId.trim()) : new ResourceLocation(FracturedUtils.MOD_ID, soundId.trim());
            return ForgeRegistries.SOUND_EVENTS.containsKey(loc);
        } catch (Exception e) {
            return false;
        }
    }
}
