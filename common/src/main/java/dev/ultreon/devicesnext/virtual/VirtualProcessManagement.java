package dev.ultreon.devicesnext.virtual;

import org.graalvm.polyglot.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class VirtualProcessManagement {
    private static final WeakHashMap<Context, List<VirtualProcess>> processes = new WeakHashMap<>();

    public static void add(Context current, VirtualProcess virtualProcess) {
        List<VirtualProcess> virtualProcesses = processes.computeIfAbsent(current, context -> new ArrayList<>());
        if (virtualProcesses.size() > 64) {
            throw new IllegalStateException("Process limit reached!");
        }
        virtualProcesses.add(virtualProcess);
    }

    public static void remove(Context parent, VirtualProcess virtualProcess) {
        List<VirtualProcess> virtualProcesses = processes.get(parent);
        if (virtualProcesses == null) return;

        virtualProcesses.remove(virtualProcess);
    }

    public static void shutdown(Context codeContext) {
        List<VirtualProcess> virtualProcesses = processes.remove(codeContext);
        if (virtualProcesses != null) {
            for (VirtualProcess virtualProcess : virtualProcesses) {
                virtualProcess.kill(1);
            }
        }
    }
}
