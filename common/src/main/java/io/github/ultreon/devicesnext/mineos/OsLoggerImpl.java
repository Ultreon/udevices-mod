package com.ultreon.mods.lib.client.devicetest;

import com.ultreon.mods.lib.UltreonLib;

public class OsLoggerImpl implements OsLogger {
    @Override
    public void debug(String message) {
        UltreonLib.LOGGER.debug(message);
    }

    @Override
    public void debug(String message, Throwable t) {
        UltreonLib.LOGGER.debug(message, t);
    }

    @Override
    public void verbose(String message) {
        if (!OperatingSystemImpl.get().kernel.isVerbose()) return;
        UltreonLib.LOGGER.debug(message);
    }

    @Override
    public void verbose(String message, Throwable t) {
        if (!OperatingSystemImpl.get().kernel.isVerbose()) return;
        UltreonLib.LOGGER.debug(message, t);
    }

    @Override
    public void info(String message) {
        UltreonLib.LOGGER.info(message);
    }

    @Override
    public void info(String message, Throwable t) {
        UltreonLib.LOGGER.info(message, t);
    }

    @Override
    public void warn(String message) {
        UltreonLib.LOGGER.warn(message);
    }

    @Override
    public void warn(String message, Throwable t) {
        UltreonLib.LOGGER.warn(message, t);
    }

    @Override
    public void error(String message) {
        UltreonLib.LOGGER.error(message);
    }

    @Override
    public void error(String message, Throwable t) {
        UltreonLib.LOGGER.error(message, t);
    }

    @Override
    public void fatal(String message) {
        UltreonLib.LOGGER.error("<FATAL> " + message);
    }

    @Override
    public void fatal(String message, Throwable t) {
        UltreonLib.LOGGER.error("<FATAL> " + message, t);
    }
}
