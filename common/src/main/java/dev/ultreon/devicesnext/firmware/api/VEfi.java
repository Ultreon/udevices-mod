package dev.ultreon.devicesnext.firmware.api;

import dev.ultreon.devicesnext.firmware.VEfiApplication;
import dev.ultreon.devicesnext.firmware.WasmFunction;
import dev.ultreon.devicesnext.firmware.WasmModule;
import dev.ultreon.devicesnext.firmware.WasmProxy;
import io.github.kawamuray.wasmtime.Memory;
import io.github.kawamuray.wasmtime.Store;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@WasmModule(name = "vefi")
public class VEfi implements WasmProxy {
    public static final int InvalidParameter = 0;
    public static final int Unsupported = 1;
    public static final int OutOfResources = 2;
    public static final int DeviceError = 3;
    public static final int NoMedia = 4;
    public static final int SecurityViolation = 5;
    public static final int NullPointer = 6;
    public static final int NotReady = 7;
    public static final int DeviceBusy = 8;
    public static final int WriteProtected = 9;
    public static final int VolumeCorrupted = 10;
    public static final int VolumeFull = 11;
    public static final int NoMediaInDevice = 12;
    public static final int MediaChanged = 13;
    public static final int IdMismatch = 14;
    public static final int MediaNotSupported = 15;
    public static final int Generic = 16;
    public static final int UnknownFSError = 16;

    protected static final Int2ObjectMap<Logger> LOGGER = new Int2ObjectArrayMap<>();
    protected static final Logger log = LoggerFactory.getLogger("UDevices:VEFI");
    protected int errorCode;
    protected int id;
    private Supplier<Memory> memorySupplier;
    private Store<?> store;

    public VEfi() {

    }

    @Override
    public void load(Supplier<Memory> memorySupplier, Store<?> store) {
        this.memorySupplier = memorySupplier;
        this.store = store;
    }

    @WasmFunction(name = "print")
    public void print(String message) {
        System.out.println("[WASM] " + message);
    }

    @WasmFunction(name = "vefi_release_logger")
    public void vEfiReleaseLogger(int logger_id) {
        if (!LOGGER.containsKey(logger_id)) {
            VEfiApplication.LOGGER.error("Invalid logger ID: {}", logger_id);
            errorCode = NullPointer;
            return;
        }
        LOGGER.remove(logger_id);
    }

    @WasmFunction(name = "vefi_debug")
    public void vEfiDebug(int logger_id, String message) {
        Logger logger = LOGGER.get(logger_id);
        if (logger == null) {
            VEfiApplication.LOGGER.error("Invalid logger ID: {}", logger_id);
            errorCode = NullPointer;
            return;
        }

        logger.debug(message);
    }

    @WasmFunction(name = "vefi_info")
    public void vEfiInfo(int logger_id, String message) {
        Logger logger = LOGGER.get(logger_id);
        if (logger == null) {
            VEfiApplication.LOGGER.error("Invalid logger ID: {}", logger_id);
            errorCode = NullPointer;
            return;
        }

        logger.info(message);
    }

    @WasmFunction(name = "vefi_warn")
    public void vEfiWarn(int logger_id, String message) {
        Logger logger = LOGGER.get(logger_id);
        if (logger == null) {
            VEfiApplication.LOGGER.error("Invalid logger ID: {}", logger_id);
            errorCode = NullPointer;
            return;
        }

        logger.warn(message);
    }

    @WasmFunction(name = "vefi_error")
    public void vEfiError(int logger_id, String message) {
        Logger logger = LOGGER.get(logger_id);
        if (logger == null) {
            VEfiApplication.LOGGER.error("Invalid logger ID: {}", logger_id);
            errorCode = NullPointer;
            return;
        }

        logger.error(message);
    }

    @WasmFunction(name = "vefi_get_logger")
    public int vEfiGetLogger(String name) {
        int id = nextId();
        LOGGER.putIfAbsent(id, LoggerFactory.getLogger("UDevices:VEFI:" + name));
        return id;
    }

    @WasmFunction(name = "vefi_get_error")
    public int vEfiReleaseLogger() {
        return errorCode;
    }

    private int nextId() {
        return id++;
    }

    public Memory getMemory() {
        return memorySupplier.get();
    }

    public Store<?> getStore() {
        return store;
    }
}
