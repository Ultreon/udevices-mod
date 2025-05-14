package dev.ultreon.devicesnext.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Stack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ScissorStack {
    public static Stack<Scissor> scissorStack = new Stack<>();

    private static boolean pushScissorTranslated(PoseStack poseStack, int x, int y, int width, int height) {
        var translation = poseStack.last().pose().getTranslation(new Vector3f());
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
        var resolution = new ScaledResolution(mc);
        var scale = resolution.getScaleFactor();
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
            var resolution = new ScaledResolution(mc);
            var scale = resolution.getScaleFactor();
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
        var resolution = new ScaledResolution(mc);
        var scale = resolution.getScaleFactor();
        var buffer = BufferUtils.createByteBuffer(3);
        RenderSystem.readPixels((int) (x * scale), (int) (mc.getWindow().getHeight() - y * scale - scale), 1, 1, GL11.GL_RGB, GL11.GL_BYTE, buffer);
        return new Color(Math.min(255, buffer.get(0) % 256*2), Math.min(255, buffer.get(1) % 256*2), Math.min(255, buffer.get(2) % 256*2));
    }

    private static boolean pushScissorTranslated(GuiGraphics gfx, int x, int y, int width, int height) {
        return pushScissorTranslated(gfx.pose(), x, y, width, height);
    }

    public static void scissor(GuiGraphics graphics, int x, int y, int width, int height, Runnable func) {
        if (pushScissorTranslated(graphics, x, y, width, height)) {
            if (x != 0 || y != 0) {
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
            }
            try {
                func.run();
            } finally {
                if (x != 0 || y != 0) graphics.pose().popPose();
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
