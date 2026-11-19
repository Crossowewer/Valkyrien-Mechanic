package com.crossower.valkyrien_mechanic.block.entity;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.custom.GasEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

public class GasEngineBlockEntity extends PortBlockEntity implements ConnectionSystem.RotationSource
{
    private static final double BASE_FORCE = 500;
    public static final int GEARS =  13;
    private static final float ENGINE_RPM = 240f;

    @Override
    public List<ConnectionSystem.Port> getPorts()
    {
        return List.of(ConnectionSystem.Port.both(0, ConnectionSystem.SignalType.MOTOR,
                Set.of(ConnectionSystem.SignalType.LOGIC,
                        ConnectionSystem.SignalType.CONTROL_SEAT),
                0.5f, 0.5f, 0.5f));
    }

    @Override
    public void serverTick()
    {
        super.serverTick();
        if (level == null || level.isClientSide) return;
        int gear = Math.min(getBlockState().getValue(GasEngineBlock.GEAR), GEARS);
        boolean active = !isSignalControlled(0) || Math.abs(input) > 0.5f;
        int target = active ? gear : 0;

        BlockState s = getBlockState();
        if(s.getValue(GasEngineBlock.POWER) != target) {
            level.setBlock(worldPosition, s.setValue(GasEngineBlock.POWER, target), Block.UPDATE_ALL);
        }
    }

    @Override
    public float getTorque()
    {
        int level = getBlockState().getValue(GasEngineBlock.POWER);
        return level == 0 ? 0f : (float) (BASE_FORCE * Math.pow(1.5, level - 1));
    }
    @Override
    public float getMaxSpeed()
    {
        return getBlockState().getValue(GasEngineBlock.POWER) > 0 ? ENGINE_RPM : 0f;
    }
    @Override
    public int getDirection()
    {
        return getBlockState().getValue(GasEngineBlock.POWER) > 0 ? (input < -0.5f ? -1 : 1) : 0;
    }
    @Override
    public float getTargetAngle()
    {
        return Float.NaN;
    }

    private float input = 0f;
    @Override
    protected void onInputChanged(int portId, float value)
    {
        if (portId == 0) input = value;
    }

    @Override
    public float getOutput(int portId)
    {
        return getBlockState().getValue(GasEngineBlock.GEAR) > 0 ? 1f : 0f;
    }

    protected GasEngineBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.GAS_ENGINE_BLOCK_ENTITY.get(), pos, blockState);
    }
}
