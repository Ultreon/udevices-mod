package com.ultreon.mods.lib.client.devicetest.gui;

import com.ultreon.mods.lib.client.devicetest.OperatingSystem;
import com.ultreon.mods.lib.client.gui.widget.BaseWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public abstract class McComponent extends BaseWidget {
    private McContextMenu contextMenu;
    private boolean holding;
    @Nullable
    McContainer parent = null;

    public McComponent(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {

    }

    public final int getScreenWidth() {
        return OperatingSystem.get().getWidth();
    }

    public final int getScreenHeight() {
        return OperatingSystem.get().getHeight();
    }

    public final Insets getWmBorder() {
        return OperatingSystem.get().getClientAreaInsets();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) this.holding = true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean preMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.holding = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean isHolding() {
        return holding;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
