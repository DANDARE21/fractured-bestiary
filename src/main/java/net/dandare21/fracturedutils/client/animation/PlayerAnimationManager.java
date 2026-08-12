package net.dandare21.fracturedutils.client.animation;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerAnimationManager {
    private static final Map<AbstractClientPlayer, ModifierLayer<IAnimation>> playerLayers = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        FracturedUtils.LOGGER.info("[PlayerAnim] Initializing PlayerAnimationAccess event listeners...");
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, animationStack) -> {
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            animationStack.addAnimLayer(1000, layer);
            playerLayers.put(player, layer);
            FracturedUtils.LOGGER.info("[PlayerAnim] Successfully registered animation stack layer (priority 1000) for player instance");
        });
    }

    public static boolean isAnimationPlaying(Player player) {
        if (player instanceof AbstractClientPlayer clientPlayer) {
            ModifierLayer<IAnimation> layer = getOrCreateLayer(clientPlayer);
            return layer != null && layer.isActive();
        }
        return false;
    }

    private static ModifierLayer<IAnimation> getOrCreateLayer(AbstractClientPlayer clientPlayer) {
        ModifierLayer<IAnimation> layer = playerLayers.get(clientPlayer);
        if (layer == null) {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(clientPlayer);
            if (animationStack != null) {
                layer = new ModifierLayer<>();
                animationStack.addAnimLayer(1000, layer);
                playerLayers.put(clientPlayer, layer);
                FracturedUtils.LOGGER.info("[PlayerAnim] Attached new ModifierLayer (priority 1000) directly to player AnimationStack");
            }
        }
        return layer;
    }

    public static void playAnimation(Player player, String animationName, boolean loop) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        ModifierLayer<IAnimation> layer = getOrCreateLayer(clientPlayer);
        if (layer == null) {
            FracturedUtils.LOGGER.error("[PlayerAnim] Could not get or create AnimationStack layer for player!");
            return;
        }

        // Print all registered animation IDs in PlayerAnimationRegistry for debugging
        var registeredMap = PlayerAnimationRegistry.getAnimations();
        FracturedUtils.LOGGER.info("[PlayerAnim] Current PlayerAnimationRegistry size: {}. Registered keys: {}", 
                registeredMap != null ? registeredMap.size() : 0, 
                registeredMap != null ? registeredMap.keySet() : "null");

        ResourceLocation id1 = new ResourceLocation(FracturedUtils.MOD_ID, animationName);
        ResourceLocation id2 = new ResourceLocation(FracturedUtils.MOD_ID, "animation.player.startDown");
        ResourceLocation id3 = new ResourceLocation(FracturedUtils.MOD_ID, "startDown");
        ResourceLocation id4 = new ResourceLocation(FracturedUtils.MOD_ID, "startdown");
        ResourceLocation id5 = new ResourceLocation(FracturedUtils.MOD_ID, "player.animation");

        KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(id1);
        if (animation == null) animation = PlayerAnimationRegistry.getAnimation(id2);
        if (animation == null) animation = PlayerAnimationRegistry.getAnimation(id3);
        if (animation == null) animation = PlayerAnimationRegistry.getAnimation(id4);
        if (animation == null) animation = PlayerAnimationRegistry.getAnimation(id5);

        // Case-insensitive & suffix search across all registered keys
        if (animation == null && registeredMap != null) {
            String targetLower = animationName.toLowerCase();
            for (Map.Entry<ResourceLocation, KeyframeAnimation> entry : registeredMap.entrySet()) {
                String pathLower = entry.getKey().getPath().toLowerCase();
                if (pathLower.equals(targetLower) || pathLower.endsWith(targetLower) || pathLower.endsWith("." + targetLower) || pathLower.contains(targetLower)) {
                    animation = entry.getValue();
                    FracturedUtils.LOGGER.info("[PlayerAnim] Matched KeyframeAnimation from registry: {}", entry.getKey());
                    break;
                }
            }
        }

        if (animation != null) {
            KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);
            animPlayer.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            animPlayer.setFirstPersonConfiguration(new FirstPersonConfiguration());

            layer.setAnimation(animPlayer);
            FracturedUtils.LOGGER.info("[PlayerAnim] SUCCESS! Playing KeyframeAnimation on player layer!");
        } else {
            FracturedUtils.LOGGER.error("[PlayerAnim] Unable to find KeyframeAnimation '{}' in PlayerAnimationRegistry! Tried: {}, {}, {}, {}, {}", 
                    animationName, id1, id2, id3, id4, id5);
        }
    }

    public static void stopAnimation(Player player) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;
        ModifierLayer<IAnimation> layer = playerLayers.get(clientPlayer);
        if (layer != null) {
            layer.setAnimation(null);
            FracturedUtils.LOGGER.info("[PlayerAnim] Stopped animation layer for player");
        }
    }
}
