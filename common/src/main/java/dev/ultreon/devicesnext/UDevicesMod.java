package dev.ultreon.devicesnext;

import com.ultreon.mods.lib.util.ServerLifecycle;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.ultreon.devicesnext.block.LaptopBlock;
import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.block.entity.LaptopBlockEntity;
import dev.ultreon.devicesnext.client.UDevicesModClient;
import dev.ultreon.devicesnext.device.McDevice;
import dev.ultreon.devicesnext.mineos.OsLogger;
import dev.ultreon.devicesnext.server.ServerGPU;
import dev.ultreon.devicesnext.util.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.LevelResource;
import org.jnode.driver.ApiNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class UDevicesMod {
    public static final LevelResource UDEVICES = new LevelResource("udevices");
    public static final String MOD_ID = "udevices";
    public static final Logger LOGGER = LoggerFactory.getLogger("UDevicesRebooted");

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> LAPTOP_BLOCK = BLOCKS.register("laptop", () -> new LaptopBlock(Block.Properties.of()));
    public static final RegistrySupplier<BlockItem> LAPTOP_ITEM = ITEMS.register("laptop", () -> new BlockItem(LAPTOP_BLOCK.get(), new BlockItem.Properties()));
    public static final RegistrySupplier<BlockEntityType<LaptopBlockEntity>> LAPTOP_BLOCK_ENTITY = BLOCK_ENTITIES.register("laptop", () -> BlockEntityType.Builder.of(LaptopBlockEntity::new, LAPTOP_BLOCK.get()).build(null));

    public static void init() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> UDevicesModClient::init);

        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
    }

    public static Path getDataPath() {
        return EnvExecutor.getEnvSpecific(
                () -> () -> ServerLifecycle.getCurrentServer().getWorldPath(UDEVICES),
                () -> () -> Path.of("udevices")
        );
    }

    public static void onGfxCallback(ServerPlayer player, BlockPos pos, int ptr, Arguments args) {
        MinecraftServer currentServer = ServerLifecycle.getCurrentServer();
        if (currentServer == null) return;

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof DeviceBlockEntity deviceBlockEntity)) return;
        McDevice device = deviceBlockEntity.getDevice();
        if (!device.implementsAPI(ServerGPU.class)) return;

        try {
            ServerGPU gpu = device.getAPI(ServerGPU.class);
            gpu.onGfxCallback(player, ptr, args);
        } catch (ApiNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static ResourceLocation res(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
