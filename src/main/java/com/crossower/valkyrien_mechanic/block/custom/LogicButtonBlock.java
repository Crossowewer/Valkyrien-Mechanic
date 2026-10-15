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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;


import javax.annotation.Nullable;

public class LogicButtonBlock extends Block implements EntityBlock
{
    public static VoxelShape SHAPE = Block.box(4.0D, 1.0D, 4.0D, 12.0D, 2.0D, 12.0D);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public LogicButtonBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ePos)
    {
        return SHAPE;
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
    }


    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(!level.isClientSide) {
            this.press(state, level, pos, 5);
            level.playSound(null, pos, ModSounds.SWITCH_CLICK.get(), SoundSource.BLOCKS, 0.3F, 1.0F);
            if (!state.getValue(POWERED)) {
                level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
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
