package com.ultreon.mods.lib.client.devicetest.gui;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.ultreon.mods.lib.client.gui.MoreGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class McContainer extends McComponent {
    private final List<McComponent> children = new ArrayList<>();
    private Insets border = new Insets(0, 0, 0, 0);
    private int borderColor = 0x00000000;
    private McComponent focused;
    public McContainer(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        var innerX = getX() + getBorder().left;
        var innerY = getY() + getBorder().top;
        MoreGuiGraphics.subInstance(gfx, innerX, innerY, getWidth() + getBorder().left, getHeight() + getBorder().top, () -> {
            var translatedX = mouseX - getX() - this.getBorder().left;
            var translatedY = mouseY - getY() - this.getBorder().top;
            renderContents(gfx, translatedX, translatedY, partialTicks);
        });
        super.render(gfx, mouseX, mouseY, partialTicks);
    }

    private void renderContents(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        for (var child : children) {
            child.render(gfx, mouseX, mouseY, partialTicks);
        }
    }

    @CanIgnoreReturnValue
    @Contract("_->param1")
    public <T extends @NotNull McComponent> @NotNull T add(@NotNull T widget) {
        widget.parent = this;
        this.children.add(widget);
        return widget;
    }

    @CanIgnoreReturnValue
    public boolean remove(@NotNull McComponent widget) {
        widget.parent = null;
        return this.children.remove(widget);
    }

    public void clearWidgets() {
        this.children.clear();
    }

    public Collection<McComponent> children() {
        return Collections.unmodifiableCollection(this.children);
    }
    
    protected Insets getBorder() {
        return this.border;
    }

    protected void setBorder(Insets border) {
        this.border = border;
    }

    protected int getBorderColor() {
        return this.borderColor;
    }

    protected void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public McComponent getFocused() {
        return this.focused;
    }

    public void setFocused(McComponent focused) {
        this.focused = focused;
    }

    @Override
    public boolean preMouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        var translatedX = mouseX - getX() - this.getBorder().left;
        var translatedY = mouseY - getY() - this.getBorder().top;
        for (var child : this.children) {
            if (child.isMouseOver(translatedX, translatedY)) {
                if (child.preMouseClicked(translatedX, translatedY, button)) return true;
                break;
            }
        }
        return super.preMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        var translatedX = mouseX - getX() - this.getBorder().left;
        var translatedY = mouseY - getY() - this.getBorder().top;
        for (var child : this.children) {
            if (child.isMouseOver(translatedX, translatedY)) {
                child.mouseClicked(translatedX, translatedY, button);
                break;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isHolding()) {
            var translatedX = mouseX - getX() - this.getBorder().left;
            var translatedY = mouseY - getY() - this.getBorder().top;
            for (var child : this.children) {
                if (child.isMouseOver(translatedX, translatedY)) {
                    child.mouseReleased(translatedX, translatedY, button);
                    break;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        var translatedX = mouseX - getX() - this.getBorder().left;
        var translatedY = mouseY - getY() - this.getBorder().top;
        var translatedDragX = dragX - getX() - this.getBorder().left;
        var translatedDragY = dragY - getY() - this.getBorder().top;
        for (var child : this.children) {
            if (child.isMouseOver(translatedX, translatedY)) {
                child.mouseDragged(translatedX, translatedY, button, translatedDragX, translatedDragY);
                break;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        var translatedX = mouseX - getX() - this.getBorder().left;
        var translatedY = mouseY - getY() - this.getBorder().top;
        for (var child : this.children) {
            if (child.isMouseOver(translatedX, translatedY)) {
                child.mouseScrolled(translatedX, translatedY, amountX, amountY);
                break;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return;
        }

        var translatedX = mouseX - getX() - this.getBorder().left;
        var translatedY = mouseY - getY() - this.getBorder().top;
        for (var child : this.children) {
            if (child.isMouseOver(translatedX, translatedY)) {
                child.mouseMoved(translatedX, translatedY);
                break;
            }
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isFocused()) {
            final var focused = this.focused;
            if (focused != null) {
                focused.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (isFocused()) {
            final var focused = this.focused;
            if (focused != null) {
                focused.keyReleased(keyCode, scanCode, modifiers);
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (isFocused()) {
            final var focused = this.focused;
            if (focused != null) {
                focused.charTyped(codePoint, modifiers);
            }
        }
        return false;
    }
}
