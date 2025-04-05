package dev.ultreon.devicesnext.mineos.gui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.ultreon.devicesnext.mineos.Icon;
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
    public TextureRegion getResource() {
        return icon.texture();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public void render(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
        TextureRegion resource = getResource();
        gfx.fill(0, 0, this.getWidth(), this.getHeight(), 0xff555555);
        gfx.fill(1, 1, this.getWidth() - 1, this.getHeight() - 1, 0xff333333);

        if (resource == null) {
            // Resource not loaded.
            McImage.drawLoadingIcon(gfx, getWidth() / 2, getHeight() / 2);
            return;
        }

        gfx.blit(resource, 0, 0, getWidth(), getHeight(), 0, 0, textureWidth(), textureHeight(), textureWidth(), textureHeight());
    }
}
