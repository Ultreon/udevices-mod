package dev.ultreon.devicesnext.virtual;

import org.graalvm.polyglot.Value;

@SuppressWarnings("unused")
public interface VirtualProcessApi {
    void kill(int code);

    int pid();

    default void update() {

    }

    void setOnExit(Value handler);

    static VirtualProcessApi of(VirtualProcess virtualProcess) {
        return new VirtualProcessApi() {
            @Override
            public void kill(int code) {
                virtualProcess.kill(code);
            }

            @Override
            public int pid() {
                return virtualProcess.getPid();
            }

            @Override
            public void setOnExit(Value handler) {
                virtualProcess.setOnExit(handler::executeVoid);
            }
        };
    }
}
