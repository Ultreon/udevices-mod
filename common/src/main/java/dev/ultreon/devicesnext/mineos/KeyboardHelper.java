package dev.ultreon.devicesnext.mineos;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

public class KeyboardHelper {

    public static boolean isKeyDown(Modifier metaKey) {
        if (metaKey == null) return false;
        return switch (metaKey) {
            case NONE -> false;
            case ALT -> isAltDown();
            case CONTROL -> isControlDown();
            case SHIFT -> isShiftDown();
            case META -> isMetaDown();
        };
    }

    public static boolean isAltDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LALT) || InputConstants.isKeyDown(window, InputConstants.KEY_RALT);
    }

    public static boolean isShiftDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT);
    }

    public static boolean isControlDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
    }

    public static boolean isMetaDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LWIN) || InputConstants.isKeyDown(window, InputConstants.KEY_RWIN);
    }
}
