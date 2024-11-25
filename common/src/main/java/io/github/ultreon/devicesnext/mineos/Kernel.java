package io.github.ultreon.devicesnext.mineos;

import com.google.common.collect.Lists;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
final class Kernel extends SystemApp {
    private boolean verbose;

    public Kernel() {
        super("com.ultreon:kernel");
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
