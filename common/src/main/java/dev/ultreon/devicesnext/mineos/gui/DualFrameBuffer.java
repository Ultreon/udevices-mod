package dev.ultreon.devicesnext.mineos.gui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import dev.ultreon.mcgdx.GdxMinecraft;
import org.jetbrains.annotations.ApiStatus;

public class DualFrameBuffer implements Disposable {
    private FrameBuffer front;
    private FrameBuffer back;
    private FrameBuffer copy;
    private boolean drawing;

    public DualFrameBuffer(int width, int height) {
        this.front = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        this.back = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        this.copy = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
    }

    @Override
    public void dispose() {
        front.dispose();
        back.dispose();
        copy.dispose();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void resize(int width, int height) {
        var front1 = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        var back1 = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        var copy1 = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);

        front1.begin();
        GdxMinecraft.instance().getBatch().begin();
        GdxMinecraft.instance().getBatch().draw(front.getColorBufferTexture(), 0, 0, front.getWidth(), front.getHeight());
        GdxMinecraft.instance().getBatch().end();
        front1.end();

        back1.begin();
        GdxMinecraft.instance().getBatch().begin();
        GdxMinecraft.instance().getBatch().draw(back.getColorBufferTexture(), 0, 0, back.getWidth(), back.getHeight());
        GdxMinecraft.instance().getBatch().end();
        back1.end();

        copy1.begin();
        GdxMinecraft.instance().getBatch().begin();
        GdxMinecraft.instance().getBatch().draw(copy.getColorBufferTexture(), 0, 0, copy.getWidth(), copy.getHeight());
        GdxMinecraft.instance().getBatch().end();
        copy1.end();

        front.dispose();
        back.dispose();
        copy.dispose();

        front = front1;
        back = back1;
        copy = copy1;
    }

    public void begin() {
        copy.begin();
        GdxMinecraft.instance().getBatch().begin();
        GdxMinecraft.instance().getBatch().draw(back.getColorBufferTexture(), 0, 0, back.getWidth(), back.getHeight());
        GdxMinecraft.instance().getBatch().end();
        copy.end();
        back.begin();
        GdxMinecraft.instance().getBatch().begin();
        GdxMinecraft.instance().getBatch().draw(copy.getColorBufferTexture(), 0, 0, copy.getWidth(), copy.getHeight());
        GdxMinecraft.instance().getBatch().end();
        drawing = true;
    }

    public void end() {
        drawing = false;
        back.end();
    }

    public Texture getTexture() {
        return front.getColorBufferTexture();
    }

    @ApiStatus.Internal
    public Texture getBackTexture() {
        return back.getColorBufferTexture();
    }

    public void flip() {
        if (!drawing) {
            throw new IllegalStateException("Not drawing");
        }

        GdxMinecraft.instance().getBatch().flush();
        front.bind();
        GdxMinecraft.instance().getBatch().draw(back.getColorBufferTexture(), 0, 0, back.getWidth(), back.getHeight());
        GdxMinecraft.instance().getBatch().flush();
        back.bind();
    }

    public FrameBuffer getFront() {
        return front;
    }

    public FrameBuffer getBack() {
        return back;
    }
}
