package dev.ultreon.devicesnext.firmware;

import io.github.kawamuray.wasmtime.Memory;
import io.github.kawamuray.wasmtime.Store;

import java.util.function.Supplier;

public interface WasmProxy {

    void load(Supplier<Memory> memorySupplier, Store<?> store);
}
