package com.crossower.valkyrien_mechanic.block.entity;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.custom.ThrusterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

import static com.crossower.valkyrien_mechanic.block.custom.ThrusterBlock.FACING;
import static com.crossower.valkyrien_mechanic.block.custom.ThrusterBlock.POWER;
import static net.minecraft.world.level.block.Block.UPDATE_ALL;

public class ThrusterBlockEntity extends PortBlockEntity implements BlockEntityPhysicsListener
{

    private static final List<ConnectionSystem.Port> PORTS = List.of(new ConnectionSystem.Port(0, ConnectionSystem.PortType.INPUT, ConnectionSystem.SignalType.LOGIC, 0.5f,0.5f, 0.5f));

    @Override
    public List<ConnectionSystem.Port> getPorts() { return PORTS; }

    public ThrusterBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.THRUSTER_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public @NotNull String getDimension()
    {
        return "minecraft:dimension:minecraft:overworld";
    }

    @Override
    public void physTick(@Nullable PhysShip physShip, @NotNull PhysLevel physLevel) {
        if (physShip == null) return;
        double thrustForce = 1000 * Math.pow(1.5, getBlockState().getValue(POWER) - 1);
        Vector3d force = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(FACING).getNormal());
                force.mul(thrustForce); // multiply direction by your strength
        physShip.applyModelForce(force, VectorConversionsMCKt.toJOML(worldPosition.getCenter()));
    }
    @Override
    public void setDimension(@NotNull String s)
    {

    }

    private int signalPower = 0;

    @Override protected void onInputChanged(int portId, float value) {
        if (portId != 0) return;
        signalPower = value > 0.5f ? 15 : 0;
        updatePower();
    }

    public void updatePower()
    {
        if (level == null || level.isClientSide) return;
        int target = Math.max(level.getBestNeighborSignal(worldPosition), signalPower);
        BlockState state = getBlockState();
        if (state.getValue(ThrusterBlock.POWER) != target) {
            level.setBlock(worldPosition, state.setValue(ThrusterBlock.POWER, target), Block.UPDATE_ALL);
        }
    }
 }
