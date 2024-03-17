package com.ultreon.mods.lib.client.devicetest.gui;

import com.ultreon.mods.lib.client.devicetest.Icon;
import com.ultreon.mods.lib.util.ScissorStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class McButton extends McComponent {
    private final List<ClickCallback> onClick = new ArrayList<>();
    private Icon icon;

    public McButton(int x, int y, int width, int height, String text) {
        this(x, y, width, height, Component.literal(text));
    }

    public McButton(int x, int y, int width, int height, Component message) {
        this(x, y, width, height, message, null);
    }

    public McButton(int i, int i1, int i2, int i3, String text, Icon icon) {
        this(i, i1, i2, i3, Component.literal(text), icon);
    }

    public McButton(int x, int y, int width, int height, Component message, Icon icon) {
        super(x, y, width, height, message);
        this.icon = icon;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        var background = 0xff444444;
        if (isMouseOver(mouseX, mouseY)) background = 0xff666666;
        if (isHolding()) background = 0xff222222;
        gfx.fill(getX(), getY(), getX() + width, getY() + height, background);
        ScissorStack.pushScissorTranslated(gfx, getX() + 1, getY() + 1, width - 2, height - 2);
        {
            if (this.icon != null) {
                this.icon.render(gfx, getX() + 2, getY() + 2, 11, 11);
                this.renderScrollingString(gfx, font, getX() + 14, getY() + 1, this.getWidth() - 15, this.getHeight() - 2, 0xffffffff);
            } else {
                this.renderScrollingString(gfx, font, 1, 0xffffffff);
            }
        }
        ScissorStack.popScissor();
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
