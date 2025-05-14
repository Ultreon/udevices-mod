package dev.ultreon.devicesnext.client;

import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.mineos.DeviceScreen;
import dev.ultreon.devicesnext.util.FuncPtr;
import dev.ultreon.devicesnext.gpu.GPU;
import dev.ultreon.devicesnext.gpu.GraphicsOutput;
import dev.ultreon.devicesnext.gpu.GfxFunctionMap;
import dev.ultreon.devicesnext.network.UDevicesNet;
import dev.ultreon.devicesnext.network.packets.GfxCallbackPacket;
import dev.ultreon.mods.xinexlib.network.Networker;
import net.minecraft.client.Minecraft;

public class ClientGPU implements GPU, GraphicsOutput {
    private final DeviceBlockEntity blockEntity;
    private final GraphicsOutput display;
    private String error;

    public ClientGPU(DeviceBlockEntity blockEntity, GraphicsOutput display) {
        this.blockEntity = blockEntity;
        this.display = display;
    }

    @FuncPtr(GfxFunctionMap.GFX_GET_ERROR)
    @Override
    public String getError() {
        return error;
    }

    @FuncPtr(GfxFunctionMap.GFX_FILL_RECT)
    @Override
    public void fillRect(int x, int y, int width, int height, int color) {
        Minecraft.getInstance().execute(() -> {
            display.fillRect(x, y, width, height, color);
        });
    }

    @FuncPtr(GfxFunctionMap.GFX_BOX_RECT)
    @Override
    public void boxRect(int x, int y, int width, int height, int color) {
        Minecraft.getInstance().execute(() -> {
            display.boxRect(x, y, width, height, color);
        });
    }

    @FuncPtr(GfxFunctionMap.GFX_LINE)
    @Override
    public void line(int x1, int y1, int x2, int y2, int color) {
        Minecraft.getInstance().execute(() -> {
            display.line(x1, y1, x2, y2, color);
        });
    }

    @FuncPtr(GfxFunctionMap.GFX_TEXT)
    @Override
    public void text(String text, int x, int y, int color) {
        Minecraft.getInstance().execute(() -> {
            display.text(text, x, y, color);
        });
    }

    @FuncPtr(GfxFunctionMap.GFX_CLOSE)
    @Override
    public void close() {
        if (Minecraft.getInstance().screen instanceof DeviceScreen)
            Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void fatal(String message) {


    }

    @Override
    public void setError(String message) {

    }

    @Override
    public void sendPacket(GfxCallbackPacket packet) {
        Networker network = UDevicesNet.get();
        network.sendToServer(packet);
    }

    @Override
    public void tick() {

    }
}
