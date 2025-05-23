package dev.ultreon.devicesnext.block.entity;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.client.DeviceManager;
import dev.ultreon.devicesnext.cpu.CPU;
import dev.ultreon.devicesnext.device.Laptop;
import dev.ultreon.devicesnext.device.McDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class LaptopBlockEntity extends DeviceBlockEntity {
    public LaptopBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public LaptopBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(UDevicesMod.LAPTOP_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(compoundTag, registries);

        DeviceManager.get(level).unregisterDevice(this.getDevice(), this);
        DeviceManager.get(level).registerDevice(this.getDevice(), this);
    }

    @Override
    protected McDevice createDevice() {
        return new Laptop(this, Laptop.Model.MINEBOOK_1);
    }

    public CPU getCPU() {
        return ((Laptop) this.getDevice()).getBlockEntity().getCPU();
    }
}
