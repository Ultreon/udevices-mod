package dev.ultreon.devicesnext;

import dev.ultreon.devicesnext.block.LaptopBlock;
import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.block.entity.LaptopBlockEntity;
import dev.ultreon.devicesnext.client.UDevicesModClient;
import dev.ultreon.devicesnext.device.McDevice;
import dev.ultreon.devicesnext.server.ServerGPU;
import dev.ultreon.devicesnext.util.Arguments;
import dev.ultreon.mods.xinexlib.Env;
import dev.ultreon.mods.xinexlib.EnvExecutor;
import dev.ultreon.mods.xinexlib.platform.XinexPlatform;
import dev.ultreon.mods.xinexlib.registrar.Registrar;
import dev.ultreon.mods.xinexlib.registrar.RegistrarManager;
import dev.ultreon.mods.xinexlib.registrar.RegistrySupplier;
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
import org.jetbrains.annotations.Nullable;
import org.jnode.driver.ApiNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UDevicesMod {
    public static final String MOD_ID = "udevices";
    public static final Logger LOGGER = LoggerFactory.getLogger("UDevicesRebooted");

    public static final RegistrarManager REGISTRAR_MANAGER = XinexPlatform.getRegistrarManager(MOD_ID);

    public static final Registrar<Block> BLOCKS = REGISTRAR_MANAGER.getRegistrar(Registries.BLOCK);
    public static final Registrar<Item> ITEMS = REGISTRAR_MANAGER.getRegistrar(Registries.ITEM);
    public static final Registrar<BlockEntityType<?>> BLOCK_ENTITIES = REGISTRAR_MANAGER.getRegistrar(Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<LaptopBlock, Block> LAPTOP_BLOCK = BLOCKS.register("laptop", () -> new LaptopBlock(Block.Properties.of()));
    public static final RegistrySupplier<BlockItem, Item> LAPTOP_ITEM = ITEMS.register("laptop", () -> new BlockItem(LAPTOP_BLOCK.get(), new BlockItem.Properties()));
    public static final RegistrySupplier<BlockEntityType<? extends LaptopBlockEntity>, BlockEntityType<?>> LAPTOP_BLOCK_ENTITY = BLOCK_ENTITIES.register("laptop", () -> BlockEntityType.Builder.of(LaptopBlockEntity::new, LAPTOP_BLOCK.get()).build(null));
    private static MinecraftServer server;

    public static void init() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> UDevicesModClient::init);

        BLOCKS.load();
        ITEMS.load();
        BLOCK_ENTITIES.load();
    }

    public static @Nullable Path getDataPath() {
        Path inEnvSpecific = EnvExecutor.getInEnvSpecific(
                () -> () -> getCurrentServer().getWorldPath(LevelResource.ROOT).resolve("data/ultreon_studios/devices_squared/"),
                () -> () -> Path.of("data/ultreon_studios/devices_squared/")
        );
        if (!Files.notExists(inEnvSpecific)) {
            try {
                Files.createDirectories(inEnvSpecific);
            } catch (IOException e) {
                LOGGER.error("Failed to create data directory", e);
                return null;
            }
        }
        return inEnvSpecific;
    }

    public static void onGfxCallback(ServerPlayer player, BlockPos pos, int ptr, Arguments args) {
        MinecraftServer currentServer = UDevicesMod.getCurrentServer();
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
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static MinecraftServer getCurrentServer() {
        return server;
    }
}
