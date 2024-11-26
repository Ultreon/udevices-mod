package dev.ultreon.devicesnext.gpu;

import dev.ultreon.devicesnext.util.FuncPtr;
import dev.ultreon.devicesnext.network.packets.GfxCallbackPacket;

public interface GraphicsOutput {
    @FuncPtr(GfxFunctionMap.GFX_GET_ERROR)
    String getError();

    @FuncPtr(GfxFunctionMap.GFX_FILL_RECT)
    void fillRect(int x, int y, int width, int height, int color);

    @FuncPtr(GfxFunctionMap.GFX_BOX_RECT)
    void boxRect(int x, int y, int width, int height, int color);

    @FuncPtr(GfxFunctionMap.GFX_LINE)
    void line(int x1, int y1, int x2, int y2, int color);

    @FuncPtr(GfxFunctionMap.GFX_TEXT)
    void text(String text, int x, int y, int color);

    @FuncPtr(GfxFunctionMap.GFX_CLOSE)
    void close();

    void sendPacket(GfxCallbackPacket gfxCallbackPacket);
}
