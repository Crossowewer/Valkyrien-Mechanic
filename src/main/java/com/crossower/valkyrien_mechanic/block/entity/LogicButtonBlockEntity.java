package com.crossower.valkyrien_mechanic.block.entity;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.custom.LogicButtonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class LogicButtonBlockEntity extends PortBlockEntity
{
    @Override
    public List<ConnectionSystem.Port> getPorts() { return PORTS; }

    private static final List<ConnectionSystem.Port> PORTS = List.of(new ConnectionSystem.Port(0, ConnectionSystem.PortType.OUTPUT, ConnectionSystem.SignalType.LOGIC, 0.5f, 0.1f, 0.5f));
    private long unpressTimeRemain = 0;


    public LogicButtonBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.LOGIC_BUTTON_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override public float getOutput(int portId) {
        return getBlockState().getValue(LogicButtonBlock.POWERED) ? 1f : 0f;
    }


    public void setUnpressTime(long time){
        this.unpressTimeRemain = time;
    }

    public long getUnpressTime()
    {
        return this.unpressTimeRemain;
    }
}
