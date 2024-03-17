package com.ultreon.mods.lib.client.devicetest;

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

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

    @CanIgnoreReturnValue
    public boolean isVerbose() {
        return verbose;
    }
}
