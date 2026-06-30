package com.java.beipuo.ae2virus.block;

import com.java.beipuo.ae2virus.machine.VirusMachineBlockEntity;
import com.java.beipuo.ae2virus.machine.VirusMachineKind;
import com.java.beipuo.ae2virus.registry.AVBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class VirusMachineBlock extends Block implements EntityBlock {
    private final VirusMachineKind kind;

    public VirusMachineBlock(VirusMachineKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public VirusMachineKind kind() {
        return this.kind;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof VirusMachineBlockEntity machine) {
            if (!level.isClientSide()) {
                player.openMenu(machine, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VirusMachineBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof VirusMachineBlockEntity machine) {
            for (var stack : machine.drops()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == AVBlockEntities.VIRUS_MACHINE.get()
                ? (tickLevel, tickPos, tickState, blockEntity) ->
                        VirusMachineBlockEntity.tick(tickLevel, tickPos, tickState, (VirusMachineBlockEntity) blockEntity)
                : null;
    }
}
