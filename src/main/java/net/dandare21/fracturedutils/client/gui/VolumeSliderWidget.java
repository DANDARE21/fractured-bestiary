package net.dandare21.fracturedutils.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class VolumeSliderWidget extends AbstractSliderButton {

    private final String labelPrefix;
    private final Consumer<Float> onValueChanged;

    public VolumeSliderWidget(int x, int y, int width, int height, String labelPrefix, float initialValue, Consumer<Float> onValueChanged) {
        super(x, y, width, height, Component.empty(), initialValue);
        this.labelPrefix = labelPrefix;
        this.onValueChanged = onValueChanged;
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        int pct = (int) Math.round(this.value * 100.0);
        this.setMessage(Component.literal(labelPrefix + ": " + (pct == 0 ? "OFF" : pct + "%")));
    }

    @Override
    protected void applyValue() {
        if (onValueChanged != null) {
            onValueChanged.accept((float) this.value);
        }
    }
}
