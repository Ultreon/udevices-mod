package dev.ultreon.devicesnext.mineos;

import com.google.common.collect.Lists;
import dev.ultreon.devicesnext.api.OperatingSystem;
import dev.ultreon.devicesnext.client.ScissorStack;
import dev.ultreon.devicesnext.client.gui.BaseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.compress.utils.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

/**
 * Screen for showing the window manager, the desktop and the taskbar.
 */
public class DeviceScreen extends BaseScreen {
    private final Screen back;
    private final boolean desktopFullscreen;
    protected int desktopX;
    protected int desktopY;
    protected int desktopWidth;
    protected int desktopHeight;
    private Kernel kernel;
    private OperatingSystemImpl system;

    public DeviceScreen(LaunchOptions options) {
        super(options.title);

        assert this.minecraft != null;

        this.back = options.back;

        this.desktopWidth = Math.max(options.width, 1);
        this.desktopHeight = Math.max(options.height, 1);
        this.desktopX = (this.width - this.desktopWidth) / 2;
        this.desktopY = (this.height - this.desktopHeight) / 2;
        this.desktopFullscreen = options.fullscreen;

        this.system = new OperatingSystemImpl(this, this.desktopX, this.desktopY, this.desktopWidth, this.desktopHeight, Lists.newArrayList(options.windows), new DesktopApplication());
        try {
            this.system.border = new Insets(0, 0, 20, 0);
            this.kernel = this.system.kernel;

            this.system._boot();
        } catch (Throwable throwable) {
            this.system._raiseHardError(throwable);
        }
    }

    @Override
    protected void init() {
        super.init();

        if (this.desktopFullscreen) {
            this.desktopWidth = this.width;
            this.desktopHeight = this.height;
            this.desktopX = 0;
            this.desktopY = 0;
            assert this.minecraft != null;
            if (this.back != null) this.back.init(this.minecraft, this.width, this.height);
        } else {
            this.desktopX = (this.width - this.desktopWidth) / 2;
            this.desktopY = (this.height - this.desktopHeight) / 2;
        }
    }

    @SafeVarargs
    protected final <T extends Application> void registerApp(ApplicationId id, ApplicationFactory<T> factory, T... typeGetter) {
        this.system.registerApp(id, factory, typeGetter);
    }

    /**
     * @return the desktop window related to the {@link #getSystem()} method.
     */
    protected final Application getDesktopApp() {
        return this.system.getDesktop();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gfx, mouseX, mouseY, partialTicks);

        double[] xPos = new double[1];
        double[] yPos = new double[1];

        assert this.minecraft != null;
        GLFW.glfwGetCursorPos(this.minecraft.getWindow().getWindow(), xPos, yPos);

        if (xPos[0] < 0) mouseX = Integer.MIN_VALUE;
        if (yPos[0] < 0) mouseY = Integer.MIN_VALUE;

        if (xPos[0] > this.minecraft.getWindow().getWidth()) mouseX = Integer.MAX_VALUE;
        if (yPos[0] > this.minecraft.getWindow().getHeight()) mouseY = Integer.MAX_VALUE;

        int finalMouseX = mouseX;
        int finalMouseY = mouseY;

        if (!this.desktopFullscreen) {
            BaseScreen.renderFrame(gfx, this.desktopX - 8, this.desktopY - 8, this.desktopWidth + 16, this.desktopHeight + 16);
        }

        try {
            ScissorStack.scissor(gfx, this.desktopX, this.desktopY, this.desktopWidth, this.desktopHeight, () -> {
                this.system.setWidth(this.width);
                this.system.setHeight(this.height);
                this.system.renderComponent(gfx, finalMouseX - this.desktopX, finalMouseY - this.desktopY, partialTicks);
            });
        } catch (Throwable throwable) {
            this.system._raiseHardError(throwable);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        if (!this.desktopFullscreen) {
            if (this.back != null) {
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, -2000);

                this.back.render(gfx, Integer.MAX_VALUE, Integer.MAX_VALUE, partialTicks);

                gfx.pose().popPose();
            }

            super.renderBackground(gfx, mouseX, mouseY, partialTicks);
        } else {
            super.renderBackground(gfx, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * @return the device's OS desktop.
     */
    public OperatingSystem getSystem() {
        return this.system;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isMouseOverDisplay(mouseX, mouseY))
            this.system.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return isMouseOverDisplay(mouseX, mouseY) && this.system.mouseReleased(mouseX - desktopX, mouseY - desktopY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return isMouseOverDisplay(mouseX, mouseY) && system.mouseClicked(mouseX - desktopX, mouseY - desktopY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return isMouseOverDisplay(mouseX, mouseY) && system.mouseDragged(mouseX - desktopX, mouseY - desktopY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        return isMouseOverDisplay(mouseX, mouseY) && system.mouseScrolled(mouseX - desktopX, mouseY - desktopY, amountX, amountY);
    }

    private boolean isMouseOverDisplay(double mouseX, double mouseY) {
        return isPointBetween((int) mouseX, (int) mouseY, desktopX, desktopY, desktopWidth, desktopHeight);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return system.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return system.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return system.charTyped(codePoint, modifiers);
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput narrationElementOutput) {
        // No-op
    }

    public void onShutdown() {
        this.system = null;
        System.gc();
        Minecraft.getInstance().setScreen(back);
    }

    public @Nullable Application getKernel() {
        return kernel;
    }

    public void open() {
        Minecraft.getInstance().setScreen(this);
    }

    public static class LaunchOptions {
        public int x = 0;
        public int y = 0;
        public int width = 427;
        public int height = 240;
        private Component title;
        private Screen back;
        private boolean fullscreen = false;
        private final Set<Window> windows = Sets.newHashSet();

        public LaunchOptions title(Component title) {
            this.title = title;
            return this;
        }

        public LaunchOptions rect(Rectangle rect) {
            this.x = rect.x;
            this.y = rect.y;
            this.width = rect.width;
            this.height = rect.height;
            return this;
        }

        public LaunchOptions back(Screen back) {
            this.back = back;
            return this;
        }

        public LaunchOptions window(Window window) {
            this.windows.add(window);
            return this;
        }

        public LaunchOptions fullscreen() {
            this.fullscreen = true;
            return this;
        }
    }
}
