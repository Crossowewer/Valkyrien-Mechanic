package com.crossower.valkyrien_mechanic.block.custom;

import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.entity.GasEngineBlockEntity;
import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
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

public class GasEngineBlock extends Block implements EntityBlock
{
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    public static final DirectionProperty FACING =  BlockStateProperties.FACING;

    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final IntegerProperty GEAR = IntegerProperty.create("gear", 0, 13);
    public static final IntegerProperty POWER_VIS = IntegerProperty.create("power_vis", 0, 3);

    //Block shenanigans ===================================================
    public GasEngineBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0).setValue(GEAR, 0).setValue(POWER_VIS, 0));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWER, GEAR, POWER_VIS);
    }
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, @NotNull BlockState blockState)
    {
        return ModBlockEntities.GAS_ENGINE_BLOCK_ENTITY.get().create(blockPos, blockState);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return PortBlockEntity.serverTicker(level);
    }
    //======================================================================

    //Engine power configuration (will change to a dedicated ui later, hopefully) ====================
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide)
        {
            int next;
            if (!player.isCrouching()) next = state.getValue(GEAR) >= GasEngineBlockEntity.GEARS ? 0 : state.getValue(GEAR) + 1;
            else next = state.getValue(GEAR) <= 0 ? GasEngineBlockEntity.GEARS : state.getValue(GEAR) - 1;
            int vis = next == 0 ? 0 : (next - 1) / 5 + 1; // 1-5 -> 1, 6-10 -> 2, 11-15 -> 3
            level.setBlock(pos, state.setValue(GEAR, next).setValue(POWER_VIS, vis), Block.UPDATE_ALL);
            player.displayClientMessage(Component.literal(next == 0 ? "Engine gear : OFF" : "Engine gear : " + next + "/" + GasEngineBlockEntity.GEARS), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    //===================================================================================================

    //Block shape and direction props ===================================================================
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
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override
    public RenderShape getRenderShape(BlockState blockState)
    {
        return RenderShape.MODEL;
    }
}
