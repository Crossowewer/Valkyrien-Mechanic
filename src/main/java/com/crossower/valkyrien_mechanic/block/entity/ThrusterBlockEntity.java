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
import java.util.Set;

import static com.crossower.valkyrien_mechanic.block.custom.ThrusterBlock.FACING;
import static com.crossower.valkyrien_mechanic.block.custom.ThrusterBlock.POWER;
import static net.minecraft.world.level.block.Block.UPDATE_ALL;

public class ThrusterBlockEntity extends PortBlockEntity implements BlockEntityPhysicsListener
{

    private static final List<ConnectionSystem.Port> PORTS = List.of(ConnectionSystem.Port.input(
            0, ConnectionSystem.SignalType.THRUSTER,
            Set.of(ConnectionSystem.SignalType.LOGIC,
                    ConnectionSystem.SignalType.CONTROL_SEAT),
            0.5f,0.5f, 0.5f));

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
        double thrustForce = 500 * Math.pow(1.5, getBlockState().getValue(POWER) - 1);
        Vector3d force = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(FACING).getNormal());
                force.mul(thrustForce); // multiply direction by your strength
        physShip.applyModelForce(force, VectorConversionsMCKt.toJOML(worldPosition.getCenter()));
    }
    @Override
    public void serverTick()
    {
        super.serverTick();
        if (level == null || level.isClientSide) return;

        BlockState s = getBlockState();
        int gear = Math.min(s.getValue(ThrusterBlock.GEAR), ThrusterBlock.MAX_GEAR);
        boolean active = !isSignalControlled(0) || input > 0.5f;
        int target = active ? gear : 0;
        int vis = ThrusterBlock.visOf(target);

        if (s.getValue(ThrusterBlock.POWER) != target || s.getValue(ThrusterBlock.POWER_VIS) != vis)
            level.setBlock(worldPosition, s.setValue(ThrusterBlock.POWER, target).setValue(ThrusterBlock.POWER_VIS, vis), Block.UPDATE_ALL);
    }

    @Override
    public void setDimension(@NotNull String s)
    {

    }

    private float input = 0f;
    @Override protected void onInputChanged(int portId, float value) {
        if (portId == 0) input = value;
    }
 }
