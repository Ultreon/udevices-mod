package dev.ultreon.devicesnext.firmware;

import net.minecraft.nbt.CompoundTag;

public abstract class Firmware {
    protected Firmware() {

    }


    public abstract void saveState(CompoundTag tag);

    public abstract void loadState(CompoundTag tag);
}
