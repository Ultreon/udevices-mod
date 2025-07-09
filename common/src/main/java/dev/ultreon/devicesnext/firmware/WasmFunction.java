package dev.ultreon.devicesnext.firmware;

import io.github.kawamuray.wasmtime.Val;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface WasmFunction {
    String name();
}
