package dev.ultreon.devicesnext.mineos.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.IntMap;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.mineos.VirtualComputer;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class VirtualGpu {
    private static final int ERR_CREATE_FAIL = 0x1;
    private static final int ERR_ILLEGAL_ARGUMENT = 0x2;
    private final GpuRenderer gpuRenderer;
    private final VirtualComputer computer;
    private final IntMap<Texture> textures = new IntMap<>();
    private String error;
    private int errno;

    public VirtualGpu(GpuRenderer gpuRenderer, VirtualComputer computer) {
        this.gpuRenderer = gpuRenderer;
        this.computer = computer;
    }

    public String getError() {
        return error;
    }

    public int getErrno() {
        return errno;
    }

    public int createTexture(String path) throws IOException {
        byte[] data;
        try (SeekableByteChannel open = computer.getFS().newByteChannel(Path.of(path), Set.of(StandardOpenOption.READ))) {
            if (open.size() > FileSize.ofKb(64).toBytes()) {
                throw new IOException("Texture too large");
            }

            data = new byte[(int) open.size()];
            open.read(ByteBuffer.wrap(data));
        }
        return createTexture(data);
    }

    public int createTexture(short[] bytes) {
        if (bytes.length > FileSize.ofKb(64).toBytes()) {
            throw new IllegalArgumentException("Texture too large");
        }
        byte[] data = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            data[i] = (byte) bytes[i];
        }

        return createTexture(data);
    }

    private int createTexture(byte[] data) {
        if (RenderSystem.isOnRenderThread()) return createTexture0(data);
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Gdx.app.postRunnable(() -> future.complete(createTexture0(data)));
        return future.join();
    }

    public void deleteTexture(int texture) {
        this.gpuCall(() -> {
            Texture t = textures.remove(texture);
            if (t != null) t.dispose();
            else {
                this.error = "Texture not found";
                this.errno = ERR_ILLEGAL_ARGUMENT;
            }
        });
    }

    public void flip() {
        // What the flip?
    }

    private void gpuCall(Runnable o) {
        if (RenderSystem.isOnRenderThread()) {
            onGpu(o);
            return;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        Gdx.app.postRunnable(() -> {
            try {
                onGpu(o);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        future.join();
    }

    private void onGpu(Runnable o) {
        gpuRenderer.begin();
        o.run();
        gpuRenderer.end();
    }

    public void drawTexture(int texture, float x, float y, float width, float height) {
        this.gpuCall(() -> this.gpuRenderer.blit(textures.get(texture), x, y, width, height));
    }

    @ApiStatus.Experimental
    public void drawTexture(int texture, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight) {
        UDevicesMod.LOGGER.warn("Experimental access to blit(int texture, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight)");
        this.gpuCall(() -> this.gpuRenderer.blit(textures.get(texture), x, y, width, height, u, v, uWidth, vHeight));
    }

    @ApiStatus.Experimental
    public void drawTexture(int texture, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight, float texWidth, float texHeight) {
        UDevicesMod.LOGGER.warn("Experimental access to blit(int texture, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight, float texWidth, float texHeight)");
        this.gpuCall(() -> this.gpuRenderer.blit(textures.get(texture), x, y, width, height, u, v, uWidth, vHeight, texWidth, texHeight));
    }

    public void drawTexture(int texture, int x, int y, int width, int height) {
        this.gpuCall(() -> this.gpuRenderer.blit(textures.get(texture), x, y, width, height));
    }

    @ApiStatus.Experimental
    public void drawTexture(int texture, int x, int y, int width, int height, int u, int v, int uWidth, int vHeight) {
        UDevicesMod.LOGGER.warn("Experimental access to blit(int texture, int x, int y, int width, int height, int u, int v, int uWidth, int vHeight)");
        this.gpuCall(() -> this.gpuRenderer.blit(textures.get(texture), x, y, width, height, u, v, uWidth, vHeight));
    }

    @ApiStatus.Experimental
    public void drawTexture(int texture, int x, int y, int width, int height, int u, int v, int uWidth, int vHeight, int texWidth, int texHeight) {
        UDevicesMod.LOGGER.warn("Experimental access to blit(int texture, int x, int y, int width, int height, int u, int v, int uWidth, int vHeight, int texWidth, int texHeight)");
        this.gpuCall(() -> this.gpuRenderer.blit(textures.get(texture), x, y, width, height, u, v, uWidth, vHeight, texWidth, texHeight));
    }

    public void fill(float x, float y, float width, float height, int color) {
        this.gpuCall(() -> this.gpuRenderer.fill(x, y, width, height, color));
    }

    public void rect(float x, float y, float width, float height, int color) {
        this.gpuCall(() -> this.gpuRenderer.renderOutline(x, y, width, height, color));
    }

    public void clear() {
        this.gpuCall(() -> this.gpuRenderer.clear());
    }

    private int createTexture0(byte[] data) {
        try {
            ByteBuffer buffer = BufferUtils.newByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            Pixmap pixmap = new Pixmap(buffer, 0, data.length);
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            this.textures.put(texture.getTextureObjectHandle(), texture);
            return texture.getTextureObjectHandle();
        } catch (Exception e) {
            this.error("Failed to create texture", ERR_CREATE_FAIL, e);
            return -1;
        }
    }

    private void error(String error, int errno, Exception exception) {
        UDevicesMod.LOGGER.error(error, exception);
        this.error = error;
        this.errno = errno;
    }
}
