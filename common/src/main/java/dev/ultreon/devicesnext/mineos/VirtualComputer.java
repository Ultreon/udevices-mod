package dev.ultreon.devicesnext.mineos;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.IntMap;
import dev.ultreon.devicesnext.filesystem.*;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.api.OperatingSystem;
import dev.ultreon.devicesnext.mineos.gui.GpuRenderer;
import dev.ultreon.devicesnext.mineos.gui.VirtualGpu;
import dev.ultreon.devicesnext.virtual.*;
import dev.ultreon.mcgdx.impl.GdxScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jnode.fs.FileSystemException;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static dev.ultreon.devicesnext.UDevicesMod.MOD_ID;

/**
 * Screen for showing the window manager, the desktop and the taskbar.
 */
public class VirtualComputer extends GdxScreen {
    private final Screen back;
    private final boolean desktopFullscreen;
    private final GpuRenderer gfx = new GpuRenderer(this);
    private final Thread codeThread;
    private final Engine engine;
    private final Context codeContext;
    public Value processEvent;
    protected int desktopX;
    protected int desktopY;
    protected int desktopWidth;
    protected int desktopHeight;
    private Kernel kernel;
    private OperatingSystemImpl system;
    private int pid = 0;
    private final IntMap<VirtualProcess> processes = new IntMap<>();
    private final FS fs;
    private final VirtualFileSystem virtualFS = new VirtualFileSystem(this);
    private final VirtualBiosApi virtualBiosApi = new VirtualBiosApi(this);

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

        this.engine = Engine.newBuilder()
                .logHandler(new Handler() {
                    @Override
                    public void publish(LogRecord record) {
                        Level level = record.getLevel();
                        Logger logger = LoggerFactory.getLogger(record.getLoggerName());
                        if (level.intValue() < Level.FINER.intValue()) {
                            logger.trace("{}: {}", record.getLevel().getName(), record.getMessage());
                        } else if (level.intValue() < Level.FINE.intValue()) {
                            logger.debug("{}: {}", record.getLevel().getName(), record.getMessage());
                        } else if (level.intValue() < Level.INFO.intValue()) {
                            logger.info("{}: {}", record.getLevel().getName(), record.getMessage());
                        } else if (level.intValue() < Level.WARNING.intValue()) {
                            logger.warn("{}: {}", record.getLevel().getName(), record.getMessage());
                        } else {
                            logger.error("{}: {}", record.getLevel().getName(), record.getMessage());
                        }
                    }

                    @Override
                    public void flush() {

                    }

                    @Override
                    public void close() throws SecurityException {

                    }
                }).build();

        codeContext = Context.newBuilder("python")
                .environment("OSTYPE", "MineOS")
                .environment("PROCESSOR_ARCHITECTURE", "AMD64")
                .environment("PYTHON_PLATFORM", "unix")
                .environment("USER", "root")
                .environment("SHELL", "/bin/shell.py")
                .environment("LANG", "en_US.UTF-8")
                .environment("HOME", "/root")
                .environment("TERM", "shell")
                .option("python.PythonPath", "/Library:/User/Library:/User/Local/Library:/VariableData/Library:/System/Library:/Boot")
                .engine(engine)
                .allowIO(IOAccess.newBuilder().fileSystem(virtualFS).build())
                .out(java.lang.System.out)
                .in(NullInputStream.INSTANCE)
                .err(java.lang.System.err)
                .timeZone(ZoneId.of("UTC"))
                .processHandler(new VirtualProcessHandler(this))
                .allowCreateThread(false)
                .allowCreateProcess(false)
                .allowNativeAccess(false)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .allowHostClassLoading(false)
                .allowHostAccess(HostAccess.newBuilder()
                        .allowAccessAnnotatedBy(VirtualApi.class)
                        .allowListAccess(true)
                        .allowMapAccess(true)
                        .allowBufferAccess(true)
                        .allowArrayAccess(true)
                        .allowPublicAccess(true)
                        .allowBigIntegerNumberAccess(true)
                        .allowIterableAccess(true)
                        .allowIteratorAccess(true)
                        .allowImplementations(Object.class)
                        .allowAllImplementations(false)
                        .allowAccessInheritance(false)
                        .denyAccess(Class.class)
                        .denyAccess(ClassLoader.class)
                        .denyAccess(MethodHandle.class)
                        .denyAccess(MethodHandles.class)
                        .denyAccess(MethodHandles.Lookup.class)
                        .denyAccess(Method.class)
                        .denyAccess(Thread.class)
                        .denyAccess(ProcessHandle.class)
                        .denyAccess(ProcessBuilder.class)
                        .denyAccess(Process.class)
                        .denyAccess(Runtime.class)
                        .denyAccess(VirtualComputer.class)
                        .denyAccess(ThreadGroup.class)
                        .denyAccess(Thread.State.class)
                        .denyAccess(Thread.UncaughtExceptionHandler.class)
                        .build())
                .allowHostClassLookup(s -> s.startsWith("com.ultreon.devices."))
                .allowValueSharing(true)
                .useSystemExit(false)
                .allowPolyglotAccess(PolyglotAccess.NONE).build();

        this.codeThread = createCodeThread();
    }

    @Override
    public void removed() {
        super.removed();

        if (this.codeThread != null) {
            this.codeThread.interrupt();
            this.codeContext.close(true);
            try {
                this.codeThread.interrupt();
                this.codeThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        this.gfx.dispose();

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
        codeContext.getBindings("python").putMember("__bios", virtualBiosApi);

        try {
            UDevicesMod.LOGGER.info("Starting BIOS...");
            //noinspection PyUnresolvedReferences,PyStatementEffect
            Value python = codeContext.eval(Source.newBuilder("python", """
                    def bios_main(__bios, logger):
                        try:
                            import os
                            print("Booting up...")
                    
                            if not os.path.exists("/Boot/main.py"):
                                print("No main.py found")
                                return -1
                    
                            with open("/Boot/main.py", "r") as f:
                                data = f.read()
                                print(data)
                                try:
                                    exec(data, {"__name__": "__main__", "__bios": __bios, "logger": logger})
                                except Exception as e:
                                    import traceback
                                    traceback.print_exc()
                                    return -2
                        
                            print("Shut down!")
                        
                            return 0
                        except BaseException as e:
                            import traceback
                            traceback.print_exception(e)            
                    
                    bios_main
                    """, "<<bios>>").build());

            python.execute(virtualBiosApi, UDevicesMod.LOGGER);

            //noinspection ResultOfMethodCallIgnored
            Minecraft.getInstance().submit(this::onClose);
        } catch (IOException e) {
            UDevicesMod.LOGGER.error("Failed to start BIOS", e);
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
    public void render(ShapeDrawer shapeDrawer, Batch batch, int i, int j, float f) {
        super.render(shapeDrawer, batch, i, j, f);

        double scale = 0;
        if (minecraft != null) {
            scale = minecraft.getWindow().getGuiScale();
        }

        gfx.begin();
        gfx.clear(1, 1, 1, 1);
        gfx.end();

//        batch.begin();
//        batch.draw(gfx.getDisplayTexture(), 0, (float) height / 2, 30, 30);
//        batch.end();
    }

    public void render(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
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
//            BaseScreen.renderFrame(gfx, this.desktopX - 8, this.desktopY - 8, this.desktopWidth + 16, this.desktopHeight + 16, this.getTheme());
            // TODO
        }


        try {
            this.system.setWidth(this.width);
            this.system.setHeight(this.height);
            this.system.render(gfx, finalMouseX - this.desktopX, finalMouseY - this.desktopY, partialTicks);
        } catch (Throwable throwable) {
            if (this.system == null) return;
            this.system._raiseHardError(throwable);
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
        if (system == null) return false;
        return isMouseOverDisplay(mouseX, mouseY) && this.system.mouseReleased(mouseX - desktopX, mouseY - desktopY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (system == null) return false;
        return isMouseOverDisplay(mouseX, mouseY) && system.mouseClicked(mouseX - desktopX, mouseY - desktopY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (system == null) return false;
        return isMouseOverDisplay(mouseX, mouseY) && system.mouseDragged(mouseX - desktopX, mouseY - desktopY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if (system == null) return false;
        return isMouseOverDisplay(pMouseX, pMouseY) && system.mouseScrolled(pMouseX - desktopX, pMouseY - desktopY, pScrollY);
    }

    private boolean isMouseOverDisplay(double mouseX, double mouseY) {
        if (system == null) return false;
        return isPointBetween((int) mouseX, (int) mouseY, desktopX, desktopY, desktopWidth, desktopHeight);
    }

    private boolean isPointBetween(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (system == null) return false;
        return system.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (system == null) return false;
        return system.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (system == null) return false;
        return system.charTyped(codePoint, modifiers);
    }

    @Override
    protected void updateNarrationState(@NotNull NarrationElementOutput narrationElementOutput) {
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

    public VirtualProcess spawnProcess(Value modules, String init, String[] command, Map<String, String> env) {
        String path = command[0];
        String[] args = Arrays.copyOfRange(command, 1, command.length);
        VirtualProcess virtualProcess = new VirtualProcess(this, ++pid, modules, init, path, args, env);
        this.processes.put(virtualProcess.getPid(), virtualProcess);
        virtualProcess.start();
        return virtualProcess;
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

    public VirtualProcess getProcess(int pid) {
        return processes.get(pid);
    }

    public Engine getEngine() {
        return engine;
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
