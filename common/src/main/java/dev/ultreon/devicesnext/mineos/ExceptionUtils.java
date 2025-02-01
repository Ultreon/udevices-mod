package dev.ultreon.devicesnext.mineos;

import com.google.common.base.CaseFormat;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        sw.append("Panic: ").append(throwable.getMessage()).append("\n");
        String name = throwable.getClass().getSimpleName();
        if (name.endsWith("Exception")) name = name.substring(0, name.length() - 9);
        name = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
        sw.append("Stop Code: ").append(name).append("\n\n");
        for (final StackTraceElement element : throwable.getStackTrace()) {
            sw.append("  -> ");
            sw.append(element.getClassName().replace(".", "/"));
            sw.append("::");
            sw.append(element.getMethodName());
            sw.append("+0x");
            try {
                sw.append(Long.toUnsignedString((long) Class.forName(element.getClassName()).hashCode() << 6 & 0xFFFFFFFFFF000000L | (element.getLineNumber() & 0x00000000FFFFFFFFL), 16));
            } catch (ClassNotFoundException e) {
                sw.append("0000000000000000");
            }
            sw.append("\n");
        }

        if (throwable.getCause() != null) {
            sw.append("--- Cause: ").append(throwable.getCause().getClass().getSimpleName()).append("\n");
            sw.append(ExceptionUtils.getStackTrace(throwable.getCause()));
        }
        return sw.toString();
    }
}
