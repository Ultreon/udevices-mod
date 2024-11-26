package dev.ultreon.devicesnext.block;

import dev.ultreon.devicesnext.block.entity.LaptopBlockEntity;
import dev.ultreon.devicesnext.client.DeviceManager;
import dev.ultreon.devicesnext.device.Laptop;
import dev.ultreon.devicesnext.device.McDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LaptopBlock extends Block implements EntityBlock {
    public static final Property<Boolean> OPEN = BlockStateProperties.OPEN;

    public LaptopBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.OPEN, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new LaptopBlockEntity(blockPos, blockState);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull Player player, @NotNull InteractionHand interactionHand, @NotNull BlockHitResult blockHitResult) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof LaptopBlockEntity laptopBlockEntity) {
            McDevice device = laptopBlockEntity.getDevice();
            if (device instanceof Laptop laptop) {
                if (player.isShiftKeyDown()) {
                    level.setBlock(blockPos, blockState.setValue(BlockStateProperties.OPEN, !blockState.getValue(BlockStateProperties.OPEN)), 3);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
