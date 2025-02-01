package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.UDevicesMod;

public class OsLoggerImpl implements OsLogger {
    @Override
    public void debug(String message) {
        UDevicesMod.LOGGER.debug(message);
    }

    @Override
    public void debug(String message, Throwable t) {
        UDevicesMod.LOGGER.debug(message, t);
    }

    @Override
    public void verbose(String message) {
        if (!OperatingSystemImpl.get().kernel.isVerbose()) return;
        UDevicesMod.LOGGER.debug(message);
    }

    @Override
    public void verbose(String message, Throwable t) {
        if (!OperatingSystemImpl.get().kernel.isVerbose()) return;
        UDevicesMod.LOGGER.debug(message, t);
    }

    @Override
    public void info(String message) {
        UDevicesMod.LOGGER.info(message);
    }

    @Override
    public void info(String message, Throwable t) {
        UDevicesMod.LOGGER.info(message, t);
    }

    @Override
    public void warn(String message) {
        UDevicesMod.LOGGER.warn(message);
    }

    @Override
    public void warn(String message, Throwable t) {
        UDevicesMod.LOGGER.warn(message, t);
    }

    @Override
    public void error(String message) {
        UDevicesMod.LOGGER.error(message);
    }

    @Override
    public void error(String message, Throwable t) {
        UDevicesMod.LOGGER.error(message, t);
    }

    @Override
    public void fatal(String message) {
        UDevicesMod.LOGGER.error("<FATAL> " + message);
    }

    @Override
    public void fatal(String message, Throwable t) {
        UDevicesMod.LOGGER.error("<FATAL> " + message, t);
    }
}
