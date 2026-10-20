package com.crossower.valkyrien_mechanic.block.custom;

import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BearingBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock
{

    public BearingBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return ModBlockEntities.BEARING_BLOCK_ENTITY.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(new Property[]{FACE, FACING});
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return PortBlockEntity.serverTicker(level);
    }

    public static Direction getHeadDirection(BlockState state) {
        return getConnectedDirection(state);
    }

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

    protected static final VoxelShape NORTH_AABB;
    protected static final VoxelShape SOUTH_AABB;
    protected static final VoxelShape WEST_AABB;
    protected static final VoxelShape EAST_AABB;
    protected static final VoxelShape UP_AABB_Z;
    protected static final VoxelShape UP_AABB_X;
    protected static final VoxelShape DOWN_AABB_Z;
    protected static final VoxelShape DOWN_AABB_X;

    static {
        NORTH_AABB = Block.box(1.0D, 1.0D, 15.0D, 15.0D, 15.0D, 16.0D);
        SOUTH_AABB = Block.box(1.0D, 1.0D, 0.0D, 15.0D, 15.0D, 1.0D);
        WEST_AABB  = Block.box(15.0D, 1.0D, 1.0D, 16.0D, 15.0D, 15.0D);
        EAST_AABB  = Block.box(0.0D, 1.0D, 1.0D, 1.0D, 15.0D, 15.0D);

        UP_AABB_Z  = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
        UP_AABB_X  = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);

        DOWN_AABB_Z = Block.box(1.0D, 15.0D, 1.0D, 15.0D, 16.0D, 15.0D);
        DOWN_AABB_X = Block.box(1.0D, 15.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    }
}
