package dev.ultreon.devicesnext.mineos.gui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.ultreon.devicesnext.UDevicesMod;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public abstract class McWidget {
    public int x;
    public int y;
    public int width;
    public int height;
    public String message;
    protected boolean focused = false;
    protected Minecraft minecraft = Minecraft.getInstance();
    protected BitmapFont font = UDevicesMod.getFont();
    public boolean active;
    public boolean visible;

    public McWidget(int x, int y, int width, int height, String message) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message;
    }

    protected abstract void render(@NotNull GpuRenderer renderer, int mouseX, int mouseY, float partialTicks);

    protected boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    protected boolean mouseScrolled(double mouseX, double mouseY, double amountY) {
        return false;
    }

    protected boolean preMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    protected void mouseMoved(double mouseX, double mouseY) {

    }

    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    protected boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    protected boolean charTyped(char c, int modifiers) {
        return false;
    }

    protected boolean isMouseOver(double mouseX, double mouseY) {
        return isPointInBounds(mouseX, mouseY, x, y, width, height);
    }

    public static boolean isPointInBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    protected boolean isFocused() {
        return focused;
    }

    protected void setFocused(boolean focused) {
        this.focused = focused;
    }

    protected void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    protected void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
