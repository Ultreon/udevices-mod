package com.ultreon.mods.lib.client.devicetest;

public abstract class KeyboardHook {
    public KeyboardHook keyPressed(int keyCode, int scanCode, int modifiers, KeyboardHook next) {
        return next;
    }

    public KeyboardHook keyReleased(int keyCode, int scanCode, int modifiers, KeyboardHook next) {
        return next;
    }

    public KeyboardHook charTyped(char character, int modifiers, KeyboardHook next) {
        return next;
    }
}
