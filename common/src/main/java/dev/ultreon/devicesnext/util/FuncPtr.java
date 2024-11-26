package dev.ultreon.devicesnext.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FuncPtr {
    int value();
}
