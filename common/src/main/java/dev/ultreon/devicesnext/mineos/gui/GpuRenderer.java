package dev.ultreon.devicesnext.mineos.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.google.common.collect.Lists;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.mineos.VirtualComputer;
import org.jetbrains.annotations.ApiStatus;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GpuRenderer implements Disposable {
    private Batch batch;
    private ShapeDrawer shapes;
    private Texture whiteTexture;
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final Color colorTmp = new Color(1, 1, 1, 1);
    private final VirtualGpu vGpu;
    private String error;
    private final List<Runnable> tasks = new CopyOnWriteArrayList<>();
    private DualFrameBuffer fbo;
    private boolean doFlip;

    public GpuRenderer(VirtualComputer computer) {
        vGpu = new VirtualGpu(this, computer);
    }

    public void begin() {
        batch.begin();
    }

    public void end() {
        batch.end();
    }

    public Batch getBatch() {
        return batch;
    }

    @Override
    public void dispose() {
        try {
            batch.end();
        } catch (NullPointerException ignored) {
            // Ignored
        }
        batch.dispose();
        whiteTexture.dispose();
    }

    public void blit(TextureRegion region, float x, float y, float width, float height) {
        batch.setColor(Color.WHITE);
        batch.draw(region, x, y, width, height);
    }

    public void blit(Texture texture, float x, float y, float width, float height) {
        batch.setColor(Color.WHITE);
        if (checkNullTex(texture)) return;
        batch.draw(texture, x, y, width, height);
    }

    private boolean checkNullTex(Texture texture) {
        if (texture == null) {
            this.error = "Texture is null";
            return true;
        }
        return false;
    }

    public void blit(Texture texture, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight) {
        batch.setColor(Color.WHITE);
        if (checkNullTex(texture)) return;
        batch.draw(texture, x, y, width, height, u / texture.getWidth(), v / texture.getHeight(), uWidth / texture.getWidth(), vHeight / texture.getHeight());
    }

    @ApiStatus.Experimental
    public void blit(Texture texture, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight, float texWidth, float texHeight) {
        batch.setColor(Color.WHITE);
        if (checkNullTex(texture)) return;
        batch.draw(texture, x, y, width, height, u / (texWidth / texture.getWidth()), v / (texHeight / texture.getHeight()), uWidth / (texWidth / texture.getWidth()), vHeight / (texHeight / texture.getHeight()));
    }

    public void blit(TextureRegion region, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight) {
        batch.setColor(Color.WHITE);
        // TODO: fix
    }

    public void blit(TextureRegion region, float x, float y) {
        batch.setColor(Color.WHITE);
        batch.draw(region, x, y);
    }

    public void fill(float x, float y, float width, float height, int color) {
        Color.argb8888ToColor(this.colorTmp, color);
        if (colorTmp.a == 0) colorTmp.a = 1;
        shapes.filledRectangle(x, y, width, height, colorTmp);
    }

    public void fill(float x, float y, float width, float height, float r, float g, float b, float a) {
        colorTmp.set(r, g, b, a);
        if (colorTmp.a == 0) colorTmp.a = 1;
        shapes.filledRectangle(x, y, width, height, colorTmp);
    }

    public void renderOutline(float x, float y, float width, float height, int color) {
        Color.argb8888ToColor(this.colorTmp, color);
        shapes.rectangle(x, y, width, height, colorTmp);
    }

    public void renderOutline(float x, float y, float width, float height, float r, float g, float b, float a) {
        shapes.rectangle(x, y, width, height, colorTmp);
    }

    public void translate(int x, int y, int z) {
        batch.setTransformMatrix(batch.getTransformMatrix().translate(x, y, z));
    }

    public void translate(int x, int y) {
        batch.setTransformMatrix(batch.getTransformMatrix().translate(x, y, 0));
    }

    public void scale(int x, int y, int z) {
        batch.setTransformMatrix(batch.getTransformMatrix().scale(x, y, z));
    }

    public void scale(float x, float y, float z) {
        batch.setTransformMatrix(batch.getTransformMatrix().scale(x, y, z));
    }

    public void scale(float x, float y) {
        batch.setTransformMatrix(batch.getTransformMatrix().scale(x, y, 1));
    }

    public void drawString(String text, int x, int y, int color, boolean shadow) {
        Color.argb8888ToColor(this.colorTmp, color);
        batch.setColor(colorTmp);
        this.font.draw(batch, text, x, y);
        if (shadow) {
            this.batch.setColor((float) (color >> 16 & 0xFF) / 511f, (float) (color >> 8 & 0xFF) / 511f, (float) (color & 0xFF) / 511f, (float) (color >> 24 & 0xFF) / 511f);
            this.font.draw(batch, text, x + 1, y + 1);
        }
    }

    public void drawString(String text, int x, int y, int color) {
        Color.argb8888ToColor(this.colorTmp, color);
        batch.setColor(colorTmp);
        this.font.draw(batch, text, x, y);
    }

    public void drawCenteredString(String text, int x, int y, int color) {
        Color.argb8888ToColor(this.colorTmp, color);
        batch.setColor(colorTmp);
        this.layout.setText(this.font, text);
        this.font.draw(batch, this.layout, x - this.layout.width / 2, y);
    }

    public void drawCenteredString(String text, int x, int y, int color, boolean shadow) {
        Color.argb8888ToColor(this.colorTmp, color);
        batch.setColor(colorTmp);
        this.layout.setText(this.font, text);
        this.font.draw(batch, this.layout, x - this.layout.width / 2, y);
        if (shadow) {
            this.batch.setColor((float) (color >> 16 & 0xFF) / 511f, (float) (color >> 8 & 0xFF) / 511f, (float) (color & 0xFF) / 511f, (float) (color >> 24 & 0xFF) / 511f);
            this.font.draw(batch, this.layout, x - this.layout.width / 2 + 1, y + 1);
        }
    }

    public void setColor(float r, float g, float b, float a) {
        this.batch.setColor(r, g, b, a);
    }

    public int width(BitmapFont font, String message) {
        this.layout.setText(font, message);
        return (int) this.layout.width;
    }

    public String substrByWidth(BitmapFont font, String message, int width) {
        int len = message.length();
        for (int i = 0; i < len; i++) {
            int w = this.width(font, message.substring(0, i + 1) + "...");
            if (w > width) {
                return message.substring(0, i) + "...";
            }
        }
        return message;
    }

    public void drawCenteredStringWithoutShadow(BitmapFont font, String s, int x, int y, int color) {
        Color.argb8888ToColor(this.colorTmp, color);
        batch.setColor(colorTmp);
        this.layout.setText(font, s);
        font.draw(batch, this.layout, x - this.layout.width / 2, y);
    }

    public void disableBlend() {
        this.batch.disableBlending();
    }

    public void enableBlend() {
        this.batch.enableBlending();
    }

    public void blit(TextureRegion resource, int x, int y, int width, int height, int u, int v, int uWidth, int vHeight, int texWidth, int texHeight) {
//        this.batch.draw(resource, x, y, width, height, u, v, uWidth, vHeight, texWidth, texHeight);
        // TODO
    }

    public VirtualGpu getVGpu() {
        return vGpu;
    }

    public void clear() {
        ScreenUtils.clear(0, 0, 0, 1);
    }

    public void clear(int r, int g, int b, int a) {
        ScreenUtils.clear(r, g, b, a);
    }

    public void postRunnable(Runnable runnable) {
        this.tasks.add(runnable);
    }

    public void runTasks(Batch batch, ShapeDrawer shapes, DualFrameBuffer fbo) {
        this.fbo = fbo;
        List<Runnable> tasks1 = Lists.newArrayList(this.tasks);
        for (Runnable task : tasks1) {
            try {
                this.batch = batch;
                this.shapes = shapes;
                task.run();
            } catch (Exception e) {
                UDevicesMod.LOGGER.error("GPU task failed", e);
            }
            if (Thread.currentThread().isInterrupted()) {
                UDevicesMod.LOGGER.error("GPU task interrupted");
                break;
            }
            if (this.error != null) {
                UDevicesMod.LOGGER.error("GPU task error: {}", this.error);
                break;
            }
        }
        tasks1.clear();
    }

    public String getError() {
        return error;
    }

    public void flip() {
        fbo.flip();
    }
}
