package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.filesystem.*;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.firmware.VEfiApplication;
import dev.ultreon.devicesnext.firmware.api.VEfi;
import dev.ultreon.devicesnext.firmware.api.VEfiExt;
import dev.ultreon.devicesnext.mineos.gui.GpuRenderer;
import dev.ultreon.devicesnext.mineos.gui.VirtualGpu;
import dev.ultreon.devicesnext.virtual.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jnode.fs.FileSystemException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import static dev.ultreon.devicesnext.UDevicesMod.MOD_ID;

/**
 * Screen for showing the window manager, the desktop and the taskbar.
 */
public class VirtualComputer extends Screen {
    private final Screen back;
    private final boolean desktopFullscreen;
    private final GpuRenderer gfx = new GpuRenderer(this);
    private final Thread codeThread;
    protected int desktopX;
    protected int desktopY;
    protected int desktopWidth;
    protected int desktopHeight;
//    private Kernel kernel;
//    private OperatingSystemImpl system;
    private int pid = 0;
    private final FS fs;
    private final VirtualFileSystem virtualFS = new VirtualFileSystem(this);
    private VEfiApplication vEfiApplication;
    private final VEfi vEfi = new VEfi();

    public VirtualComputer(LaunchOptions options) {
        super(options.title);

        assert this.minecraft != null;

        this.back = options.back;

        this.desktopWidth = Math.max(options.width, 1);
        this.desktopHeight = Math.max(options.height, 1);
        this.desktopX = (this.width - this.desktopWidth) / 2;
        this.desktopY = (this.height - this.desktopHeight) / 2;
        this.desktopFullscreen = options.fullscreen;

        try {
            Path resolve = UDevicesMod.getDataPath().resolve(String.valueOf(UUID.nameUUIDFromBytes("DevDebug".getBytes(StandardCharsets.UTF_8))) + ".ext2");
            if (Files.notExists(resolve)) {
                InputStream resourceAsStream = getClass().getResourceAsStream("/data/" + MOD_ID + "/filesystems/main.ext2");
                if (resourceAsStream == null) {
                    throw new RuntimeException("Failed to load main.ext2");
                }
                IOUtils.copy(resourceAsStream, Files.newOutputStream(resolve));
            }
            this.fs = Ext2FS.open(resolve);
        } catch (IOException | FileSystemException e) {
            throw new RuntimeException(e);
        }

        this.codeThread = createCodeThread();
    }

    @Override
    public void removed() {
        super.removed();

        if (this.codeThread != null) {
            this.codeThread.interrupt();
            try {
                this.codeThread.interrupt();
                this.codeThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        this.gfx.delete();

        try {
            fs.close();
        } catch (IOException e) {
            UDevicesMod.LOGGER.error("Failed to close filesystem", e);
        }
    }

    private Thread createCodeThread() {
        Thread codeThread = new Thread(this::initialize);

        codeThread.setDaemon(false);
        codeThread.start();
        return codeThread;
    }

    private void initialize() {
        try (SeekableByteChannel channel = getFileSystem().open(new PathHandle("/Boot/WASM32.VEFI"), StandardOpenOption.READ)) {
            SeekableByteChannel position = channel.position(0L);
            long size = channel.size();
            if (size > Integer.MAX_VALUE) {
                UDevicesMod.LOGGER.error("VEFI Boot file is too large: {} >= Integer.MAX_VALUE", size);
                return;
            }
            ByteBuffer allocate = ByteBuffer.allocate((int) size);
            position.read(allocate);
            byte[] array = allocate.array();
            VEfiApplication virtualEfiApp = new VEfiApplication(array, "WASMX86.VEFI", new VEfiExt(fs));
            vEfiApplication = virtualEfiApp;
            virtualEfiApp.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
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

        this.gfx.reconnectDisplay(this.width, this.height);
    }

//    @SafeVarargs
//    protected final <T extends Application> void registerApp(ApplicationId id, ApplicationFactory<T> factory, T... typeGetter) {
//        this.system.registerApp(id, factory, typeGetter);
//    }

//    /**
//     * @return the desktop window related to the {@link #getSystem()} method.
//     */
//    protected final Application getDesktopApp() {
//        return this.system.getDesktop();
//    }


    public void render(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
//        double[] xPos = new double[1];
//        double[] yPos = new double[1];
//
//        assert this.minecraft != null;
//        GLFW.glfwGetCursorPos(this.minecraft.getWindow().getWindow(), xPos, yPos);
//
//        if (xPos[0] < 0) mouseX = Integer.MIN_VALUE;
//        if (yPos[0] < 0) mouseY = Integer.MIN_VALUE;
//
//        if (xPos[0] > this.minecraft.getWindow().getWidth()) mouseX = Integer.MAX_VALUE;
//        if (yPos[0] > this.minecraft.getWindow().getHeight()) mouseY = Integer.MAX_VALUE;
//
//        int finalMouseX = mouseX;
//        int finalMouseY = mouseY;
//
//        if (!this.desktopFullscreen) {
////            BaseScreen.renderFrame(gfx, this.desktopX - 8, this.desktopY - 8, this.desktopWidth + 16, this.desktopHeight + 16, this.getTheme());
//            // TODO
//        }
//
//
//        try {
//            this.system.setWidth(this.width);
//            this.system.setHeight(this.height);
//            this.system.render(gfx, finalMouseX - this.desktopX, finalMouseY - this.desktopY, partialTicks);
//        } catch (Throwable throwable) {
//            if (this.system == null) return;
//            this.system._raiseHardError(throwable);
//        }
    }

    /**
     * @return the device's OS desktop.
     */
    public VEfiApplication getSystem() {
        return vEfiApplication;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
//        if (isMouseOverDisplay(mouseX, mouseY))
//            this.vEfi.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
//        if (system == null) return false;
//        return isMouseOverDisplay(mouseX, mouseY) && this.system.mouseReleased(mouseX - desktopX, mouseY - desktopY, button);
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//        if (system == null) return false;
//        return isMouseOverDisplay(mouseX, mouseY) && system.mouseClicked(mouseX - desktopX, mouseY - desktopY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
//        if (system == null) return false;
//        return isMouseOverDisplay(mouseX, mouseY) && system.mouseDragged(mouseX - desktopX, mouseY - desktopY, button, dragX, dragY);
        return false;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
//        if (system == null) return false;
//        return isMouseOverDisplay(pMouseX, pMouseY) && system.mouseScrolled(pMouseX - desktopX, pMouseY - desktopY, pScrollY);
        return false;
    }

    private boolean isMouseOverDisplay(double mouseX, double mouseY) {
//        if (system == null) return false;
//        return isPointBetween((int) mouseX, (int) mouseY, desktopX, desktopY, desktopWidth, desktopHeight);
        return false;
    }

    private boolean isPointBetween(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//        if (system == null) return false;
//        return system.keyPressed(keyCode, scanCode, modifiers);
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
//        if (system == null) return false;
//        return system.keyReleased(keyCode, scanCode, modifiers);
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
//        if (system == null) return false;
//        return system.charTyped(codePoint, modifiers);
        return false;
    }

    @Override
    protected void updateNarrationState(@NotNull NarrationElementOutput narrationElementOutput) {
        // No-op
    }

    public void onShutdown() {
        vEfiApplication.close();
        System.gc();
        Minecraft.getInstance().setScreen(back);
    }

    public @Nullable VEfiApplication getKernel() {
        return vEfiApplication;
    }

    public void open() {
        Minecraft.getInstance().setScreen(this);
    }

    public FS getFileSystem() {
        return fs;
    }

    public VirtualFileSystem getFS() {
        return virtualFS;
    }

    public VirtualGpu getGpu() {
        return gfx.getVGpu();
    }

    public static class LaunchOptions {
        public int x = 0;
        public int y = 0;
        public int width = 427;
        public int height = 240;
        private Component title;
        private Screen back;
        private boolean fullscreen = false;

        public LaunchOptions title(Component title) {
            this.title = title;
            return this;
        }

        public LaunchOptions back(Screen back) {
            this.back = back;
            return this;
        }

        public LaunchOptions fullscreen() {
            this.fullscreen = true;
            return this;
        }
    }
}
