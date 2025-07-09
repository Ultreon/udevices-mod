package dev.ultreon.devicesnext.mineos.gui;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ultreon.devicesnext.mineos.VirtualComputer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class GpuRenderer {
    private final VirtualGpu vGpu;
    private final FrameBufferObject fbo;
    private boolean blend = false;
    private boolean depthMask = true;
    private boolean cull = false;
    private boolean scissorTest = false;
    private float red;
    private float green;
    private float blue;
    private float alpha;

    public GpuRenderer(VirtualComputer computer) {
        vGpu = new VirtualGpu(this, computer);
        fbo = new FrameBufferObject(Minecraft.getInstance().getTextureManager(), 400, 300);
    }

    public void delete() {
        fbo.delete();
    }

    public void translate(int x, int y, int z) {
        renderCall(() -> {
            
        });
    }

    public void translate(int x, int y) {
        renderCall(() -> {

        });
    }

    public void scale(int x, int y, int z) {
        renderCall(() -> {

        });
    }

    public void scale(float x, float y, float z) {
        renderCall(() -> {

        });
    }

    public void scale(float x, float y) {
        renderCall(() -> {

        });
    }

    public void drawString(String text, int x, int y, int color, boolean shadow) {
        renderCall(() -> {
            Minecraft instance = Minecraft.getInstance();
            Font font = instance.font;

            GuiGraphics graphics = new GuiGraphics(instance, instance.renderBuffers().crumblingBufferSource());
            graphics.drawString(font, text, x, y, color, shadow);
        });
    }

    public void drawString(String text, int x, int y, int color) {
        renderCall(() -> {
            Minecraft instance = Minecraft.getInstance();
            Font font = instance.font;

            GuiGraphics graphics = new GuiGraphics(instance, instance.renderBuffers().crumblingBufferSource());
            graphics.drawString(font, text, x, y, color);
        });
    }

    public void drawCenteredString(String text, int x, int y, int color) {
        renderCall(() -> {
            Minecraft instance = Minecraft.getInstance();
            Font font = instance.font;

            GuiGraphics graphics = new GuiGraphics(instance, instance.renderBuffers().crumblingBufferSource());
            graphics.drawCenteredString(font, text, x, y, color);
        });
    }

    public void drawCenteredString(String text, int x, int y, int color, boolean shadow) {
        renderCall(() -> {
            Minecraft instance = Minecraft.getInstance();
            Font font = instance.font;

            GuiGraphics graphics = new GuiGraphics(instance, instance.renderBuffers().crumblingBufferSource());
            graphics.drawString(font, text, x - font.width(text) / 2, y, color, shadow);
        });
    }

    public void setColor(float r, float g, float b, float a) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.alpha = a;
    }

    public int width(GpuFont font, String message) {
        return font.mc().width(message);
    }

    public String substrByWidth(GpuFont font, String message, int width) {
        int len = message.length();
        for (int i = 0; i < len; i++) {
            int w = this.width(font, message.substring(0, i + 1) + "...");
            if (w > width) {
                return message.substring(0, i) + "...";
            }
        }
        return message;
    }

    public void drawCenteredStringWithoutShadow(GpuFont font, String s, int x, int y, int color) {
        renderCall(() -> {
            Minecraft instance = Minecraft.getInstance();

            GuiGraphics graphics = new GuiGraphics(instance, instance.renderBuffers().crumblingBufferSource());
            graphics.drawString(font.mc(), s, x, y, color, false);
        });
    }

    private void renderCall(RenderCall call) {
        RenderSystem.recordRenderCall(() -> {
            int width = Minecraft.getInstance().getWindow().getWidth();
            int height = Minecraft.getInstance().getWindow().getWidth();
            fbo.bind();
            if (blend) GlStateManager._enableBlend();
            else GlStateManager._disableBlend();
            if (cull) GlStateManager._enableCull();
            else GlStateManager._disableCull();
            if (scissorTest) GlStateManager._enableScissorTest();
            else GlStateManager._disableScissorTest();
            RenderSystem.setShaderColor(red, green, blue, alpha);
            GlStateManager._depthMask(depthMask);
            
            call.execute();
            
            FrameBufferObject.unbind(width, height);
        });
    }

    public boolean isBlend() {
        return blend;
    }

    public void setBlend(boolean blend) {
        this.blend = blend;
    }

    public boolean isDepthMask() {
        return depthMask;
    }

    public void setDepthMask(boolean depthMask) {
        this.depthMask = depthMask;
    }

    public boolean isCull() {
        return cull;
    }

    public void setCull(boolean cull) {
        this.cull = cull;
    }

    public boolean isScissorTest() {
        return scissorTest;
    }

    public void setScissorTest(boolean scissorTest) {
        this.scissorTest = scissorTest;
    }

    public VirtualGpu getVGpu() {
        return vGpu;
    }

    public void clear() {
        renderCall(() -> {
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        });
    }

    public void clear(float r, float g, float b, float a) {
        renderCall(() -> {
            glClearColor(r, g, b, a);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        });
    }

    public void reconnectDisplay(int width, int height) {
        
    }

    public FrameBufferObject getDisplayBuffer() {
        return this.fbo;
    }

    public void fill(int x, int y, int width, int height, int argb) {
        renderCall(() -> {
            GuiGraphics graphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().crumblingBufferSource());
            graphics.fill(x, y, width, height, argb);
        });   
    }

    public FrameBufferObject getFbo() {
        return fbo;
    }
}
