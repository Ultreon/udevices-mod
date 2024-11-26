package dev.ultreon.devicesnext.block.entity;

import dev.ultreon.devicesnext.device.McDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class DeviceBlockEntity extends BlockEntity {
    private final McDevice device;

    public DeviceBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);

        this.device = createDevice();
    }

    protected abstract McDevice createDevice();

    public McDevice getDevice() {
        return this.device;
    }
}
