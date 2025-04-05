package dev.ultreon.devicesnext.client;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import dev.ultreon.devicesnext.mineos.gui.GpuRenderer;
import dev.ultreon.devicesnext.mineos.gui.McWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

public class DisplayWidget extends McWidget {
    private final GuiGraphics gfx;
    private final MultiBufferSource.BufferSource bufferSource;
    private final Minecraft minecraft;
    private final Font font;
    private FrameBuffer frameBuffer;
    private boolean bound = false;

    public DisplayWidget(Screen screen, int x, int y, int width, int height) {
        super(x, y, width, height, "");

        minecraft = Minecraft.getInstance();
        font = minecraft.font;
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, screen.width, screen.height, true);

        bufferSource = minecraft.renderBuffers().bufferSource();
        gfx = new GuiGraphics(minecraft, bufferSource);
    }

    public void close() {
        unbind();
        frameBuffer.dispose();
    }

    private void unbind() {
        if (bound) {
            frameBuffer.unbind();
            bound = false;
        }
    }

    private void bind() {
        if (!bound) {
            frameBuffer.bind();
            bound = true;
        }
    }

    public void resize(int width, int height) {
        if (width == this.width && height == this.height) return;
        if (this.frameBuffer != null) this.frameBuffer.dispose();
        this.frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);

        this.width = width;
        this.height = height;
    }

    public void fillRect(int x, int y, int width, int height, int color) {
        bind();
        gfx.fill(x, y, x + width, y + height, color);
        bufferSource.endBatch();
        unbind();
    }

    @Override
    protected void render(@NotNull GpuRenderer guiGraphics, int i, int j, float f) {
        guiGraphics.blit(frameBuffer.getColorBufferTexture(), getX(), getY(), width, height, 0, 0, width, height, width, height);
    }

    public void boxRect(int x, int y, int width, int height, int color) {
        bind();
        gfx.renderOutline(x, y, x + width, y + height, color);
        bufferSource.endBatch();
        unbind();
    }

    public void line(int x1, int y1, int x2, int y2, int color) {
        // TODO
    }

    public void text(String text, int x, int y, int color) {
        bind();
        gfx.drawString(font, text, x, y, color);
        bufferSource.endBatch();
        unbind();
    }
}
