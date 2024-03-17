package com.ultreon.mods.lib.client.devicetest;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ultreon.libs.commons.v0.Color;
import com.ultreon.libs.commons.v0.util.ExceptionUtils;
import com.ultreon.mods.lib.UltreonLib;
import com.ultreon.mods.lib.client.devicetest.security.Permission;
import com.ultreon.mods.lib.client.devicetest.security.SpawnApplicationPermission;
import com.ultreon.mods.lib.client.gui.screen.test.device.TestEmptyApplication;
import com.ultreon.mods.lib.client.devicetest.exception.McAccessDeniedException;
import com.ultreon.mods.lib.client.devicetest.exception.McAppNotFoundException;
import com.ultreon.mods.lib.client.devicetest.exception.McNoPermissionException;
import com.ultreon.mods.lib.client.devicetest.exception.McSecurityException;
import com.ultreon.mods.lib.client.devicetest.sizing.IntSize;
import com.ultreon.mods.lib.input.GameKeyboard;
import com.ultreon.mods.lib.input.GameKeyboard.Modifier;
import com.ultreon.mods.lib.util.ScissorStack;
import it.unimi.dsi.fastutil.objects.Reference2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

final class OperatingSystemImpl extends WindowManager implements OperatingSystem {
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
    private final ScheduledExecutorService shutdownScheduler = Executors.newScheduledThreadPool(1);
    private List<Application> crashing = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public OperatingSystemImpl(DeviceScreen screen, int width, int height, ArrayList<Window> windows, DesktopApplication desktopApp) {
        this(screen, 0, 0, width, height, windows, desktopApp);
    }

    public OperatingSystemImpl(DeviceScreen screen, int x, int y, int width, int height, ArrayList<Window> windows, DesktopApplication desktopApp) {
        super(x, y, width, height, windows);

        instance = this;

        try {
            // Register apps and spawn kernel
            this._spawn(this.kernel, new String[]{});
            this.registerApp(this.kernel.getId(), () -> this.kernel);
            this.registerApp(DesktopApplication.id(), () -> desktopApp);
            this.registerApp(TestEmptyApplication.id(), TestEmptyApplication::new);

            // Setup permissions
            this.permissionManager.grantPermission(DesktopApplication.id(), Permission.SHUTDOWN);
            this.permissionManager.grantPermission(DesktopApplication.id(), Permission.LIST_APPLICATIONS);
            this.permissionManager.grantPermission(DesktopApplication.id(), Permission.SPAWN_APPLICATIONS);

            this.screen = screen;
            this.desktopApp = desktopApp;
            this._spawn(desktopApp, new String[0]);

            this.desktop = desktopApp.getDesktop();
            this.taskbar = desktopApp.getTaskbar();
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
            UltreonLib.LOGGER.warn("Application cooldown for " + application.getId());
            return false;
        }

        this.openApps.add(application);
        try {
            application._main(this, this, argv, this.pid++);
            return true;
        } catch (Exception e) {
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

        this.scheduler.schedule(() -> {
            _spawn(applications.get(application.getId()).create(), application.getArgv());
        }, 5, TimeUnit.SECONDS);

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

        this.createWindow(new ShutdownWindow(kernel, 0, 0, width, height, "Shutting down..."));

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

    private void _raiseHardError(Throwable throwable) {
        this.openApps.clear();
        this.windows.clear();

        this.shutdownTime = System.currentTimeMillis() + 10000;
        this.bsod = new Bsod(throwable);
        UltreonLib.LOGGER.error("System hard error:", throwable);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.bsod != null) return false;

        try {
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

            Window activeWindow = getActiveWindow();
            try {
                if ((keyCode == InputConstants.KEY_Q && GameKeyboard.isKeyDown(metaKey)) || (keyCode == InputConstants.KEY_F4 && GameKeyboard.isAltDown())) {
                    activeWindow.close();
                    return true;
                }
                if (GameKeyboard.isKeyDown(metaKey)) {
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
                }
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
            gfx.drawString(this.font, ChatFormatting.BOLD + "Your Minecraft ran into a problem and needs to restart.", 6, 25, 0xffffffff, false);
            gfx.drawString(this.font, "Shutting down in: " + (Mth.ceil(millisRemaining / 1000f)), 20, 10, 0xb0ffffff, false);
            AtomicInteger i = new AtomicInteger(0);
            ExceptionUtils.getStackTrace(this.bsod.throwable()).lines().forEachOrdered(s -> {
                gfx.drawString(this.font, s.replaceAll("\t", "      "), 6, 40 + (i.addAndGet(this.font.lineHeight)), 0xb0ffffff, false);
            });

            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            return;
        }

        if (this.windows.isEmpty() && !this.shuttingDown) {
            this.autoShutdownToken = new ShutdownToken();
            this._delayShutdown(this.autoShutdownToken, new ShutdownTimer(5000));
            return;
        } else if (this.autoShutdownToken != null) {
            this.cancelShutdown(this.autoShutdownToken);
        }

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
    public void resize(int width, int height) {

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        if (this.bsod != null) return false;

        try {
            return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
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
}
