package dev.ultreon.devicesnext.mineos;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.ultreon.devicesnext.mineos.gui.GpuRenderer;
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

    default void render(GpuRenderer gfx, int x, int y) {
//        ResourceLocation resource = this.resource();
//        gfx.blit(resource, x, y, width(), height(), u(), v(), uWidth(), vHeight(), texWidth(), texHeight());
    }

    default void render(GpuRenderer gfx, int x, int y, int width, int height) {
//        ResourceLocation resource = this.resource();
//        gfx.blit(resource, x, y, width, height, u(), v(), uWidth(), vHeight(), texWidth(), texHeight());
    }

    TextureRegion texture();
}
