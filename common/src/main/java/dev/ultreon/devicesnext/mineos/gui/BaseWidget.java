package dev.ultreon.devicesnext.mineos.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BaseWidget extends AbstractWidget {
    protected Minecraft minecraft = Minecraft.getInstance();
    protected Font font = minecraft.font;

    public BaseWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {

    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationOutput) {

    }

    protected void onLeftClick(int clicks) {

    }
}
