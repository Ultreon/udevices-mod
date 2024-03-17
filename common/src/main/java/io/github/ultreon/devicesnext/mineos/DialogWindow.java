package com.ultreon.mods.lib.client.devicetest;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector2i;

public class DialogWindow extends Window {
    @Nullable
    Window root;
    Window parent;
    private boolean holdingTitle;
    private @Nullable Vector2d holdingTitleFrom;
    private @Nullable Vector2d holdingTitleSince;

    public DialogWindow(@NotNull Application application , int width, int height, String title) {
        super(application, 0, 0, width, height, title);
    }

    public DialogWindow(@NotNull Application application, int width, int height, Component title) {
        super(application, 0, 0, width, height, title);
    }

    @Override
    public void minimize() {

    }

    @Override
    public boolean close() {
        if (this.parent == null) return super.close();
        return this.parent.closeDialog(this);
    }

    @Override
    boolean _isValid() {
        if (this.parent == null) return super._isValid();
        return this.parent.getDialog() == this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean b = super.mouseClicked(mouseX, mouseY, button);
        if (!b && this.isMouseOverTitle(mouseX, mouseY) && button == 0 && this.root != null) {
            this.holdingTitleFrom = new Vector2d(mouseX, mouseY);
            this.holdingTitleSince = new Vector2d(root.getX(), root.getY());
            this.holdingTitle = true;
            return true;
        }
        return b;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.holdingTitle = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.holdingTitle && this.holdingTitleFrom != null && this.holdingTitleSince != null && this.root != null) {
            var fromX = (int) this.holdingTitleFrom.x;
            var fromY = (int) this.holdingTitleFrom.y;
            var sinceX = (int) this.holdingTitleSince.x;
            var sinceY = (int) this.holdingTitleSince.y;

            var deltaX = (int) mouseX - fromX;
            var deltaY = (int) mouseY - fromY;

            root.setX(sinceX + deltaX);
            root.setY(sinceY + deltaY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    @Nullable
    protected Vector2i getForcePosition() {
        Window parent = this.parent;
        if (parent == null) return null;
        return new Vector2i(parent.getX() + (parent.getWidth() - this.width) / 2, parent.getY() + (parent.getHeight() - this.height) / 2);
    }

    public Window getParent() {
        return this.parent;
    }
}
