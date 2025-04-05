package dev.ultreon.devicesnext.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class Constants {
    public static final AtomicReference<Path> DATA_REF = new AtomicReference<>();
    static final Logger LOGGER = LoggerFactory.getLogger("UDevices:CraftFS");
}
