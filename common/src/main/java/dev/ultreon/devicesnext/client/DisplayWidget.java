package dev.ultreon.devicesnext.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DisplayWidget extends AbstractWidget {
    private static final ResourceLocation DYNAMIC_ID = ResourceLocation.parse("udevices:dynamic/display_framebuffer");
    private final GuiGraphics gfx;
    private final MultiBufferSource.BufferSource bufferSource;
    private final Minecraft minecraft;
    private final Font font;
    private FrameBuffer frameBuffer;
    private boolean bound = false;

    public DisplayWidget(Screen screen, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());

        minecraft = Minecraft.getInstance();
        font = minecraft.font;
        frameBuffer = new FrameBuffer(screen.width, screen.height);

        minecraft.getTextureManager().register(DYNAMIC_ID, frameBuffer);

        bufferSource = minecraft.renderBuffers().bufferSource();
        gfx = new GuiGraphics(minecraft, bufferSource);
    }

    public void close() {
        unbind();
        frameBuffer.close();
        minecraft.getTextureManager().release(DYNAMIC_ID);
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
        this.frameBuffer = new FrameBuffer(width, height);
    }

    public void fillRect(int x, int y, int width, int height, int color) {
        bind();
        gfx.fill(x, y, x + width, y + height, color);
        bufferSource.endBatch();
        unbind();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blit(DYNAMIC_ID, getX(), getY(), width, height, 0, 0, width, height, width, height);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // No-op
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
