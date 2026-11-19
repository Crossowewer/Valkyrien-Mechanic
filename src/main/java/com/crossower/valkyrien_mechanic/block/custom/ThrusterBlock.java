package com.crossower.valkyrien_mechanic.block.custom;

import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities;
import com.crossower.valkyrien_mechanic.block.entity.ThrusterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.crossower.valkyrien_mechanic.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThrusterBlock extends Block implements EntityBlock
{
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final IntegerProperty GEAR = IntegerProperty.create("gear", 0, 13);
    public static final IntegerProperty POWER_VIS = IntegerProperty.create("power_vis", 0, 3);
    public static final int MAX_GEAR = 13;

    //Block shenanigans ===========================================
    public ThrusterBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GEAR, 0).setValue(POWER, 0).setValue(POWER_VIS, 0));
    }
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, @NotNull BlockState blockState)
    {
        return ModBlockEntities.THRUSTER_BLOCK_ENTITY.get().create(blockPos, blockState);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return PortBlockEntity.serverTicker(level);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, GEAR, POWER, POWER_VIS);
    }

    //================================================================

    //Custom behaviour ===============================================
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!level.isClientSide)
        {
            int gear = state.getValue(GEAR);
            int next = !player.isCrouching()
                    ? (gear >= MAX_GEAR ? 0 : gear + 1)
                    : (gear <= 0 ? MAX_GEAR : gear - 1);
            level.setBlock(pos, state.setValue(GEAR, next), Block.UPDATE_ALL);
            player.displayClientMessage(Component.literal(next == 0 ? "Thruster power : OFF" : "Thruster power : " + next + "/" + MAX_GEAR), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static int visOf(int power) {
        return power == 0 ? 0 : (power - 1) / 5 + 1;
    }
    //================================================================

    //Block direction and state shenanigans =====================================
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getNearestLookingDirection();
        Player player = context.getPlayer();

        if (player != null && player.isSecondaryUseActive()) {
            facing = facing.getOpposite();
        }

        BlockPos dummyPos = pos.relative(facing.getOpposite());
        if (!level.getBlockState(dummyPos).canBeReplaced(context)) {
            return null;
        }

        return defaultBlockState().setValue(FACING, facing);
    }
    @Override
    public void onRemove(@NotNull BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                Direction facing = state.getValue(FACING);
                BlockPos dummyPos = pos.relative(facing.getOpposite());

                if (level.getBlockState(dummyPos).is(ModBlocks.THRUSTER_DUMMY.get())) {
                    level.removeBlock(dummyPos, false);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide())
        {
            Direction direction = state.getValue(FACING);
            BlockPos dummyPos = pos.relative(direction.getOpposite());

            level.setBlock(dummyPos, ModBlocks.THRUSTER_DUMMY.get().defaultBlockState().setValue(FACING, direction), Block.UPDATE_ALL);
        }
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState)
    {
        return RenderShape.MODEL;
    }
}
