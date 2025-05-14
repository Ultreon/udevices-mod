package dev.ultreon.devicesnext.mineos.gui;

import dev.ultreon.devicesnext.api.OperatingSystem;
import dev.ultreon.devicesnext.client.ScissorStack;
import dev.ultreon.devicesnext.mineos.Insets;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public abstract class McComponent extends BaseWidget {
    private McContextMenu contextMenu;
    private boolean holding;
    @Nullable
    McContainer parent = null;

    public McComponent(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void renderComponent(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {

    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderComponent(gfx, mouseX, mouseY, partialTicks);
    }

    public final int getScreenWidth() {
        if (OperatingSystem.get() == null) {
            return 0;
        }
        return OperatingSystem.get().getWidth();
    }

    public final int getScreenHeight() {
        if (OperatingSystem.get() == null) {
            return 0;
        }
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

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationOutput) {
        defaultButtonNarrationText(narrationOutput);
    }

    public void setHeight(int height) {
        this.height = height;
    }

    protected void renderScrollingString(@NotNull GuiGraphics guiGraphics, @NotNull Font font, int inset, int color) {
        int l = this.getWidth() - inset;
        renderScrollingString(guiGraphics, font, this.getMessage(), inset, 0, l, this.getHeight(), color);
    }

    protected static void renderScrollingString(GuiGraphics gfx, Font font, @NotNull Component message, int x, int y, int width, int height, int color) {
        int textWidth = font.width(message);
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
            ScissorStack.scissor(gfx, x, y, width, height, () -> {
                gfx.drawString(font, message, x - (int) actualTextX, textY, color);
            });
        } else {
            gfx.drawCenteredString(font, message, (x + width) / 2, textY, color);
        }
    }

    public void setContextMenu(McContextMenu contextMenu) {
        this.contextMenu = contextMenu;
    }

    public McContextMenu getContextMenu() {
        return this.contextMenu;
    }

    public void openContextMenu(int mouseX, int mouseY) {
        this.contextMenu.open(mouseX, mouseY);
    }

    public void closeContextMenu() {
        this.contextMenu.close();
    }
}
