package dev.ultreon.devicesnext.firmware.api;

import dev.ultreon.devicesnext.filesystem.FS;
import dev.ultreon.devicesnext.firmware.VEfiApplication;
import dev.ultreon.devicesnext.firmware.WasmFunction;
import dev.ultreon.devicesnext.firmware.WasmModule;
import dev.ultreon.devicesnext.firmware.WasmProxy;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jnode.driver.block.BlockDeviceAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

@WasmModule(name = "vefi")
public class VEfiExt extends VEfi {
    private final FS fs;
    private final BlockDeviceAPI device;

    public VEfiExt(FS fs) {
        super();
        this.fs = fs;
        this.device = fs.getBlockDevice();
    }

    @WasmFunction(name = "vefi_read_drive")
    public int vEfiReadDrive(int drive, long offset, int size, int out) {
        ByteBuffer buf = getMemory().buffer(getStore());
        buf.position(out);
        try {
            device.read(offset, buf);
        } catch (IOException e) {
            return 0;
        }
        return 1;
    }

    @WasmFunction(name = "vefi_write_drive")
    public int vEfiWriteDrive(int drive, long offset, int size, int data) {
        ByteBuffer buf = getMemory().buffer(getStore());
        buf.position(data);
        try {
            device.write(offset, buf);
        } catch (IOException e) {
            return 0;
        }
        return 1;
    }

    @WasmFunction(name = "vefi_open_drive")
    public int vEfiOpenDrive(int drive) {
        if (drive != 0) return 0;
        return 1;
    }

    @WasmFunction(name = "vefi_close_drive")
    public int vEfiCloseDrive(int drive) {
        if (drive != 0) return 0;
        return 1;
    }

    @WasmFunction(name = "vefi_get_main_drive")
    public int vEfiGetMainDrive() {
        return 0;
    }

    public FS getFs() {
        return fs;
    }
}
