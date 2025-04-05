package dev.ultreon.devicesnext;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;
import dev.ultreon.devicesnext.block.LaptopBlock;
import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.block.entity.LaptopBlockEntity;
import dev.ultreon.devicesnext.client.UDevicesModClient;
import dev.ultreon.devicesnext.device.McDevice;
import dev.ultreon.devicesnext.mineos.gui.McContainer;
import dev.ultreon.devicesnext.mineos.gui.McWidget;
import dev.ultreon.devicesnext.network.UDevicesNet;
import dev.ultreon.devicesnext.server.ServerGPU;
import dev.ultreon.devicesnext.util.Arguments;
import dev.ultreon.mods.xinexlib.Env;
import dev.ultreon.mods.xinexlib.EnvExecutor;
import dev.ultreon.mods.xinexlib.client.event.ClientStartedEvent;
import dev.ultreon.mods.xinexlib.client.event.ClientStoppedEvent;
import dev.ultreon.mods.xinexlib.event.server.ServerStartedEvent;
import dev.ultreon.mods.xinexlib.event.server.ServerStoppingEvent;
import dev.ultreon.mods.xinexlib.event.system.EventSystem;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.LevelResource;
import org.jnode.driver.ApiNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Cleaner;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UDevicesMod {
    public static final LevelResource UDEVICES = LevelResource.ROOT;
    public static final String MOD_ID = "udevices";
    public static final Logger LOGGER = LoggerFactory.getLogger("UDevicesRebooted");
    
    public static final RegistrarManager REGISTRAR_MANAGER = XinexPlatform.getRegistrarManager(MOD_ID);

    public static final Registrar<Block> BLOCKS = REGISTRAR_MANAGER.getRegistrar(Registries.BLOCK);
    public static final Registrar<Item> ITEMS = REGISTRAR_MANAGER.getRegistrar(Registries.ITEM);
    public static final Registrar<BlockEntityType<?>> BLOCK_ENTITIES = REGISTRAR_MANAGER.getRegistrar(Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<LaptopBlock, Block> LAPTOP_BLOCK = BLOCKS.register("laptop", () -> new LaptopBlock(Block.Properties.of()));
    public static final RegistrySupplier<BlockItem, Item> LAPTOP_ITEM = ITEMS.register("laptop", () -> new BlockItem(LAPTOP_BLOCK.get(), new BlockItem.Properties()));
    public static final RegistrySupplier<BlockEntityType<LaptopBlockEntity>, BlockEntityType<?>> LAPTOP_BLOCK_ENTITY = BLOCK_ENTITIES.register("laptop", () -> BlockEntityType.Builder.of(LaptopBlockEntity::new, LAPTOP_BLOCK.get()).build(null));
    private static final Cleaner CLEANER = Cleaner.create();
    private static MinecraftServer server;
    private static BitmapFont font;
    private static List<Disposable> disposables = new ArrayList<>();

    public static void init() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> UDevicesModClient::init);

        BLOCKS.load();
        ITEMS.load();
        BLOCK_ENTITIES.load();

        UDevicesNet.setup();

        EventSystem.MAIN.on(ServerStartedEvent.class, serverStartedEvent -> server = serverStartedEvent.getServer());
        EventSystem.MAIN.on(ServerStoppingEvent.class, serverStoppingEvent -> server = null);

        EventSystem.MAIN.on(ClientStoppedEvent.class, clientStoppedEvent -> {
            disposables.forEach(Disposable::dispose);
            disposables.clear();
        });

        EventSystem.MAIN.on(ClientStartedEvent.class, clientStartedEvent -> {
            font = new BitmapFont();
            CLEANER.register(font, () -> {
                font.dispose();
            });
        });

    }

    public static Path getDataPath() {
        return EnvExecutor.getInEnvSpecific(
                () -> () -> server.getWorldPath(UDEVICES),
                () -> () -> Path.of("udevices")
        );
    }

    public static void onGfxCallback(ServerPlayer player, BlockPos pos, int ptr, Arguments args) {
        MinecraftServer currentServer = server;
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

    public static MinecraftServer getServer() {
        return server;
    }

    public static BitmapFont getFont() {
        return font;
    }

    public static Texture texture(String resourcePath) {
        FileHandle internal = Gdx.files.internal("assets/" + MOD_ID + "/" + resourcePath);
        Texture texture = new Texture(internal);
        disposables.add(texture);

        return texture;
    }
}
