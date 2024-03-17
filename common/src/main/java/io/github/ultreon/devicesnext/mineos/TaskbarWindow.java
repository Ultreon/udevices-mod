package com.ultreon.mods.lib.client.devicetest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ultreon.mods.lib.UltreonLib;
import com.ultreon.mods.lib.input.GameKeyboard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

public final class TaskbarWindow extends Window {
    private final int size;
    private final StartButton startButton;
    private StartMenuWindow startMenu;

    public TaskbarWindow(Application application, int size) {
        super(application, 0, application.getSystem().getHeight() - size, application.getSystem().getWidth(), size, Component.empty());
        this.addOnClosingListener(() -> false);

        this.setAbsolute(true);
        this.setUndecorated(true);
        this.setTransparent(true);

        this.size = size;

        this.startButton = this.add(new StartButton(this, 0, 0, this.size, this.size, Component.literal("Start Menu")));
        this.startButton.loadIcon(UltreonLib.res("textures/gui/device/taskbar/icon.png"), 16, 16);
        this.startButton.setCallback(this::openStartMenu);
    }

    @Override
    public void onCreated() {
        super.onCreated();

        this.getApplication().getSystem().addKeyboardHook(new KeyboardHook() {
            @Override
            public KeyboardHook keyPressed(int keyCode, int scanCode, int modifiers, KeyboardHook next) {
                if (keyCode == InputConstants.KEY_A && GameKeyboard.isKeyDown(getApplication().getSystem().getMetaKey())) {
                    openStartMenu();
                    return null;
                }

                return next;
            }
        });
        this.setTopMost(true);
    }

    private boolean openStartMenu(StartButton button) {
        if (this.startMenu != null && this.startMenu.isValid()) return false;
        return this.openStartMenu();
    }

    public boolean openStartMenu() {
        if (this.startMenu != null && this.startMenu.isValid()) return this.startMenu.close();
        this.startMenu = new StartMenuWindow(this.getApplication(), this.getX(), this.getY() - StartMenuWindow.HEIGHT) {
            @Override
            protected @NotNull Vector2i getForcePosition() {
                return new Vector2i(TaskbarWindow.this.getX(), TaskbarWindow.this.getY() - StartMenuWindow.HEIGHT);
            }
        };
        this.startMenu.create();
        this.startMenu.requestFocus();
        return true;
    }

    @Override
    public void onFocusGained() {
        super.onFocusGained();

        if (this.startMenu != null && !this.startMenu.isValid()) {
            this.startMenu = null;
        }
    }

    @Override
    public boolean preMouseClicked(double mouseX, double mouseY, int button) {
        return super.preMouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    public int getSize() {
        return size;
    }

    @Override
    protected void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.setWidth(this.getScreenWidth());
        this.setHeight(this.size);
        this.setX(0);
        this.setY(this.getScreenHeight() - size);
        this.setTopMost(true);

        RenderSystem.enableBlend();
        gfx.fill(0, 0, width, height, 0x80101010);
        RenderSystem.disableBlend();
    }
}
