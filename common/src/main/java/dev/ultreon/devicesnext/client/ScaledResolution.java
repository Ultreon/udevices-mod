package dev.ultreon.devicesnext.client;

import net.minecraft.client.Minecraft;

public class ScaledResolution {
    private final Minecraft mc;

    public ScaledResolution(Minecraft mc) {
        this.mc = mc;
    }

    public double getScaleFactor() {
        return mc.getWindow().getGuiScale();
    }

    public int getWidth() {
        return this.mc.getWindow().getGuiScaledWidth();
    }

    public int getHeight() {
        return this.mc.getWindow().getGuiScaledHeight();
    }
}
