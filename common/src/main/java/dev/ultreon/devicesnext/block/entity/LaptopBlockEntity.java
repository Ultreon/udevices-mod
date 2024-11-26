package dev.ultreon.devicesnext.block.entity;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.cpu.CPU;
import dev.ultreon.devicesnext.device.Laptop;
import dev.ultreon.devicesnext.device.McDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LaptopBlockEntity extends DeviceBlockEntity {
    public LaptopBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public LaptopBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(UDevicesMod.LAPTOP_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    @Override
    protected McDevice createDevice() {
        return new Laptop(this, Laptop.Model.MINEBOOK_1);
    }

    public CPU getCPU() {
        return ((Laptop) this.getDevice()).getBlockEntity().getCPU();
    }
}
