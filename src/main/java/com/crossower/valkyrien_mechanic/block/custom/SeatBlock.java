package com.crossower.valkyrien_mechanic.block.custom;

import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities;
import com.crossower.valkyrien_mechanic.block.entity.SeatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SeatBlock extends Block implements EntityBlock
{
    public SeatBlock(Properties properties)
    {
        super(properties);
    }
    protected final static VoxelShape shape = box(0, 0, 0, 16, 8, 16);

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return ModBlockEntities.SEAT_BLOCK_ENTITY.get().create(pos, state);
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public <T extends BlockEntity>BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return PortBlockEntity.serverTicker(level);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,  BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer sp
        && level.getBlockEntity(pos) instanceof SeatBlockEntity seat) seat.mount(sp);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SeatBlockEntity seat)
            seat.dismountAll();
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
