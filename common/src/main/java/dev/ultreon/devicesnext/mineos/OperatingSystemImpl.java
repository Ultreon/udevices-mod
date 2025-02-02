package dev.ultreon.devicesnext.mineos;

import com.caoccao.javet.enums.JSRuntimeType;
import com.caoccao.javet.exceptions.JavetError;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.options.V8RuntimeOptions;
import com.caoccao.javet.javenode.JNEventLoop;
import com.caoccao.javet.javenode.JNEventLoopOptions;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.reference.V8Module;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ultreon.mods.lib.util.KeyboardHelper;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.api.Color;
import dev.ultreon.devicesnext.api.OperatingSystem;
import dev.ultreon.devicesnext.client.ScissorStack;
import dev.ultreon.devicesnext.device.hardware.FSDirectory;
import dev.ultreon.devicesnext.device.hardware.FSFile;
import dev.ultreon.devicesnext.device.hardware.FSNode;
import dev.ultreon.devicesnext.mineos.exception.McAccessDeniedException;
import dev.ultreon.devicesnext.mineos.exception.McAppNotFoundException;
import dev.ultreon.devicesnext.mineos.exception.McNoPermissionException;
import dev.ultreon.devicesnext.mineos.exception.McSecurityException;
import dev.ultreon.devicesnext.mineos.security.Permission;
import dev.ultreon.devicesnext.mineos.security.SpawnApplicationPermission;
import dev.ultreon.devicesnext.mineos.sizing.IntSize;
import it.unimi.dsi.fastutil.objects.Reference2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ultreon.mods.lib.util.KeyboardHelper.Modifier;

public final class OperatingSystemImpl extends WindowManager implements OperatingSystem {
    public static final Gson GSON = new GsonBuilder().create();
    private static OperatingSystemImpl instance;
    private final Map<ApplicationId, ApplicationFactory<?>> applications = new HashMap<>();
    private final Map<Class<?>, ApplicationId> applicationTypes = new HashMap<>();
    private final PermissionManager permissionManager = new PermissionManager();
    private final List<Application> openApps = new CopyOnWriteArrayList<>();
    private DeviceScreen screen;
    private DesktopWindow desktop;
    private TaskbarWindow taskbar;
    private DesktopApplication desktopApp;
    private final Modifier metaKey = Modifier.CTRL;
    private long pid = 0L;
    final Kernel kernel = new Kernel();
    private final OsLogger logger = new OsLoggerImpl();
    private final Deque<KeyboardHook> keyboardHooks = new ArrayDeque<>();
    private Bsod bsod;
    private long shutdownTime;
    private boolean shuttingDown;
    private final Reference2LongMap<ApplicationId> applicationCooldown = new Reference2LongArrayMap<>();
    private final Map<ShutdownToken, ShutdownTimer> shutdownTimers = new HashMap<>();
    @Nullable
    private ShutdownToken autoShutdownToken = null;
    private ScheduledExecutorService shutdownScheduler = Executors.newScheduledThreadPool(1);
    private List<Application> crashing = new ArrayList<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private FileSystem fileSystem;
    private FileDescriptorManager fdManager;
    private LibStd stdLib;
    private Disk disk;
    private LibMineOS mineOSLib;
    private V8Host host;
    private V8Runtime runtime;

    public OperatingSystemImpl(DeviceScreen screen, int width, int height, ArrayList<Window> windows, DesktopApplication desktopApp) {
        this(screen, 0, 0, width, height, windows, desktopApp);
    }

    public OperatingSystemImpl(DeviceScreen screen, int x, int y, int width, int height, ArrayList<Window> windows, DesktopApplication desktopApp) {
        super(x, y, width, height, windows);

        instance = this;

        this.screen = screen;
        this.desktopApp = desktopApp;
    }

    void _boot() {
        this.disk = new Disk(UUID.nameUUIDFromBytes("player".getBytes()), UUID.nameUUIDFromBytes("kernel".getBytes()));
        this.disk.open();
        this.fileSystem = new FileSystem(disk);
        this.fdManager = new FileDescriptorManager(this);
        this.stdLib = new LibStd(this);
        this.stdLib._init();

        this.mineOSLib = new LibMineOS(this);
        this.mineOSLib._init();

        this.host = V8Host.getInstance(JSRuntimeType.Node);

        if (!this.fileSystem.isInitialized()) {
            this.fileSystem.initialize();
        }

        V8RuntimeOptions options = new V8RuntimeOptions();
        options.setGlobalName("global");
        try {
            this.runtime = this.host.createV8Runtime(options);
        } catch (JavetException e) {
            this._raiseHardError(e);
            return;
        }

        runtime.setV8ModuleResolver((v8Runtime, resourceName, v8ModuleReferrer) -> {
            String resource = this.mineOSLib.readModule(resourceName);

            if (resource == null) {
                throw new JavetException(JavetError.ModuleNotFound, new IOException("Module not found: " + resourceName));
            }

            return v8Runtime.getExecutor(resource).setResourceName(resourceName).setModule(true).compileV8Module();

        });

        this.loadApps(runtime, new JNEventLoop(runtime, Util.make(new JNEventLoopOptions(), jnEventLoopOptions -> {
            jnEventLoopOptions.setGcBeforeClosing(true);
        })));

        try {
            // Register apps and spawn kernel
            this._spawn(this.kernel, new String[]{});
            this.registerApp(this.kernel.getId(), () -> this.kernel);
            this.registerApp(DesktopApplication.id(), () -> desktopApp);

            // Register setup
            ApplicationId setupAppId = new ApplicationId("dev.ultreon:setup");
            FirstTimeSetupApplication setupApp = new FirstTimeSetupApplication(this, setupAppId);
            this.registerApp(setupAppId, () -> setupApp);

            // Setup permissions
            this.permissionManager.grantPermission(DesktopApplication.id(), Permission.SHUTDOWN);
            this.permissionManager.grantPermission(DesktopApplication.id(), Permission.LIST_APPLICATIONS);
            this.permissionManager.grantPermission(DesktopApplication.id(), Permission.SPAWN_APPLICATIONS);

            if (!this.fileSystem.exists("/data/installed")) {
                this._spawn(setupApp, new String[0]);
            } else {
                this._spawn(desktopApp, new String[0]);
            }

            this.desktop = desktopApp.getDesktop();
            this.taskbar = desktopApp.getTaskbar();
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
        }
    }

    @SuppressWarnings("t")
    private void loadApps(V8Runtime runtime, JNEventLoop eventLoop) {
        try {
            FSNode fsNode = this.fileSystem.get("/data/appcfg/");

            if (fsNode == null || !fsNode.isDirectory()) {
                return;
            }

            FSDirectory fsDirectory = (FSDirectory) fsNode;

            for (FSNode file : fsDirectory.list()) {
                if (file.isDirectory()) {
                    continue;
                }

                FSFile fsFile = (FSFile) file;

                String name = fsFile.getName();
                if (!name.endsWith(".json")) {
                    continue;
                }

                int open = stdLib.open("/data/appcfg/" + name, 0);

                if (open == -1) {
                    continue;
                }

                ByteBuffer buffer = ByteBuffer.allocate((int) fsFile.getLength());
                stdLib.read(open, buffer);
                buffer.flip();

                AppConfig appConfig = GSON.fromJson(new String(buffer.array()), AppConfig.class);

                if (appConfig == null) {
                    continue;
                }

                this.<Application>registerApp(new ApplicationId(appConfig.appId), () -> {
                    try {
                        V8Module v8Module = runtime.getExecutor("""
                                import Application from "%s"
                                
                                export default function() {
                                    return new Application();
                                }
                                """).setResourceName("/data/appcfg/" + name).setModule(true).compileV8Module();

                        V8Value namespace = v8Module.getNamespace();

                        return new Application(appConfig.appId) {
                            @Override
                            public void create() {
                                // TODO
                            }
                        };
                    } catch (JavetException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    final <T extends Application> void registerApp(ApplicationId id, ApplicationFactory<T> factory, T... typeGetter) {
        Class<T> componentType = (Class<T>) typeGetter.getClass().getComponentType();
        this.applicationTypes.put(componentType, id);
        this.applications.put(id, factory);
        this.permissionManager.registerPerms(id);
    }

    public static OperatingSystemImpl get() {
        return instance;
    }

    DesktopApplication getDesktop() {
        return desktopApp;
    }

    private boolean _spawn(Application application, String[] argv) {
        if (application.isOpenOnlyOne() && this.openApps.stream().anyMatch(application::isSame)) {
            MessageDialog dialog = MessageDialog.create(
                    this.kernel, MessageDialog.Icons.ERROR, Component.literal("Application"),
                    Component.literal("Application '" + application.getId() + "' can't be opened multiple.")
            );
            this.kernel.createWindow(dialog);
            dialog.requestFocus();
            return false;
        }

        if (this.applicationCooldown.getLong(application.getId()) > System.currentTimeMillis()) {
            UDevicesMod.LOGGER.warn("Application cooldown for {}", application.getId());
            return false;
        }

        this.openApps.add(application);
        try {
            application._main(this, this, argv, this.pid++);
            return true;
        } catch (Exception e) {
            if (application instanceof DesktopApplication) {
                this._raiseHardError(e);
                return false;
            }
            this.crashApplication(application, e);
            return false;
        }
    }

    void crashApplication(Application application, Exception e) {
        if (application instanceof Kernel) {
            this._raiseHardError(e);
            return;
        }

        this.crashing.add(application);
        this.getLogger().error("Application crash:", e);
        this.annihilateApp(application);
        MutableComponent description = Component.literal((ChatFormatting.BOLD + "%s:\n" + ChatFormatting.WHITE + "  %s\n\n" + ChatFormatting.GRAY + "Check logs for more information").formatted(e.getClass().getSimpleName(), e.getMessage()));
        this.kernel.createWindow(MessageDialog.create(this.kernel, MessageDialog.Icons.ERROR, Component.literal("Application Crash"), description));

        this.crashing.remove(application);
    }

    private void annihilateApp(Application application) {
        for (Window window : application.windows) {
            window._destroy();
            this.windows.remove(window);
        }

        this.openApps.remove(application);
    }

    private Application resolveApplication(ApplicationId id) throws McAppNotFoundException {
        ApplicationFactory<?> factory = this.applications.get(id);
        if (factory == null) throw new McAppNotFoundException(id);
        return factory.create();
    }

    boolean spawn(Application executor, ApplicationId id, String... argv) throws McSecurityException, McAppNotFoundException {
        if (!this.permissionManager.hasPermission(executor, Permission.SPAWN_APPLICATIONS) &&
            !this.permissionManager.hasPermission(executor, new SpawnApplicationPermission(id))) {
            throw new McNoPermissionException(executor, new SpawnApplicationPermission(id));
        }

        Application application = this.resolveApplication(id);
        if (application instanceof SystemApp && !(executor instanceof SystemApp)) {
            throw new McAccessDeniedException("Can't open system app from non-system source.");
        }

        return this._spawn(application, argv);
    }

    @Override
    public void shutdown(Application executor) throws McNoPermissionException {
        this.permissionManager.checkPermission(executor, Permission.SHUTDOWN);
        this._shutdown();
    }

    @Override
    public IntSize getScreenSize() {
        return new IntSize(this.width, this.height);
    }

    private void _shutdown() {
        this.shutdownScheduler.shutdownNow().clear();
        this.shutdownTimers.clear();
        this.shuttingDown = true;
        try {
            this.desktopApp.quit();
        } catch (Exception e) {
            this.crashApplication(this.desktopApp, e);
        }

        try {
            this.createWindow(new ShutdownWindow(kernel, 0, 0, width, height, "Shutting down..."));
        } catch (Exception e) {
            logger.error("Failed to create shutdown window", e);
        }

        CompletableFuture.runAsync(() -> {
            CompletableFuture<Void> future = CompletableFuture.runAsync(this::quitAppsForShutdown);

            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                future.cancel(true);
            }
            this.openApps.clear();
            this.windows.clear();

            Minecraft.getInstance().submit(() -> this.screen.onShutdown());

            OperatingSystemImpl.instance = null;
        });
    }

    void destroyApplication(Application application) {
        this.openApps.remove(application);
        try {
            for (Window window : application.windows) {
                this.destroyWindow(window);
            }
            ApplicationId id = application.getId();
            this.applicationCooldown.put(id, System.currentTimeMillis() + 1000);
        } catch (Exception e) {
            this.logger.error("Failed to normally close windows", e);
        }
    }

    @Override
    public void loadWallpaper(File file) {
        this.desktop.loadWallpaper(file);
    }

    @Override
    public void loadWallpaper(Path path) {
        this.desktop.loadWallpaper(path);
    }

    @Override
    public void setColorBackground(Color color) {
        this.desktop.setBackgroundColor(color);
    }

    @Override
    public void setX(int i) {

    }

    @Override
    public void setY(int i) {

    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getWidth() {
        return screen.desktopWidth;
    }

    @Override
    public int getHeight() {
        return screen.desktopHeight;
    }

    public void setWindowActiveColor(int windowActiveColor) {
        this.windowActiveColor = windowActiveColor;
    }

    public void setWindowInactiveColor(int windowInactiveColor) {
        this.windowInactiveColor = windowInactiveColor;
    }

    @Override
    public void raiseHardError(Application executor, Throwable throwable) throws McNoPermissionException {
        this.permissionManager.checkPermission(executor, Permission.HARD_ERROR);
        this._raiseHardError(throwable);
    }

    void _raiseHardError(Throwable throwable) {
        this.openApps.clear();
        this.windows.clear();

        this.shutdownTime = System.currentTimeMillis() + 10000;
        this.bsod = new Bsod(throwable);
        UDevicesMod.LOGGER.error("System hard error:", throwable);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.bsod != null) return false;

        try {
            if (hookIfAvailable(keyCode, scanCode, modifiers)) return true;

            Window activeWindow = getActiveWindow();
            try {
                if (handleKey(keyCode, activeWindow)) return true;
            } catch (Exception e) {
                this.crashApplication(activeWindow.application, e);
                return false;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
            return true;
        }
    }

    private boolean handleKey(int keyCode, Window activeWindow) {
        if ((keyCode == InputConstants.KEY_Q && KeyboardHelper.isKeyDown(metaKey)) || (keyCode == InputConstants.KEY_F4 && KeyboardHelper.isAltDown())) {
            activeWindow.close();
            return true;
        }
        if (!KeyboardHelper.isKeyDown(metaKey)) return false;

        switch (keyCode) {
            case InputConstants.KEY_UP -> {
                if (activeWindow.isMinimized()) activeWindow.restore();
                else activeWindow.maximize();
                return true;
            }
            case InputConstants.KEY_DOWN -> {
                if (activeWindow.isMaximized()) activeWindow.restore();
                else activeWindow.minimize();
                return true;
            }
        }
        return false;
    }

    private boolean hookIfAvailable(int keyCode, int scanCode, int modifiers) {
        Iterator<KeyboardHook> iterator = this.keyboardHooks.iterator();
        if (iterator.hasNext()) {
            KeyboardHook current = iterator.next();
            current = current.keyPressed(keyCode, scanCode, modifiers, current);
            if (current == null) return true;
            while (iterator.hasNext()) {
                current = current.keyPressed(keyCode, scanCode, modifiers, iterator.next());
                if (current == null) return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.bsod != null) return false;

        try {
            Iterator<KeyboardHook> iterator = this.keyboardHooks.iterator();
            if (iterator.hasNext()) {
                KeyboardHook current = iterator.next();
                while (iterator.hasNext()) {
                    current = current.keyReleased(keyCode, scanCode, modifiers, iterator.next());
                    if (current == null) return true;
                }
            }

            return super.keyReleased(keyCode, scanCode, modifiers);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
            return true;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.fill(this.getX(), this.getY(), this.width, this.height, 0xff404040);

        if (this.bsod != null) {
            long millisRemaining = this.shutdownTime - System.currentTimeMillis();
            if (millisRemaining < 0) {
                this._shutdown();
            }

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            gfx.fill(0, 0, getWidth(), getHeight(), 0xff0000ff);
            gfx.pose().pushPose();
            gfx.pose().scale(2, 2, 1);
            gfx.drawString(this.font, ":(", 3, 3, 0xffffffff, false);
            gfx.pose().popPose();
            gfx.drawString(this.font, ChatFormatting.BOLD + "Your in-game system ran into a problem and needs to restart.", 6, 25, 0xffffffff, false);
            gfx.drawString(this.font, "Shutting down in: " + (Mth.ceil(millisRemaining / 1000f)), 20, 10, 0xb0ffffff, false);
            AtomicInteger i = new AtomicInteger(0);
            ExceptionUtils.getStackTrace(this.bsod.throwable()).lines().forEachOrdered(s -> {
                gfx.drawString(this.font, s.replaceAll("\t", "      "), 6, 40 + (i.addAndGet(this.font.lineHeight)), 0xb0ffffff, false);
            });

            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            return;
        }

//        if (this.windows.isEmpty() && !this.shuttingDown) {
//            this.autoShutdownToken = new ShutdownToken();
//            this._delayShutdown(this.autoShutdownToken, new ShutdownTimer(5000));
//            return;
//        } else if (this.autoShutdownToken != null) {
//            this.cancelShutdown(this.autoShutdownToken);
//        }

        try {
            this.openApps.forEach(Application::update);

            super.render(gfx, mouseX, mouseY, partialTicks);
        } catch (Throwable throwable) {
            ScissorStack.clearScissorStack();
            this._raiseHardError(throwable);
        }
    }

    private void cancelShutdown(@NotNull ShutdownToken token) {
        token.schedule.cancel(false);
        this.shutdownTimers.remove(token);
    }

    private void _delayShutdown(ShutdownToken token, ShutdownTimer timer) {
        token.schedule = shutdownScheduler.schedule(() -> {
            Minecraft.getInstance().submit(this::_shutdown);
            return token;
        }, timer.millis(), TimeUnit.MILLISECONDS);
        this.shutdownTimers.put(token, timer);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        try {
            super.renderWidget(gfx, mouseX, mouseY, partialTicks);
        } catch (Throwable throwable) {
            ScissorStack.clearScissorStack();
            this._raiseHardError(throwable);
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.bsod != null) return false;

        try {
            Iterator<KeyboardHook> iterator = this.keyboardHooks.iterator();
            if (iterator.hasNext()) {
                KeyboardHook current = iterator.next();
                while (iterator.hasNext()) {
                    current = current.charTyped(codePoint, modifiers, iterator.next());
                    if (current == null) return true;
                }
            }

            return super.charTyped(codePoint, modifiers);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
            return true;
        }
    }

    public OsLogger getLogger() {
        return logger;
    }

    @Override
    public void addKeyboardHook(KeyboardHook hook) {
        Preconditions.checkNotNull(hook, "hook");
        this.keyboardHooks.addFirst(hook);
    }

    @Override
    public void removeKeyboardHook(KeyboardHook hook) {
        Preconditions.checkNotNull(hook, "hook");
        this.keyboardHooks.remove(hook);
    }

    @Override
    public Modifier getMetaKey() {
        return metaKey;
    }

    @Override
    public void setActiveWindow(Window activeWindow) {
        try {
            super.setActiveWindow(activeWindow);
        } catch (Exception e) {
            this.crashApplication(activeWindow.application, e);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountY) {
        if (this.bsod != null) return false;

        try {
            return super.mouseScrolled(mouseX, mouseY, amountY);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
            return false;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.bsod != null) return false;

        try {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
            return false;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.bsod != null) return false;

        try {
            return super.mouseClicked(mouseX, mouseY, button);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.bsod != null) return false;

        try {
            return super.mouseReleased(mouseX, mouseY, button);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
            return false;
        }
    }

    @Override
    public boolean preMouseClicked(double mouseX, double mouseY, int button) {
        if (this.bsod != null) return false;

        try {
            return super.preMouseClicked(mouseX, mouseY, button);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
            return false;
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.bsod != null) return;

        try {
            super.mouseMoved(mouseX, mouseY);
        } catch (Throwable throwable) {
            this._raiseHardError(throwable);
        }
    }

    public boolean isApplicationRegistered(Application application) {
        try {
            return Objects.equals(applicationTypes.get(application.getClass()), application.getId());
        } catch (Exception e) {
            return false;
        }
    }

    private void quitAppsForShutdown() {
        this.openApps.forEach(application -> {
            try {
                application.quit();
            } catch (Exception e) {
                this.crashApplication(application, e);
            }
        });
    }

    @Override
    public Insets getClientAreaInsets() {
        if (this.openApps.contains(this.desktopApp)) {
            return super.getClientAreaInsets();
        }
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public List<ApplicationId> getApplications(Application context) throws McSecurityException {
        this.permissionManager.checkPermission(context, Permission.LIST_APPLICATIONS);

        return _getApplications();
    }

    private List<ApplicationId> _getApplications() {
        return this.applications.keySet().stream().sorted(Comparator.comparing(a -> a.getName().getString())).toList();
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public AppConfig getAppConfig(ApplicationId id) {
        int fd = this.stdLib.open("/apps/" + id.getGroup() + "/" + id.getModule() + ".json", 0);

        if (fd == -1) {
            return null;
        }

        try {
            long l = this.stdLib.fstat(fd).st_size();
            if (l > Integer.MAX_VALUE) throw new IOException("File too large");
            ByteBuffer buffer = ByteBuffer.allocate((int) l);
            this.stdLib.read(fd, buffer);
            buffer.flip();

            String json = new String(buffer.array());
            return GSON.fromJson(json, AppConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileDescriptorManager gerFdManager() {
        return this.fdManager;
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public LibStd getStdLib() {
        return this.stdLib;
    }

    public LibMineOS getMineOSLib() {
        return this.mineOSLib;
    }
}
