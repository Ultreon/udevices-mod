package io.github.ultreon.devicesnext.api;

import com.ultreon.mods.lib.util.KeyboardHelper;
import io.github.ultreon.devicesnext.mineos.*;
import io.github.ultreon.devicesnext.mineos.exception.McNoPermissionException;
import io.github.ultreon.devicesnext.mineos.exception.McSecurityException;
import io.github.ultreon.devicesnext.mineos.sizing.IntSize;

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

    KeyboardHelper.Modifier getMetaKey();

    static OperatingSystem get() {
        return OperatingSystemImpl.get();
    }

    void loadWallpaper(File file);

    void loadWallpaper(Path path);

    void setColorBackground(Color color);

    int getWidth();

    int getHeight();

    Insets getClientAreaInsets();

    List<ApplicationId> getApplications(Application context) throws McSecurityException;
}
