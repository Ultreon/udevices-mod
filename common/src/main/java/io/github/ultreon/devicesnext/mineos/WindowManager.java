package com.ultreon.mods.lib.client.devicetest;

import com.google.common.collect.Iterators;
import com.google.common.collect.Queues;
import com.ultreon.libs.commons.v0.size.IntSize;
import com.ultreon.mods.lib.client.devicetest.gui.McComponent;
import com.ultreon.mods.lib.util.ScissorStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.awt.*;
import java.util.List;
import java.util.*;

abstract sealed class WindowManager extends McComponent permits OperatingSystemImpl {
    final Deque<Window> windows = Queues.synchronizedDeque(new ArrayDeque<>());
    private final Deque<Window> midStack = new ArrayDeque<>();
    private final Deque<Window> topStack = new ArrayDeque<>();
    private final Deque<Window> bottomStack = new ArrayDeque<>();
    private final Object wmLock = new Object();
    private Window pressedWindow;

    private Window activeWindow;
    protected int windowActiveColor = 0xffdc143c;
    protected int windowInactiveColor = 0xff555555;
    Insets border;
    private final OperatingSystemImpl system;

    public WindowManager(int x, int y, int width, int height, List<Window> windows) {
        super(x, y, width, height, Component.empty());
        this.windows.addAll(new ArrayDeque<>(windows));
        this.midStack.addAll(windows);
        if (this instanceof OperatingSystemImpl operatingSystem) {
            this.system = operatingSystem;
        } else {
            this.system = OperatingSystemImpl.get();
        }
        windows.forEach(window -> window.wm = this);
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderWindows(gfx, mouseX, mouseY, partialTicks);
    }

    private void renderWindows(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.pose().pushPose();
        gfx.pose().scale(1, 1, 1 / 2000f);
        synchronized (wmLock) {
            renderWindowsBS(gfx, mouseX, mouseY, partialTicks);
            renderWindowsNS(gfx, mouseX, mouseY, partialTicks);
            renderWindowsTS(gfx, mouseX, mouseY, partialTicks);
        }
        gfx.pose().popPose();
    }

    private void renderWindowsNS(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        var windows = new ArrayList<>(this.midStack);
        var hoveredWindow = getHoveredWindow(mouseX, mouseY);
        for (var i = windows.size() - 1; i > -1; i--) {
            var window = windows.get(i);

            renderWindow(gfx, mouseX, mouseY, partialTicks, window, hoveredWindow);
            gfx.pose().translate(0, 0, 1);
        }
    }

    private void renderWindow(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks, @NotNull Window window, Window hoveredWindow) {
        try {
            @NotNull Window _window = window;
            @Nullable DialogWindow dialog = _window.getDialog();

            while (dialog != null) {
                @Nullable IntSize forceSize = _window.getForceSize();
                if (forceSize != null) _window.resize(forceSize.width(), forceSize.height());
                @Nullable Vector2i forcePosition = _window.getForcePosition();
                if (forcePosition != null) _window.setPosition(forcePosition.x, forcePosition.y);
                _window.render(gfx, Integer.MAX_VALUE, Integer.MAX_VALUE, partialTicks);
                _window.getDialog().render(gfx, Integer.MAX_VALUE, Integer.MAX_VALUE, partialTicks);

                _window = dialog;
                dialog = _window.getDialog();
            }

            @Nullable IntSize forceSize = _window.getForceSize();
            if (forceSize != null) _window.resize(forceSize.width(), forceSize.height());
            @Nullable Vector2i forcePosition = _window.getForcePosition();
            if (forcePosition != null) _window.setPosition(forcePosition.x, forcePosition.y);
            if (_window.equals(hoveredWindow)) _window.render(gfx, mouseX, mouseY, partialTicks);
            else _window.render(gfx, Integer.MAX_VALUE, Integer.MAX_VALUE, partialTicks);
        } catch (Exception e) {
            ScissorStack.clearScissorStack();
            crashApplication(window.application, e);
        }
    }

    private void renderWindowsTS(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        var windows = new ArrayList<>(this.topStack);
        var hoveredWindow = getHoveredWindow(mouseX, mouseY);
        for (var i = windows.size() - 1; i > -1; i--) {
            var window = windows.get(i);

            renderWindow(gfx, mouseX, mouseY, partialTicks, window, hoveredWindow);
            gfx.pose().translate(0, 0, 1);
        }
    }

    private void renderWindowsBS(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        var windows = new ArrayList<>(this.bottomStack);
        var hoveredWindow = getHoveredWindow(mouseX, mouseY);
        for (var i = windows.size() - 1; i > -1; i--) {
            var window = windows.get(i);

            renderWindow(gfx, mouseX, mouseY, partialTicks, window, hoveredWindow);
            gfx.pose().translate(0, 0, 1);
        }
    }

    @Nullable
    private Window getHoveredWindow(int mouseX, int mouseY) {
        synchronized (this.wmLock) {
            for (var window : this.windows()) {
                if (window.isMouseOver(mouseX, mouseY)) {
                    return window;
                }
            }
        }
        return null;
    }

    public synchronized void createWindow(Window window) {
        synchronized (this.wmLock) {
            if (this.windows.contains(window)) throw new IllegalArgumentException("Window already exists.");
            if (!this.system.isApplicationRegistered(window.application)) throw new IllegalArgumentException("Invalid application: " + window.application.getId());
            window.wm = this;
            this.windows.addLast(window);
            this.midStack.addLast(window);
            window.topMost = false;
            window.bottomMost = false;
            window.onCreated();
        }
    }

    private synchronized void setTopStack(Window window) {
        synchronized (this.wmLock) {
            this.bottomStack.remove(window);
            this.midStack.remove(window);
            this.topStack.addFirst(window);
            window.topMost = true;
            window.bottomMost = false;
        }
    }

    private synchronized void setBottomStack(Window window) {
        synchronized (this.wmLock) {
            this.topStack.remove(window);
            this.midStack.remove(window);
            this.bottomStack.addFirst(window);
            window.topMost = false;
            window.bottomMost = true;
        }
    }

    private synchronized void setMidStack(Window window) {
        synchronized (this.wmLock) {
            this.topStack.remove(window);
            this.midStack.remove(window);
            this.bottomStack.addFirst(window);
            window.topMost = false;
            window.bottomMost = true;
        }
    }

    synchronized void setTopMost(Window window, boolean topMost) {
        synchronized (this.wmLock) {
            if (this.bottomStack.contains(window)) throw new IllegalStateException("Window is already bottom-most, did you meant to set that to false?");
            if (topMost) {
                if (window.topMost) {
                    return;
                }
                setTopStack(window);
            } else if (window.topMost) {
                setMidStack(window);
            }
        }
    }

    synchronized void setBottomMost(Window window, boolean bottomMost) {
        synchronized (this.wmLock) {
            if (this.topStack.contains(window)) throw new IllegalStateException("Window is already top-most, did you meant to set that to false?");
            if (bottomMost) {
                if (window.bottomMost)
                    return;
                this.setBottomStack(window);
            } else if (window.bottomMost)
                this.setMidStack(window);
        }
    }

    synchronized void destroyWindow(Window window) {
        synchronized (this.wmLock) {
            if (!this.windows.contains(window)) return;

            this.windows.remove(window);
            this.topStack.remove(window);
            this.midStack.remove(window);
            this.bottomStack.remove(window);

            try {
                window.getApplication()._destroyWindow(window);
            } catch (Exception e) {
                crashApplication(window.application, e);
                return;
            }

            setNewWindow: {
                Window newWindow;

                if (!this.topStack.isEmpty()) newWindow = this.topStack.getFirst();
                else if (!this.midStack.isEmpty()) newWindow = this.midStack.getFirst();
                else if (!this.bottomStack.isEmpty()) newWindow = this.bottomStack.getFirst();
                else break setNewWindow;

                try {
                    this.moveToForeground(newWindow);
                } catch (Exception e) {
                    crashApplication(newWindow.application, e);
                }
            }
        }
    }

    synchronized void moveToForeground(Window window) {
        synchronized (wmLock) {
            try {
                if (!this.windows.contains(window)) throw new IllegalArgumentException("Window doesn't exist.");

                var oldWindow = this.activeWindow;
                if (oldWindow != null) {
                    oldWindow.handleSetInactive();
                }
            } catch (Exception e) {
                crashApplication(this.pressedWindow.application, e);
                return;
            }

            this.windows.remove(window);
            this.windows.addFirst(window);
            if (window.topMost) {
                this.topStack.remove(window);
                this.topStack.addFirst(window);
            } else if (window.bottomMost) {
                this.bottomStack.remove(window);
                this.bottomStack.addFirst(window);
            } else {
                this.midStack.remove(window);
                this.midStack.addFirst(window);
            }

            try {
                this.activeWindow = window;
                this.activeWindow.handleSetActive();
            } catch (Exception e) {
                crashApplication(this.pressedWindow.application, e);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        synchronized (this.wmLock) {
            for (var window : this.windows()) {
                try {
                    if (window.isMouseOverOrDialog(mouseX, mouseY)) {
                        if (window.preMouseClicked(mouseX, mouseY, button)) return true;
                        this.moveToForeground(window);
                        this.pressedWindow = window;
                        window.mouseClicked(mouseX, mouseY, button);
                        return true;
                    }
                } catch (Exception e) {
                    crashApplication(this.pressedWindow.application, e);
                    return false;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    private Iterable<Window> windows() {
        var concat = Iterators.concat(this.topStack.iterator(), this.midStack.iterator(), this.bottomStack.iterator());
        return () -> concat;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        synchronized (this.wmLock) {
            if (this.pressedWindow != null) {
                try {
                    this.pressedWindow.mouseReleased(mouseX, mouseY, button);
                    return true;
                } catch (Exception e) {
                    crashApplication(this.pressedWindow.application, e);
                    return false;
                }
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        synchronized (this.wmLock) {
            if (this.pressedWindow != null) {
                try {
                    this.pressedWindow.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                } catch (Exception e) {
                    crashApplication(this.pressedWindow.application, e);
                    return false;
                }
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    private void crashApplication(Application application, Exception e) {
        this.system.crashApplication(application, e);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        synchronized (this.wmLock) {
            for (var window : this.windows()) {
                try {
                    if (window.isMouseOver(mouseX, mouseY)) {
                        window.mouseMoved(mouseX, mouseY);
                        return;
                    }
                } catch (Exception e) {
                    crashApplication(this.pressedWindow.application, e);
                    return;
                }
            }
            super.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        synchronized (this.wmLock) {
            for (var window : this.windows()) {
                try {
                    if (window.isMouseOver(mouseX, mouseY) && window.mouseScrolled(mouseX, mouseY, amountX, amountY)) {
                        return true;
                    }
                } catch (Exception e) {
                    crashApplication(this.pressedWindow.application, e);
                    return false;
                }
            }
            return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Window window = this.activeWindow;
        if (window != null) {
            try {
                if (window.keyPressed(keyCode, scanCode, modifiers)) return true;
            } catch (Exception e) {
                crashApplication(window.application, e);
                return false;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        Window window = this.activeWindow;
        if (window != null) {
            try {
                if (window.keyReleased(keyCode, scanCode, modifiers)) return true;
            } catch (Exception e) {
                crashApplication(window.application, e);
                return false;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        Window window = this.activeWindow;
        if (window != null) {
            try {
                if (window.charTyped(codePoint, modifiers)) return true;
            } catch (Exception e) {
                crashApplication(window.application, e);
                return false;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    public boolean isActiveWindow(Window window) {
        return activeWindow == window;
    }

    public Window getActiveWindow() {
        return activeWindow;
    }

    public int getWindowActiveColor() {
        return windowActiveColor;
    }

    public int getWindowInactiveColor() {
        return windowInactiveColor;
    }

    public void setActiveWindow(Window activeWindow) {
        synchronized (wmLock) {
                var oldWindow = this.activeWindow;
                if (oldWindow != null) {
                    oldWindow.handleSetInactive();
                }
                this.activeWindow = activeWindow;
                activeWindow.handleSetActive();
        }
    }

    public Insets getClientAreaInsets() {
        return border;
    }

    public boolean isValid(Window window) {
        return this.windows.contains(window);
    }
}
