package dev.ultreon.devicesnext.mineos;

import com.google.common.collect.Lists;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
final class Kernel extends SystemApp {
    private boolean verbose;

    public Kernel() {
        super("dev.ultreon:kernel");
    }

    @Override
    public void create() {
        super.create();

        var argv = Lists.newArrayList(this.getArgv());
        this.verbose = argv.remove("-v");
    }

    public boolean isVerbose() {
        return verbose;
    }
}
