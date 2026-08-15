package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.client.ClientDialogHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CDialogDisplayPacket {
    private final String speaker;
    private final String text;
    private final int delayTicks;
    private final int charSpeedTicks;
    private final String letterSound;
    private final float letterSoundPitchMin;
    private final float letterSoundPitchMax;
    private final boolean waitForInput;

    // Custom camera settings
    private final boolean useCamera;
    private final double cameraX;
    private final double cameraY;
    private final double cameraZ;
    private final float cameraYaw;
    private final float cameraPitch;
    private final double cameraFov;

    public S2CDialogDisplayPacket(String speaker, String text, int delayTicks, int charSpeedTicks, String letterSound, float letterSoundPitchMin, float letterSoundPitchMax, boolean waitForInput, boolean useCamera, double cameraX, double cameraY, double cameraZ, float cameraYaw, float cameraPitch, double cameraFov) {
        this.speaker = speaker != null ? speaker : "";
        this.text = text != null ? text : "";
        this.delayTicks = Math.max(1, delayTicks);
        this.charSpeedTicks = Math.max(0, charSpeedTicks);
        this.letterSound = letterSound != null ? letterSound : "";
        this.letterSoundPitchMin = letterSoundPitchMin > 0 ? letterSoundPitchMin : 0.8f;
        this.letterSoundPitchMax = letterSoundPitchMax > 0 ? letterSoundPitchMax : 1.2f;
        this.waitForInput = waitForInput;
        this.useCamera = useCamera;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.cameraYaw = cameraYaw;
        this.cameraPitch = cameraPitch;
        this.cameraFov = cameraFov > 0 ? cameraFov : 70.0;
    }

    public S2CDialogDisplayPacket(FriendlyByteBuf buf) {
        this.speaker = buf.readUtf(32767);
        this.text = buf.readUtf(32767);
        this.delayTicks = buf.readVarInt();
        this.charSpeedTicks = buf.readVarInt();
        this.letterSound = buf.readUtf(32767);
        this.letterSoundPitchMin = buf.readFloat();
        this.letterSoundPitchMax = buf.readFloat();
        this.waitForInput = buf.readBoolean();
        this.useCamera = buf.readBoolean();
        this.cameraX = buf.readDouble();
        this.cameraY = buf.readDouble();
        this.cameraZ = buf.readDouble();
        this.cameraYaw = buf.readFloat();
        this.cameraPitch = buf.readFloat();
        this.cameraFov = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(speaker, 32767);
        buf.writeUtf(text, 32767);
        buf.writeVarInt(delayTicks);
        buf.writeVarInt(charSpeedTicks);
        buf.writeUtf(letterSound, 32767);
        buf.writeFloat(letterSoundPitchMin);
        buf.writeFloat(letterSoundPitchMax);
        buf.writeBoolean(waitForInput);
        buf.writeBoolean(useCamera);
        buf.writeDouble(cameraX);
        buf.writeDouble(cameraY);
        buf.writeDouble(cameraZ);
        buf.writeFloat(cameraYaw);
        buf.writeFloat(cameraPitch);
        buf.writeDouble(cameraFov);
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public int getCharSpeedTicks() {
        return charSpeedTicks;
    }

    public String getLetterSound() {
        return letterSound;
    }

    public float getLetterSoundPitchMin() {
        return letterSoundPitchMin;
    }

    public float getLetterSoundPitchMax() {
        return letterSoundPitchMax;
    }

    public boolean isWaitForInput() {
        return waitForInput;
    }

    public boolean isUseCamera() {
        return useCamera;
    }

    public double getCameraX() {
        return cameraX;
    }

    public double getCameraY() {
        return cameraY;
    }

    public double getCameraZ() {
        return cameraZ;
    }

    public float getCameraYaw() {
        return cameraYaw;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public double getCameraFov() {
        return cameraFov;
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDialogHandler.displayDialog(
                    speaker, text, delayTicks, charSpeedTicks, letterSound, letterSoundPitchMin, letterSoundPitchMax, waitForInput,
                    useCamera, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch, cameraFov
            ));
        });
        ctx.setPacketHandled(true);
    }
}
