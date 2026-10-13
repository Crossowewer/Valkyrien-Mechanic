package com.crossower.valkyrien_mechanic.block.entity;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.custom.SwitchBlock;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SwitchBlockEntity extends PortBlockEntity
{
    @Override
    public List<ConnectionSystem.Port> getPorts()
    {
        return PORTS;
    }

    private static final List<ConnectionSystem.Port> PORTS = List.of(new ConnectionSystem.Port(0, ConnectionSystem.PortType.OUTPUT, ConnectionSystem.SignalType.LOGIC, 0.5f, 0.1f, 0.5f));


    public SwitchBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.SWITCH_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override public float getOutput(int portId) {
        return getBlockState().getValue(SwitchBlock.POWERED) ? 1f : 0f;
    }


}
