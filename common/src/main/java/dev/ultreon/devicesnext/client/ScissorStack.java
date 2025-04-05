package dev.ultreon.devicesnext.client;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector3;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ultreon.devicesnext.mineos.gui.GpuRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Stack;
import net.minecraft.client.Minecraft;

public class ScissorStack {
    private static final Vector3 position = new Vector3();
    public static Stack<Scissor> scissorStack = new Stack<>();

    private static boolean pushScissorTranslated(Batch poseStack, int x, int y, int width, int height) {
        var translation = poseStack.getTransformMatrix().getTranslation(position);
        x += (int) translation.x;
        y += (int) translation.y;

        if (!scissorStack.isEmpty()) {
            var scissor = scissorStack.peek();
            x = Math.max(scissor.x, x);
            y = Math.max(scissor.y, y);
            width = x + width > scissor.x + scissor.width ? scissor.x + scissor.width - x : width;
            height = y + height > scissor.y + scissor.height ? scissor.y + scissor.height - y : height;
        } else {
            GlStateManager._enableScissorTest();
        }

        if (width <= 0 || height <= 0) {
            return false;
        }

        var mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        GlStateManager._scissorBox((int) (x * scale), (int) (mc.getWindow().getHeight() - y * scale - height * scale), (int) Math.max(0, width * scale), (int) Math.max(0, height * scale));
        scissorStack.push(new Scissor(x, y, width, height));
        return true;
    }

    private static void popScissor() {
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }
        restoreScissor();
    }

    private static void restoreScissor() {
        if (!scissorStack.isEmpty()) {
            var scissor = scissorStack.peek();
            var mc = Minecraft.getInstance();
            var scale = mc.getWindow().getGuiScale();
            GlStateManager._scissorBox((int) (scissor.x * scale), (int) (mc.getWindow().getHeight() - scissor.y * scale - scissor.height * scale), (int) Math.max(0, scissor.width * scale), (int) Math.max(0, scissor.height * scale));
        } else {
            GlStateManager._disableScissorTest();
        }
    }

    public static boolean isScissorStackEmpty() {
        return scissorStack.isEmpty();
    }

    /**
     * Do not call! Used for core only.
     */
    public static void clearScissorStack() {
        scissorStack.clear();
    }

    public static Color getPixel(int x, int y) {
        var mc = Minecraft.getInstance();
        var scale = mc.getWindow().getGuiScale();
        var buffer = BufferUtils.createByteBuffer(3);
        RenderSystem.readPixels((int) (x * scale), (int) (mc.getWindow().getHeight() - y * scale - scale), 1, 1, GL11.GL_RGB, GL11.GL_BYTE, buffer);
        return new Color(Math.min(255, buffer.get(0) % 256*2), Math.min(255, buffer.get(1) % 256*2), Math.min(255, buffer.get(2) % 256*2));
    }

    private static boolean pushScissorTranslated(GpuRenderer gfx, int x, int y, int width, int height) {
        return pushScissorTranslated(gfx.getBatch(), x, y, width, height);
    }

    public static void scissor(GpuRenderer graphics, int x, int y, int width, int height, Runnable func) {
        if (pushScissorTranslated(graphics, x, y, width, height)) {
            if (x != 0 || y != 0) {
                graphics.getBatch().setTransformMatrix(graphics.getBatch().getTransformMatrix().translate(x, y, 0));
            }
            try {
                func.run();
            } finally {
                if (x != 0 || y != 0) {
                    graphics.getBatch().setTransformMatrix(graphics.getBatch().getTransformMatrix().translate(-x, -y, 0));
                }
                popScissor();
            }
        }
    }

    public static class Scissor {
        public int x;
        public int y;
        public int width;
        public int height;

        Scissor(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
