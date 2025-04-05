package dev.ultreon.devicesnext.mineos.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ultreon.devicesnext.mineos.Icon;
import dev.ultreon.devicesnext.client.ScissorStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class McIconButton extends McComponent {
    private final List<ClickCallback> onClick = new ArrayList<>();
    private Icon icon;

    public McIconButton(int x, int y, Icon icon) {
        super(x, y, icon.width() + 4, icon.height() + 4, "Icon: " + icon.resource());
        this.icon = icon;
    }

    @Override
    public void render(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
        var background = 0xff444444;
        if (isMouseOver(mouseX, mouseY)) background = 0xff666666;
        if (isHolding()) background = 0xff222222;
        gfx.fill(getX(), getY(), getX() + this.width, getY() + this.height, background);
        ScissorStack.scissor(gfx, getX() + 1, getY() + 1, this.width - 2, this.height - 2, () -> {
            RenderSystem.enableBlend();
            this.icon.render(gfx, this.getX() + 2, this.getY() + 2);
            RenderSystem.disableBlend();
        });
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        this.width = icon.width() + 4;
        this.height = icon.height() + 4;
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
        void onClick(McIconButton button);
    }
}
