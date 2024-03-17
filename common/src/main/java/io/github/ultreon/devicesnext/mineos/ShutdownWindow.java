package com.ultreon.mods.lib.client.devicetest;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

class ShutdownWindow extends Window {

    public ShutdownWindow(@NotNull Application application, int x, int y, int width, int height, String title) {
        super(application, x, y, width, height, title);
    }

    public ShutdownWindow(Application application, int x, int y, int width, int height, Component title) {
        super(application, x, y, width, height, title);
    }
}
