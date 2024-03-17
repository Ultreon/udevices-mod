package com.ultreon.mods.lib.client.devicetest.gui;

import com.ultreon.mods.lib.client.devicetest.Icon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class McIcon extends McImage {
    private Icon icon;

    public McIcon(int x, int y, int size, Icon icon) {
        super(x, y, size, size);
        this.icon = icon;
    }

    @Override
    public int getWidth() {
        return icon.width();
    }

    @Override
    public int imageU() {
        return icon.u();
    }

    @Override
    public int imageV() {
        return icon.v();
    }

    @Override
    public int imageUWidth() {
        return icon.uWidth();
    }

    @Override
    public int imageVHeight() {
        return icon.vHeight();
    }

    @Override
    public int textureWidth() {
        return icon.texWidth();
    }

    @Override
    public int textureHeight() {
        return icon.texHeight();
    }

    @Override
    public ResourceLocation getResource() {
        return icon.resource();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        ResourceLocation resource = getResource();
        gfx.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xff555555);
        gfx.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, 0xff333333);

        if (resource == null) {
            // Resource not loaded.
            McImage.drawLoadingIcon(gfx, getX() + getWidth() / 2, getY() + getHeight() / 2);
            return;
        }

        gfx.blit(resource, getX(), getY(), getWidth(), getHeight(), 0, 0, textureWidth(), textureHeight(), textureWidth(), textureHeight());
    }
}
