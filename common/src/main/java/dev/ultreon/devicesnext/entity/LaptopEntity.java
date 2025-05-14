package dev.ultreon.devicesnext.entity;

import dev.ultreon.devicesnext.device.hardware.Drive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class LaptopEntity extends Entity {
    private Drive drive;

    public LaptopEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        UUID dataUUID = NbtUtils.loadUUID(compoundTag.getCompound("DataUUID"));
        this.drive = loadDrive(dataUUID);
    }

    private Drive loadDrive(UUID dataUUID) {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }
}
