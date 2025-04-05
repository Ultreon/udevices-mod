package dev.ultreon.devicesnext.mineos;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

class ShutdownWindow extends Window {

    public ShutdownWindow(@NotNull Application application, int x, int y, int width, int height, String title) {
        super(application, x, y, width, height, title);
    }
}
