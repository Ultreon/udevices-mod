package com.ultreon.mods.lib.client.devicetest;

import com.ultreon.mods.lib.client.devicetest.exception.McNoPermissionException;
import com.ultreon.mods.lib.client.devicetest.exception.McSecurityException;
import com.ultreon.mods.lib.client.devicetest.sizing.IntSize;
import com.ultreon.mods.lib.input.GameKeyboard;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface OperatingSystem {
    void shutdown(Application executor) throws McNoPermissionException;

    IntSize getScreenSize();

    void raiseHardError(Application executor, Throwable throwable) throws McNoPermissionException;

    OsLogger getLogger();

    void addKeyboardHook(KeyboardHook keyboardHook);

    void removeKeyboardHook(KeyboardHook keyboardHook);

    GameKeyboard.Modifier getMetaKey();

    static OperatingSystem get() {
        return OperatingSystemImpl.get();
    }

    void loadWallpaper(File file);

    void loadWallpaper(Path path);

    void setColorBackground(com.ultreon.libs.commons.v0.Color color);

    int getWidth();

    int getHeight();

    Insets getClientAreaInsets();

    List<ApplicationId> getApplications(Application context) throws McSecurityException;
}
