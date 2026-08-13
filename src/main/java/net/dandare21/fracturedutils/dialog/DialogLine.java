package net.dandare21.fracturedutils.dialog;

public class DialogLine {
    private String speaker;
    private String text;
    private int delayTicks;
    private String sound;
    private float volume;
    private float pitch;

    // Undertale-like typewriter settings
    private int charSpeedTicks;
    private String letterSound;
    private float letterSoundPitchMin;
    private float letterSoundPitchMax;
    private boolean waitForInput;

    // Custom camera override settings
    private boolean useCamera;
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    private float cameraYaw;
    private float cameraPitch;

    public DialogLine() {
        this.speaker = "";
        this.text = "";
        this.delayTicks = 40;
        this.sound = "";
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.charSpeedTicks = 1;
        this.letterSound = "";
        this.letterSoundPitchMin = 0.8f;
        this.letterSoundPitchMax = 1.2f;
        this.waitForInput = true;
        this.useCamera = false;
        this.cameraX = 0.0;
        this.cameraY = 0.0;
        this.cameraZ = 0.0;
        this.cameraYaw = 0.0f;
        this.cameraPitch = 0.0f;
    }

    public DialogLine(String speaker, String text, int delayTicks, String sound, float volume, float pitch, int charSpeedTicks, String letterSound, float letterSoundPitchMin, float letterSoundPitchMax, boolean waitForInput, boolean useCamera, double cameraX, double cameraY, double cameraZ, float cameraYaw, float cameraPitch) {
        this.speaker = speaker != null ? speaker : "";
        this.text = text != null ? text : "";
        this.delayTicks = Math.max(1, delayTicks);
        this.sound = sound != null ? sound : "";
        this.volume = volume > 0 ? volume : 1.0f;
        this.pitch = pitch > 0 ? pitch : 1.0f;
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
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker != null ? speaker : "";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = Math.max(1, delayTicks);
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound != null ? sound : "";
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public int getCharSpeedTicks() {
        return charSpeedTicks;
    }

    public void setCharSpeedTicks(int charSpeedTicks) {
        this.charSpeedTicks = Math.max(0, charSpeedTicks);
    }

    public String getLetterSound() {
        return letterSound;
    }

    public void setLetterSound(String letterSound) {
        this.letterSound = letterSound != null ? letterSound : "";
    }

    public float getLetterSoundPitchMin() {
        return letterSoundPitchMin;
    }

    public void setLetterSoundPitchMin(float letterSoundPitchMin) {
        this.letterSoundPitchMin = letterSoundPitchMin;
    }

    public float getLetterSoundPitchMax() {
        return letterSoundPitchMax;
    }

    public void setLetterSoundPitchMax(float letterSoundPitchMax) {
        this.letterSoundPitchMax = letterSoundPitchMax;
    }

    public boolean isWaitForInput() {
        return waitForInput;
    }

    public void setWaitForInput(boolean waitForInput) {
        this.waitForInput = waitForInput;
    }

    public boolean isUseCamera() {
        return useCamera;
    }

    public void setUseCamera(boolean useCamera) {
        this.useCamera = useCamera;
    }

    public double getCameraX() {
        return cameraX;
    }

    public void setCameraX(double cameraX) {
        this.cameraX = cameraX;
    }

    public double getCameraY() {
        return cameraY;
    }

    public void setCameraY(double cameraY) {
        this.cameraY = cameraY;
    }

    public double getCameraZ() {
        return cameraZ;
    }

    public void setCameraZ(double cameraZ) {
        this.cameraZ = cameraZ;
    }

    public float getCameraYaw() {
        return cameraYaw;
    }

    public void setCameraYaw(float cameraYaw) {
        this.cameraYaw = cameraYaw;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public void setCameraPitch(float cameraPitch) {
        this.cameraPitch = cameraPitch;
    }

    public DialogLine copy() {
        return new DialogLine(this.speaker, this.text, this.delayTicks, this.sound, this.volume, this.pitch, this.charSpeedTicks, this.letterSound, this.letterSoundPitchMin, this.letterSoundPitchMax, this.waitForInput, this.useCamera, this.cameraX, this.cameraY, this.cameraZ, this.cameraYaw, this.cameraPitch);
    }
}
