package dev.ultreon.devicesnext.mineos.gui;

import dev.ultreon.devicesnext.client.ScissorStack;
import dev.ultreon.devicesnext.mineos.Icon;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class McButton extends McComponent {
    private final List<ClickCallback> onClick = new ArrayList<>();
    private Icon icon;

    public McButton(int x, int y, int width, int height, String text) {
        this(x, y, width, height, text, null);
    }

    public McButton(int i, int i1, int i2, int i3, String text, Icon icon) {
        super(i, i1, i2, i3, text);
        this.icon = icon;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public void render(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
        var background = 0xff444444;
        if (isMouseOver(mouseX, mouseY)) background = 0xff666666;
        if (isHolding()) background = 0xff222222;
        gfx.fill(0, 0, width, height, background);
        ScissorStack.scissor(gfx, 1, 1, width - 2, height - 2, () -> {
            if (this.icon != null) {
                this.icon.render(gfx, 1, 1, 11, 11);
                renderScrollingString(gfx, font, this.getMessage(), 14, 1, this.getWidth() - 15, this.getHeight() - 2, 0xffffffff);
            } else {
                this.renderScrollingString(gfx, font, 1, 0xffffffff);
            }
        });
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (var callback : onClick) {
                callback.onClick(this);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void addClickHandler(ClickCallback onClick) {
        this.onClick.add(onClick);
    }

    public void removeClickHandler(ClickCallback onClick) {
        this.onClick.remove(onClick);
    }

    @FunctionalInterface
    public interface ClickCallback {
        void onClick(McButton button);
    }
}
