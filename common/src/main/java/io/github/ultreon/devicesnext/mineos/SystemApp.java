package com.ultreon.mods.lib.client.devicetest;

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
