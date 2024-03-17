package com.ultreon.mods.lib.client.devicetest.gui;

import net.minecraft.network.chat.Component;

public class McMenuItem {
    private Component name;
    private final Callback callback;

    public McMenuItem(Component name, Callback callback) {
        this.name = name;
        this.callback = callback;
    }

    public Component getName() {
        return name;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public void call() {
        callback.call(this);
    }

    @FunctionalInterface
    public interface Callback {
        void call(McMenuItem item);
    }
}
