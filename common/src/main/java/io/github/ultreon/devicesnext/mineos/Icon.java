package com.ultreon.mods.lib.client.devicetest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public interface Icon {
    ResourceLocation resource();
    int width();
    int height();
    int vHeight();
    int uWidth();
    int v();
    int u();
    int texWidth();
    int texHeight();

    default void render(GuiGraphics gfx, int x, int y) {
        ResourceLocation resource = this.resource();
        gfx.blit(resource, x, y, width(), height(), u(), v(), uWidth(), vHeight(), texWidth(), texHeight());
    }

    default void render(GuiGraphics gfx, int x, int y, int width, int height) {
        ResourceLocation resource = this.resource();
        gfx.blit(resource, x, y, width, height, u(), v(), uWidth(), vHeight(), texWidth(), texHeight());
    }
}
