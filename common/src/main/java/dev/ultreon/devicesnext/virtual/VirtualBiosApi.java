package dev.ultreon.devicesnext.virtual;

import dev.ultreon.devicesnext.mineos.VirtualComputer;
import dev.ultreon.devicesnext.mineos.gui.VirtualGpu;
import org.graalvm.polyglot.Value;

import java.util.Map;

@SuppressWarnings("unused")
@VirtualApi
public class VirtualBiosApi {
    private final VirtualComputer virtualComputer;

    public VirtualBiosApi(VirtualComputer virtualComputer) {
        this.virtualComputer = virtualComputer;
    }

    @Deprecated
    @VirtualApi
    public boolean isWorldLess() {
        return false;
    }

    @VirtualApi
    public VirtualProcessApi spawnProcess(Value modules, String init, String[] command, Map<String, String> env) {
        return virtualComputer.spawnProcess(modules, init, command, env).api();
    }

    @VirtualApi
    public VirtualProcessApi getProcess(int pid) {
        VirtualProcess process = virtualComputer.getProcess(pid);
        if (process == null) return null;
        return process.api();
    }

    @VirtualApi
    public VirtualGpu getGpu() {
        return virtualComputer.getGpu();
    }
}
