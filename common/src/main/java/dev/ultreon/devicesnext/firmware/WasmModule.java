package dev.ultreon.devicesnext.firmware;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface WasmModule {
    String name() default "";
}
