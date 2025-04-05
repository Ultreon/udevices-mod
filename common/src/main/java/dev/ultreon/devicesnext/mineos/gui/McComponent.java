package dev.ultreon.devicesnext.mineos.gui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import dev.ultreon.devicesnext.api.OperatingSystem;
import dev.ultreon.devicesnext.client.ScissorStack;
import dev.ultreon.devicesnext.mineos.Insets;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public abstract class McComponent extends McWidget {
    private McContextMenu contextMenu;
    private boolean holding;
    @Nullable
    McContainer parent = null;
    private final GlyphLayout layout = new GlyphLayout();

    public McComponent(int x, int y, int width, int height, String message) {
        super(x, y, width, height, message);
    }

    @Override
    public void render(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {

    }

    public final int getScreenWidth() {
        return OperatingSystem.get().getWidth();
    }

    public final int getScreenHeight() {
        return OperatingSystem.get().getHeight();
    }

    public final Insets getWmBorder() {
        return OperatingSystem.get().getClientAreaInsets();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) this.holding = true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean preMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.holding = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean isHolding() {
        return holding;
    }

    protected void renderScrollingString(@NotNull GpuRenderer guiGraphics, @NotNull BitmapFont font, int inset, int color) {
        int l = this.getWidth() - inset;
        renderScrollingString(guiGraphics, font, this.getMessage(), inset, 0, l, this.getHeight(), color);
    }

    protected void renderScrollingString(GpuRenderer gfx, BitmapFont font, @NotNull String message, int x, int y, int width, int height, int color) {
        int textWidth = width(message);
        int endY = y + height;
        Objects.requireNonNull(font);
        int textY = (endY - 9) / 2 + 1;
        int textX = width - x;
        if (textWidth > textX) {
            int endX = textWidth - textX;
            double curSeconds = (double) Util.getMillis() / (double) 1000f;
            double tmp = Math.max((double) endX * (double) 0.5f, 3f);
            double textOffset = Math.sin((Math.PI / 2.0) * Math.cos((Math.PI * 2.0) * curSeconds / tmp)) / (double) 2f + (double) 0.5f;
            double actualTextX = Mth.lerp(textOffset, 0f, endX);
            ScissorStack.scissor(gfx, x, y, width, height, () -> gfx.drawString(message, x - (int) actualTextX, textY, color));
        } else {
            gfx.drawCenteredString(message, (x + width) / 2, textY, color);
        }
    }

    private int width(@NotNull String message) {
        layout.setText(font, message);
        return (int) layout.width;
    }
}
