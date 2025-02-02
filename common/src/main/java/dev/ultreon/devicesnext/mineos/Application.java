package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.api.OperatingSystem;
import dev.ultreon.devicesnext.mineos.exception.McAppNotFoundException;
import dev.ultreon.devicesnext.mineos.exception.McSecurityException;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class Application {
    final Set<Window> windows = new CopyOnWriteArraySet<>();
    private final ApplicationId id;
    private OperatingSystemImpl system;
    private String[] argv;
    private long pid;
    WindowManager wm;

    public Application(String id) {
        this(new ApplicationId(id));
    }

    public Application(String group, String module) {
        this(new ApplicationId(group, module));
    }

    public Application(ApplicationId id) {
        this.id = id;
    }

    public final ApplicationId getId() {
        return id;
    }

    public void create() {

    }

    void _main(OperatingSystemImpl system, WindowManager wm, String[] argv, long pid) {
        this.system = system;
        this.wm = wm;
        this.argv = argv;
        this.create();
        this.pid = pid;
    }

    public final boolean isService() {
        return system.getAppConfig(id).runAsService;
    }

    public boolean isOpenOnlyOne() {
        return false;
    }

     public void createWindow(Window window) {
        if (this.wm == null) return;
        this.wm.createWindow(window);
        this.windows.add(window);
    }

    public long getPid() {
        return pid;
    }

    protected final String[] getArgv() {
        return Arrays.copyOf(this.argv, this.argv.length);
    }

    public OperatingSystem getSystem() {
        return system;
    }

    public boolean spawnApplication(ApplicationId id, String... argv) throws McAppNotFoundException, McSecurityException {
        return this.system.spawn(this, id, argv);
    }

    public final void quit() {
        this.windows.forEach(Window::close);
        this.dispose();
    }

    protected void dispose() {

    }

    public void _destroyWindow(Window window) {
        this.windows.remove(window);
    }

    public void update() {

    }

    public final boolean isSame(Application application) {
        return this.id.equals(application.id);
    }

    public LibStd getLibStd() {
        return system.getStdLib();
    }

    public LibMineOS getMineOSLib() {
        return system.getMineOSLib();
    }
}
