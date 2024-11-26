package dev.ultreon.devicesnext.device;

import com.ultreon.mods.lib.util.ServerLifecycle;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.ultreon.devicesnext.block.entity.LaptopBlockEntity;
import dev.ultreon.devicesnext.client.ClientDeviceManager;
import dev.ultreon.devicesnext.cpu.CPU;
import dev.ultreon.devicesnext.device.hardware.Drive;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.UUID;

public class Laptop extends McDevice {
    private final LaptopBlockEntity blockEntity;

    public Laptop(LaptopBlockEntity laptopBlock, Model model) {
        super(model.getId(), UUID.randomUUID());
        this.blockEntity = laptopBlock;

        Level level = laptopBlock.getLevel();
        if (level == null) return;
        if (level.isClientSide()) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                ClientDeviceManager.get().registerDevice(this, laptopBlock);
            });
        }

        try {
            Drive drive = DriveManager.get(ServerLifecycle.getCurrentServer()).create(this, Drive.Class.MEDIUM);
            GraphicsDevice gpu = new GraphicsDevice(laptopBlock, UUID.randomUUID());
            this.registerComponent(drive);
            this.registerComponent(gpu);

            this.registerAPI(Drive.class, drive);
            this.registerAPI(GraphicsDevice.class, gpu);
            this.registerAPI(CPU.class, laptopBlock.getCPU());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LaptopBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public enum Model {
        MINEBOOK_1("generic_x");

        private final String id;

        Model(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }
}
