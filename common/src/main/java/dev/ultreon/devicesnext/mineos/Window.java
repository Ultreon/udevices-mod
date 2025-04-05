package dev.ultreon.devicesnext.mineos;

import com.badlogic.gdx.graphics.Texture;
import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.client.ScissorStack;
import dev.ultreon.devicesnext.mineos.gui.GpuRenderer;
import dev.ultreon.devicesnext.mineos.gui.McContainer;
import dev.ultreon.devicesnext.mineos.sizing.IntSize;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector2i;

import java.util.List;
import java.util.function.BooleanSupplier;

@SuppressWarnings("SameParameterValue")
public class Window extends McContainer {
    private static final Texture SHADOW = UDevicesMod.texture("textures/gui/desktop/window_shadow.png");
    private final List<Runnable> onClosed = Lists.newArrayList();
    private final List<BooleanSupplier> onClosing = Lists.newArrayList();
    final Application application;
    boolean topMost;
    boolean bottomMost;
    boolean undecorated;
    WindowManager wm;
    private boolean holdingTitle;
    private @Nullable Vector2d holdingTitleFrom;
    private @Nullable Vector2d holdingTitleSince;
    private boolean maximized;
    private boolean minimized;
    private boolean shadowShown = true;
    private @NotNull Vector2i previousSize;
    private @NotNull Vector2i previousPos;
    private @Nullable DialogWindow dialog;
    private boolean maximizable = true;
    private boolean absolute;
    private DialogWindow pressedDialog;
    private boolean transparent;

    public Window(@NotNull Application application, int x, int y, int width, int height, String title) {
        super(x, y, width, height, title);
        Preconditions.checkNotNull(application, "application");

        if (this.width <= 0 || this.height <= 0)
            throw new IllegalStateException("Size must be positive, got: %d * %d".formatted(width, height));

        this.application = application;
        this.wm = application.wm;

        this.previousSize = new Vector2i(width, height);
        this.previousPos = new Vector2i(x, y);

        setBorderColor(0xff555555);

        setBorder(new Insets(1, 13, 1, 1));
    }

    public void create() {
        this.application.createWindow(this);
    }

    public final Application getApplication() {
        return this.application;
    }

    public void addOnClosedListener(Runnable listener) {
        this.onClosed.add(listener);
    }

    public void removeOnClosedListener(Runnable listener) {
        this.onClosed.remove(listener);
    }

    public void addOnClosingListener(BooleanSupplier listener) {
        this.onClosing.add(listener);
    }

    public void removeOnClosingListener(BooleanSupplier listener) {
        this.onClosing.remove(listener);
    }

    @Override
    public final void render(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
        int mx = mouseX, my = mouseY;

        if (this.isDialogOver(mx, my)) {
            mx = my = Integer.MAX_VALUE;
        }

        this.renderInternal(gfx, mx, my, partialTicks);

        int finalMouseX = mx;
        int finalMouseY = my;

        ScissorStack.scissor(gfx, this.getX() + getBorder().left(), this.getY() + getBorder().top(), this.getWidth(), this.getHeight(), () -> {
            this.renderBackground(gfx, finalMouseX, finalMouseY, partialTicks);
            super.render(gfx, finalMouseX, finalMouseY, partialTicks);
        });

    }

    void renderInternal(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
        if (this.width <= 0 || this.height <= 0)
            throw new IllegalStateException("Size must be positive, got: %d * %d".formatted(width, height));

        this.fixPosition();

        this.renderShadow(gfx, mouseX, mouseY, partialTicks);
        this.renderFrame(gfx, mouseX, mouseY, partialTicks);
    }

    void renderFrame(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
        var message = this.getMessage();

        var titleWidth = Math.min(this.getWidth() - 24, 150);
        var titleHeight = getBorder().top() - 1;

        int borderColor = this.getBorderColor();
        if (getDialog() != null) borderColor = wm.getWindowInactiveColor();
        if (this instanceof DialogWindow dialogWindow && this.dialog == null && dialogWindow.root != null && dialogWindow.root.isFocused())
            borderColor = wm.getWindowActiveColor();

        if (!this.isUndecorated())
            gfx.fill(this.getX(), this.getY(), this.getX() + this.getWidth() + getBorder().left() + getBorder().right(), this.getY() + this.getHeight() + getBorder().top() + getBorder().bottom(), borderColor);
        if (!this.isTransparent())
            gfx.fill(this.getX() + this.getBorder().left(), this.getY() + this.getBorder().top(), this.getX() + this.getWidth() + getBorder().left(), this.getY() + this.getHeight() + getBorder().top(), 0xff333333);

        if (!this.isUndecorated()) {
            if (titleWidth > 2 && titleHeight > this.font.getLineHeight()) {
                // Scissored title text/
                ScissorStack.scissor(gfx, this.getX() + 1, this.getY() + 1, titleWidth, titleHeight, () -> {
                    if (gfx.width(font, message) > titleWidth)
                        gfx.drawString(gfx.substrByWidth(font, message, titleWidth - 4), 0 + 3, 0 + 3, 0xffdddddd, false);
                    else
                        gfx.drawString(message, 0 + 3, 0 + 3, 0xffdddddd, false);
                });
            }

            RenderSystem.enableBlend();
            int tcr = this.getX() + this.getWidth() + 1; // Title Controls Right
            if (this.isMouseOver(mouseX, mouseY, tcr - 23, this.getY() + 1, 23, 12)) {
                gfx.fill(tcr - 23, this.getY() + 1, tcr, this.getY() + 13, 0x33ffffff);
                gfx.drawCenteredStringWithoutShadow(this.font, "×", tcr - 11, this.getY() + 3, 0xffffffff);
            } else gfx.drawCenteredStringWithoutShadow(this.font, "×", tcr - 11, this.getY() + 3, 0xffffffff);

            if (isMaximizable()) {
                tcr -= 23;
                if (this.isMouseOver(mouseX, mouseY, tcr - 23, this.getY() + 1, 23, 12)) {
                    gfx.fill(tcr - 23, this.getY() + 1, tcr, this.getY() + 13, 0x33ffffff);
                    gfx.drawCenteredStringWithoutShadow(this.font, "□", tcr - 11, this.getY() + 3, 0xffffffff);
                } else gfx.drawCenteredStringWithoutShadow(this.font, "□", tcr - 11, this.getY() + 3, 0xffffffff);
            }

            tcr -= 23;
            if (this.isMouseOver(mouseX, mouseY, tcr - 23, this.getY() + 1, 23, 12)) {
                gfx.fill(tcr - 23, this.getY() + 1, tcr, this.getY() + 13, 0x33ffffff);
                gfx.drawCenteredStringWithoutShadow(this.font, "-", tcr - 11, this.getY() + 3, 0xffffffff);
            } else gfx.drawCenteredStringWithoutShadow(this.font, "-", tcr - 11, this.getY() + 3, 0xffffffff);
            RenderSystem.disableBlend();
        }
    }

    void renderShadow(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
        if (this.isShadowShown()) {
            gfx.enableBlend();
            gfx.setColor(1, 1, 1, 0.5f);
            {
                // Corners
                gfx.blit(SHADOW, this.getX() - 9, this.getY() - 9, 9, 9, 0, 0, 9, 9, 256, 256);
                gfx.blit(SHADOW, this.getX() - 9, this.getY() + this.getHeight() + this.getBorder().top() + this.getBorder().bottom(), 9, 9, 0, 11, 9, 9, 256, 256);
                gfx.blit(SHADOW, this.getX() + this.getWidth() + this.getBorder().left() + this.getBorder().right(), this.getY() - 9, 9, 9, 11, 0, 9, 9, 256, 256);
                gfx.blit(SHADOW, this.getX() + this.getWidth() + this.getBorder().left() + this.getBorder().right(), this.getY() + this.getHeight() + this.getBorder().top() + this.getBorder().bottom(), 9, 9, 11, 11, 9, 9, 256, 256);

                // Vertical Parts
                gfx.blit(SHADOW, this.getX() - 9, this.getY(), 9, this.getHeight() + this.getBorder().top() + this.getBorder().bottom(), 0, 10, 9, 1, 256, 256);
                gfx.blit(SHADOW, this.getX() + this.getWidth() + this.getBorder().left() + this.getBorder().right(), this.getY(), 9, this.getHeight() + this.getBorder().top() + this.getBorder().bottom(), 10, 10, 9, 1, 256, 256);

                // Horizontal Parts
                gfx.blit(SHADOW, this.getX(), this.getY() - 9, this.getWidth() + this.getBorder().left() + this.getBorder().right(), 9, 10, 0, 1, 9, 256, 256);
                gfx.blit(SHADOW, this.getX(), this.getY() + this.getHeight() + this.getBorder().top() + this.getBorder().bottom(), this.getWidth() + this.getBorder().left() + this.getBorder().right(), 9, 10, 10, 1, 9, 256, 256);
            }
            gfx.setColor(1, 1, 1, 1);
            gfx.disableBlend();
        }
    }

    protected void renderBackground(GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {

    }

    @Nullable
    protected Vector2i getForcePosition() {
        return null;
    }

    @Nullable
    protected IntSize getForceSize() {
        return null;
    }

    void handleSetActive() {
        this.setBorderColor(wm.getWindowActiveColor());
        this.setFocused(true);
        this.onFocusGained();
    }

    void handleSetInactive() {
        this.onFocusLost();
        this.setFocused(false);
        this.setBorderColor(wm.getWindowInactiveColor());
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return this.active && this.visible && mouseX >= (double) x && mouseY >= (double) y && mouseX < (double) (x + width) && mouseY < (double) (y + height);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double) this.getX() && mouseY >= (double) this.getY() && mouseX < (double) (this.getX() + this.getWidth() + getBorder().left() + getBorder().right()) && mouseY < (double) (this.getY() + this.getHeight() + getBorder().top() + getBorder().bottom());
    }

    public boolean isMouseOverOrDialog(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY) || (dialog != null && dialog.isMouseOverOrDialog(mouseX, mouseY));
    }

    @Override
    public void setX(int x) {
        if (this.absolute) {
            super.setX(x);
            return;
        }

        if (this.maximized) {
            super.setX(-this.getBorder().left());
            return;
        }
        int wmWidth = wm.getWidth();

        // Fix position when the window is outside the WM.
        Insets border = getBorder();
        Insets wmBorder = wm.getClientAreaInsets();

        int right = wmWidth - border.left() - border.right() - wmBorder.left() - wmBorder.right() - this.getWidth();
        if (getX() > right) super.setX(right);
        else if (getX() < wmBorder.left()) super.setX(wmBorder.left());
        else super.setX(x);
    }

    @Override
    public void setY(int y) {
        if (this.absolute) {
            super.setY(y);
            return;
        }

        if (this.maximized) {
            super.setY(-1);
            return;
        }

        int wmHeight = wm.getHeight();

        // Fix position when the window is outside the WM.
        Insets border = getBorder();
        Insets wmBorder = wm.getClientAreaInsets();

        int bottom = wmHeight - border.top() - border.bottom() - wmBorder.top() - wmBorder.bottom() - this.getHeight();
        if (getY() > bottom) super.setY(bottom);
        else if (getY() < wmBorder.top()) super.setY(wmBorder.top());
        else super.setY(y);
    }

    void fixPosition() {
        if (this.absolute) {
            return;
        }

        int wmWidth = wm.getWidth();
        int wmHeight = wm.getHeight();

        // Fix position when the window is outside the WM.
        Insets border = getBorder();
        Insets wmBorder = wm.getClientAreaInsets();

        if (this.maximized) {
            this.setX(wmBorder.bottom());
            this.setY(wmBorder.top());
            this.setWidth(wm.getWidth() - wmBorder.left() - wmBorder.right());
            this.setHeight(wm.getHeight() - wmBorder.top() - wmBorder.bottom() - this.getBorder().top() + 1);
            return;
        }

        int right = wmWidth - border.left() - border.right() - wmBorder.left() - wmBorder.right() - this.getWidth();
        if (getX() > right) super.setX(right);
        else if (getX() < wmBorder.left()) super.setX(wmBorder.left());

        int bottom = wmHeight - border.top() - border.bottom() - wmBorder.top() - wmBorder.bottom() - this.getHeight();
        if (getY() > bottom) super.setY(bottom);
        else if (getY() < wmBorder.top()) super.setY(wmBorder.top());
    }

    public final boolean destroy() {
        if (!this.isValid()) return false;

        this._destroy();
        if (!(this instanceof DialogWindow) && this.wm != null) {
            this.wm.destroyWindow(this); // Destroy window in WM.
            return true;
        }
        return false;
    }

    final void _destroy() {

    }

    public boolean close() {
        if (!isValid()) return false;

        var doClose = true;

        for (var booleanSupplier : this.onClosing) {
            doClose &= booleanSupplier.getAsBoolean();
        }

        if (doClose) {
            return destroy();
        }

        return false;
    }

    public void maximize() {
        previousPos = new Vector2i(getX(), this.getY());
        previousSize = new Vector2i(width, height);
        this.maximized = true;
    }

    public void minimize() {
        this.minimized = true;
    }

    public void restore() {
        this.maximized = false;
        this.minimized = false;

        this.setX(previousPos.x);
        this.setY(previousPos.y);
        this.setWidth(previousSize.x);
        this.setHeight(previousSize.y);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.holdingTitle = false;
        this.pressedDialog = null;

        boolean flag = false;
        DialogWindow dialogWindow = this.dialogWindowAt(mouseX, mouseY);
        if (dialogWindow != null) {
            flag = dialogWindow.mouseReleased(mouseX, mouseY, button);
        }

        return flag || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountY) {
        DialogWindow dialogWindow = this.dialogWindowAt(mouseX, mouseY);
        if (dialogWindow != null) {
            return dialogWindow.mouseScrolled(mouseX, mouseY, amountY);
        }
        return super.mouseScrolled(mouseX, mouseY, amountY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        DialogWindow dialogWindow = this.dialogWindowAt(mouseX, mouseY);
        if (dialogWindow != null) {
            dialogWindow.mouseMoved(mouseX, mouseY);
            return;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean preMouseClicked(double mouseX, double mouseY, int button) {
        DialogWindow dialogWindow = this.dialogWindowAt(mouseX, mouseY);
        if (dialogWindow != null) {
            this.pressedDialog = dialogWindow;
            return dialogWindow.preMouseClicked(mouseX, mouseY, button);
        }
        return super.preMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DialogWindow dialogWindow = this.dialogWindowAt(mouseX, mouseY);
        if (dialogWindow != null) {
            this.pressedDialog = dialogWindow;
            return dialogWindow.mouseClicked(mouseX, mouseY, button);
        }

        if (isMouseOverTitle(mouseX, mouseY)) {
            int tcr = this.getX() + this.getWidth() + 1; // Title Controls Right
            if (this.isMouseOver((int) mouseX, (int) mouseY, tcr - 24, this.getY() + 1, 24, 12)) {
                close();
                return true;
            }

            if (this.isMaximizable()) {
                tcr -= 23;
                if (this.isMouseOver((int) mouseX, (int) mouseY, tcr - 24, this.getY() + 1, 24, 12)) {
                    if (maximized) restore();
                    else maximize();
                    return true;
                }
            }

            tcr -= 23;
            if (this.isMouseOver((int) mouseX, (int) mouseY, tcr - 24, this.getY() + 1, 24, 12)) {
                if (minimized) restore();
                else minimize();
                return true;
            }
            if (button == 0) {
                this.holdingTitleFrom = new Vector2d(mouseX, mouseY);
                this.holdingTitleSince = new Vector2d(getX(), this.getY());
                this.holdingTitle = true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public final boolean isMaximizable() {
        return maximizable;
    }

    public final void setMaximizable(boolean maximizable) {
        this.maximizable = maximizable;
    }

    private @Nullable DialogWindow dialogWindowAt(double mouseX, double mouseY) {
        @Nullable DialogWindow dialog = this.dialog;
        @Nullable DialogWindow dialogAt = null;
        while (dialog != null) {
            if (dialog.isMouseOver(mouseX, mouseY)) dialogAt = dialog;
            dialog = dialog.getDialog();
        }
        return dialogAt;
    }

    private boolean isDialogOver(int mouseX, int mouseY) {
        @Nullable DialogWindow dialog = this.dialog;
        while (dialog != null) {
            if (dialog.isMouseOver(mouseX, mouseY)) return true;
            dialog = dialog.getDialog();
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (pressedDialog != null) {
            return pressedDialog.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        if (this.holdingTitle && this.holdingTitleFrom != null && this.holdingTitleSince != null) {
            var fromX = (int) this.holdingTitleFrom.x;
            var fromY = (int) this.holdingTitleFrom.y;
            var sinceX = (int) this.holdingTitleSince.x;
            var sinceY = (int) this.holdingTitleSince.y;

            var deltaX = (int) mouseX - fromX;
            var deltaY = (int) mouseY - fromY;

            setX(sinceX + deltaX);
            setY(sinceY + deltaY);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    final boolean isMouseOverTitle(double mouseX, double mouseY) {
        return mouseX > this.getX() + 2 && mouseX < this.getX() + width - 2 && mouseY > this.getY() + 2 && mouseY <= this.getY() + getBorder().top();
    }

    @Override
    protected final Insets getBorder() {
        if (this.undecorated)
            return new Insets(0, 0, 0, 0);

        if (this.maximized) {
            Insets b = super.getBorder();
            return new Insets(b.top(), b.left(), b.bottom(), b.right());
        }
        return super.getBorder();
    }

    @Override
    protected final void setBorder(Insets border) {
        if (undecorated) {
            return;
        }
        super.setBorder(border);
    }

    @Override
    protected final int getBorderColor() {
        return super.getBorderColor();
    }

    @Override
    protected final void setBorderColor(int borderColor) {
        super.setBorderColor(borderColor);
    }

    public boolean isTopMost() {
        return topMost;
    }

    public void setTopMost(boolean topMost) {
        this.wm.setTopMost(this, topMost);
    }

    public boolean isBottomMost() {
        return bottomMost;
    }

    public void setBottomMost(boolean bottomMost) {
        this.wm.setBottomMost(this, bottomMost);
    }

    public boolean isUndecorated() {
        return undecorated;
    }

    public boolean isActiveWindow() {
        return this.wm.isActiveWindow(this);
    }

    public void setUndecorated(boolean undecorated) {
        if (undecorated) {
            this.setX(this.getX() - getBorder().left());
            this.setY(this.getY() - getBorder().top());
        } else {
            this.setX(this.getX() + getBorder().left());
            this.setY(this.getY() + getBorder().top());
        }
        this.fixPosition();
        this.undecorated = undecorated;
    }

    public boolean isShadowShown() {
        return shadowShown;
    }

    public void setShadowShown(boolean shadowShown) {
        this.shadowShown = shadowShown;
    }

    public boolean isMinimized() {
        return minimized;
    }

    public boolean isMaximized() {
        return maximized;
    }

    public final boolean isValid() {
        return _isValid();
    }

    boolean _isValid() {
        return this.wm != null && this.wm.isValid(this);
    }

    public void onFocusGained() {

    }

    public void onFocusLost() {

    }

    public void onCreated() {

    }

    public void requestFocus() {
        this.wm.moveToForeground(this);
    }

    public final void openDialog(DialogWindow dialog) {
        if (this.dialog != null && (this.dialog.isValid() || !this.dialog.close())) return;
        dialog.parent = this;
        dialog.root = this instanceof DialogWindow thisDialog ? thisDialog.root : this;
        dialog.wm = wm;
        this.dialog = dialog;
        this.dialog.onCreated();
    }

    @Nullable
    DialogWindow getDialog() {
        return this.dialog;
    }

    protected boolean closeDialog(DialogWindow dialog) {
        if (dialog != this.dialog) return false;
        this.dialog = null;
        return true;
    }

    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }

    public boolean isAbsolute() {
        return absolute;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void setPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
    }
}
