package dev.ultreon.devicesnext.device;

import dev.ultreon.devicesnext.block.entity.LaptopBlockEntity;
import dev.ultreon.devicesnext.client.ClientDeviceManager;
import dev.ultreon.devicesnext.cpu.CPU;
import dev.ultreon.devicesnext.mineos.VirtualComputer;
import dev.ultreon.mods.xinexlib.Env;
import dev.ultreon.mods.xinexlib.EnvExecutor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

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

//        Drive drive = DriveManager.get(UDevicesMod.getServer()).create(this, Drive.Class.MEDIUM);
        GraphicsDevice gpu = new GraphicsDevice(laptopBlock, UUID.randomUUID());
//        this.registerComponent(drive);
        this.registerComponent(gpu);

//        this.registerAPI(Drive.class, drive);
        this.registerAPI(GraphicsDevice.class, gpu);
        this.registerAPI(CPU.class, laptopBlock.getCPU());
    }

    public LaptopBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public void open(@NotNull Player player) {
        Level level = blockEntity.getLevel();
        if (level == null) return;
        if (level.isClientSide()) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                ClientDeviceManager.get().registerDevice(this, blockEntity);
            });
        }
    }

    @Override
    public void close(@NotNull Player player) {
        Level level = blockEntity.getLevel();
        if (level == null) return;
        if (level.isClientSide()) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                ClientDeviceManager.get().unregisterDevice(this, blockEntity);
            });
        }
    }

    @Override
    public void connectDisplay(@NotNull Player player) {
        super.connectDisplay(player);

        VirtualComputer screen = new VirtualComputer(new VirtualComputer.LaunchOptions().fullscreen().title(Component.literal("MineOS")));
        screen.open();
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
