package com.crossower.valkyrien_mechanic.block.entity;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.custom.LogicButtonBlock;
import com.crossower.valkyrien_mechanic.block.custom.SwitchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import java.util.List;

public class SwitchBlockEntity extends PortBlockEntity
{
    @Override
    public List<ConnectionSystem.Port> getPorts() {
        BlockState state = this.getBlockState();
        AttachFace face = state.getValue(LogicButtonBlock.FACE);
        Direction dir = state.getValue(LogicButtonBlock.FACING);

        return List.of(new ConnectionSystem.Port(
                0,
                ConnectionSystem.PortType.OUTPUT,
                ConnectionSystem.SignalType.LOGIC,
                getX(face, dir),
                getY(face, dir),
                getZ(face, dir)
        ));
    }

    protected final static float floor_x = 0.5f, floor_y = 0.1f, floor_z = 0.5f;
    protected final static float ceiling_x = 0.5f, ceiling_y = 0.9f, ceiling_z = 0.5f;
    protected final static float north_x = 0.5f, north_y = 0.5f, north_z = 0.9f;
    protected final static float south_x = 0.5f, south_y = 0.5f, south_z = 0.1f;
    protected final static float east_x = 0.1f, east_y = 0.5f, east_z = 0.5f;
    protected final static float west_x = 0.9f, west_y = 0.5f, west_z = 0.5f;

    private static float getX(AttachFace face, Direction dir){
        if (face == AttachFace.CEILING) return ceiling_x;
        else if (face == AttachFace.FLOOR) return floor_x;
        else if (face == AttachFace.WALL){
            switch (dir){
                case WEST: return west_x;
                case EAST: return east_x;
                case SOUTH: return south_x;
                case NORTH: return north_x;
                default: return 0.1f;
            }
        }
        return 0.1f;
    }
    private static float getY(AttachFace face, Direction dir){
        if (face == AttachFace.CEILING) return ceiling_y;
        else if (face == AttachFace.FLOOR) return floor_y;
        else if (face == AttachFace.WALL){
            switch (dir){
                case WEST: return west_y;
                case EAST: return east_y;
                case SOUTH: return south_y;
                case NORTH: return north_y;
                default: return 0.1f;
            }
        }
        return 0.1f;
    }
    private static float getZ(AttachFace face, Direction dir){
        if (face == AttachFace.CEILING) return ceiling_z;
        else if (face == AttachFace.FLOOR) return floor_z;
        else if (face == AttachFace.WALL){
            switch (dir){
                case WEST: return west_z;
                case EAST: return east_z;
                case SOUTH: return south_z;
                case NORTH: return north_z;
                default: return 0.1f;
            }
        }
        return 0.1f;
    }

    public SwitchBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.SWITCH_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override public float getOutput(int portId) {
        return getBlockState().getValue(SwitchBlock.POWERED) ? 1f : 0f;
    }


}
