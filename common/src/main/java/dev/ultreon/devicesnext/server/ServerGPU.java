package dev.ultreon.devicesnext.server;

import dev.ultreon.devicesnext.api.ConnectedClient;
import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.network.packets.GfxCallPacket;
import dev.ultreon.devicesnext.util.FuncPtr;
import dev.ultreon.devicesnext.gpu.GPU;
import dev.ultreon.devicesnext.cpu.CpuFunctionMap;
import dev.ultreon.devicesnext.gpu.GfxFunctionMap;
import dev.ultreon.devicesnext.gpu.GraphicsOutput;
import dev.ultreon.devicesnext.network.packets.GfxCallbackPacket;
import dev.ultreon.devicesnext.util.Arguments;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class ServerGPU implements GPU, GraphicsOutput {
    private final List<ConnectedClient> connected = new ArrayList<>();
    private final DeviceBlockEntity blockEntity;
    private String error;

    public ServerGPU(DeviceBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public void fatal(String message) {
        Arguments arguments = new Arguments(1);
        arguments.setString(0, message);
        this.sendPacket(new GfxCallbackPacket(blockEntity.getBlockPos(), CpuFunctionMap.GFX_ERROR, arguments));
    }

    @Override
    public String getError() {
        return error;
    }

    @FuncPtr(GfxFunctionMap.GFX_FILL_RECT)
    @Override
    public void fillRect(int x, int y, int width, int height, int color) {
        Arguments arguments = new Arguments(5);
        arguments.setInt(0, x);
        arguments.setInt(1, y);
        arguments.setInt(2, width);
        arguments.setInt(3, height);
        arguments.setInt(4, color);
        this.sendPacket(new GfxCallbackPacket(blockEntity.getBlockPos(), GfxFunctionMap.GFX_FILL_RECT, arguments));
    }

    @FuncPtr(GfxFunctionMap.GFX_BOX_RECT)
    @Override
    public void boxRect(int x, int y, int width, int height, int color) {
        Arguments arguments = new Arguments(5);
        arguments.setInt(0, x);
        arguments.setInt(1, y);
        arguments.setInt(2, width);
        arguments.setInt(3, height);
        arguments.setInt(4, color);
        this.sendPacket(new GfxCallbackPacket(blockEntity.getBlockPos(), GfxFunctionMap.GFX_BOX_RECT, arguments));
    }

    @FuncPtr(GfxFunctionMap.GFX_LINE)
    @Override
    public void line(int x1, int y1, int x2, int y2, int color) {
        Arguments arguments = new Arguments(5);
        arguments.setInt(0, x1);
        arguments.setInt(1, y1);
        arguments.setInt(2, x2);
        arguments.setInt(3, y2);
        arguments.setInt(4, color);
        this.sendPacket(new GfxCallbackPacket(blockEntity.getBlockPos(), GfxFunctionMap.GFX_LINE, arguments));
    }

    @FuncPtr(GfxFunctionMap.GFX_TEXT)
    @Override
    public void text(String text, int x, int y, int color) {
        Arguments arguments = new Arguments(4);
        arguments.setString(0, text);
        arguments.setInt(1, x);
        arguments.setInt(2, y);
        arguments.setInt(3, color);
        this.sendPacket(new GfxCallbackPacket(blockEntity.getBlockPos(), GfxFunctionMap.GFX_TEXT, arguments));
    }

    public void sendPacket(GfxCallbackPacket gfxCallbackPacket) {
        // Meow :3
    }

    @Override
    public void setError(String message) {
        error = message;
    }

    public void onGfxCallback(ServerPlayer player, int ptr, Arguments args) {
        if (ptr == CpuFunctionMap.GFX_ERROR) {
            this.error = args.getString(0);
        }
    }

    @Override
    public void tick() {

    }

    @Override
    public void close() {
        for (ConnectedClient connectedClient : connected) {
            connectedClient.sendPacket(new GfxCallPacket(blockEntity.getBlockPos(), GfxFunctionMap.GFX_CLOSE, null));
        }
    }
}
