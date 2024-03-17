package com.ultreon.mods.lib.client.devicetest;

public interface OsLogger {
    void debug(String message);

    void debug(String message, Throwable t);

    void verbose(String message);

    void verbose(String message, Throwable t);

    void info(String message);

    void info(String message, Throwable t);

    void warn(String message);

    void warn(String message, Throwable t);

    void error(String message);

    void error(String message, Throwable t);

    void fatal(String message);

    void fatal(String message, Throwable t);
}
