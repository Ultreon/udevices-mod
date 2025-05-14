package dev.ultreon.devicesnext.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BaseScreen extends Screen {
    protected BaseScreen(Component pTitle) {
        super(pTitle);
    }

    public static void renderFrame(@NotNull GuiGraphics gfx, int x, int y, int width, int height) {

    }

    public boolean isPointBetween(int pointX, int pointY, int x, int y, int width, int height) {
        return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
    }
}
