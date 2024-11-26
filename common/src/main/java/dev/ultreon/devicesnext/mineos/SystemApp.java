package dev.ultreon.devicesnext.mineos;

sealed class SystemApp extends Application permits Kernel {
    public SystemApp(String id) {
        super(id);
    }

    public SystemApp(String group, String module) {
        super(group, module);
    }

    public SystemApp(ApplicationId id) {
        super(id);
    }
}
