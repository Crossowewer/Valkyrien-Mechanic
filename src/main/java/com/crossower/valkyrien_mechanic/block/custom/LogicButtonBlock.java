package com.crossower.valkyrien_mechanic.block.custom;

import com.crossower.valkyrien_mechanic.block.entity.LogicButtonBlockEntity;
import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities;
import com.crossower.valkyrien_mechanic.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;


import javax.annotation.Nullable;

public class LogicButtonBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock
{
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;


    public LogicButtonBlock(Properties properties)
    {
        super(properties);
        registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH).setValue(POWERED, false))).setValue(FACE, AttachFace.WALL));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, @NotNull BlockState state)
    {
        return ModBlockEntities.LOGIC_BUTTON_BLOCK_ENTITY.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(POWERED);
        builder.add(new Property[]{FACE, FACING});
    }


    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(!level.isClientSide) {
            this.press(state, level, pos, 5);

            if (!state.getValue(POWERED)) {
                level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
                level.playSound(null, pos, ModSounds.SWITCH_CLICK.get(), SoundSource.BLOCKS, 0.3F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void press(BlockState state, Level level, BlockPos pos, int ticks) {
        long targeetPressTime = level.getGameTime() + ticks;
        if (level.getBlockEntity(pos) instanceof LogicButtonBlockEntity be) {
            be.setUnpressTime(targeetPressTime);
        }

        if (!state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, true), 3);
            level.updateNeighborsAt(pos, this);
        }
        level.scheduleTick(pos, this, ticks);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED))
        {
            if (level.getBlockEntity(pos) instanceof LogicButtonBlockEntity be) {
                if (level.getGameTime() >= be.getUnpressTime()) {
                    level.setBlock(pos, state.setValue(POWERED, false), 3);
                    this.updateNeighbours(level, pos);
                    level.playSound(null, pos, ModSounds.SWITCH_CLICK.get(), SoundSource.BLOCKS, 0.3F, 0.7F);
                    level.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, pos);
                }
                else {
                    long remainingTime = be.getUnpressTime() - level.getGameTime();
                    level.scheduleTick(pos, this, (int) Math.max(1, remainingTime));
                }
            }
        }
    }

    protected static final VoxelShape NORTH_AABB;
    protected static final VoxelShape SOUTH_AABB;
    protected static final VoxelShape WEST_AABB;
    protected static final VoxelShape EAST_AABB;
    protected static final VoxelShape UP_AABB_Z;
    protected static final VoxelShape UP_AABB_X;
    protected static final VoxelShape DOWN_AABB_Z;
    protected static final VoxelShape DOWN_AABB_X;

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch ((AttachFace)state.getValue(FACE)) {
            case FLOOR:
                switch (((Direction)state.getValue(FACING)).getAxis()) {
                    case X:
                        return UP_AABB_X;
                    case Z:
                    default:
                        return UP_AABB_Z;
                }
            case WALL:
                switch ((Direction)state.getValue(FACING)) {
                    case EAST:
                        return EAST_AABB;
                    case WEST:
                        return WEST_AABB;
                    case SOUTH:
                        return SOUTH_AABB;
                    case NORTH:
                    default:
                        return NORTH_AABB;
                }
            case CEILING:
            default:
                switch (((Direction)state.getValue(FACING)).getAxis()) {
                    case X:
                        return DOWN_AABB_X;
                    case Z:
                    default:
                        return DOWN_AABB_Z;
                }
        }
    }

    static {
        NORTH_AABB = Block.box(4.0D, 4.0D, 14.0D, 12.0D, 12.0D, 16.0D);
        SOUTH_AABB = Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 2.0D);
        WEST_AABB  = Block.box(14.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D);
        EAST_AABB  = Block.box(0.0D, 4.0D, 4.0D, 2.0D, 12.0D, 12.0D);

        UP_AABB_Z  = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 2.0D, 12.0D);
        UP_AABB_X  = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 2.0D, 12.0D);

        DOWN_AABB_Z = Block.box(4.0D, 14.0D, 4.0D, 12.0D, 16.0D, 12.0D);
        DOWN_AABB_X = Block.box(4.0D, 14.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    }

    private void updateNeighbours(Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(POWERED) ? 15 : 0;
    }
}
