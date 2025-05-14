package dev.ultreon.devicesnext.block;

import dev.ultreon.devicesnext.block.entity.LaptopBlockEntity;
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
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);

        builder.add(OPEN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new LaptopBlockEntity(blockPos, blockState);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState blockState, Level level, @NotNull BlockPos blockPos, @NotNull Player player, @NotNull BlockHitResult pHitResult) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof LaptopBlockEntity laptopBlockEntity) {
            McDevice device = laptopBlockEntity.getDevice();
            if (device instanceof Laptop laptop) {
                if (player.isShiftKeyDown()) {
                    toggleOpen(blockState, level, blockPos, player, device);
                } else if (level.isClientSide && blockState.getValue(BlockStateProperties.OPEN)) {
                    connectDisplay(player, device);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private void connectDisplay(@NotNull Player player, McDevice device) {
        device.connectDisplay(player);
    }

    private static void toggleOpen(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull Player player, McDevice device) {
        boolean open = !blockState.getValue(BlockStateProperties.OPEN);
        level.setBlock(blockPos, blockState.setValue(BlockStateProperties.OPEN, open), 3);
        if (open) {
            device.open(player);
        } else {
            device.close(player);
        }
    }
}
