package com.crossower.valkyrien_mechanic.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import org.slf4j.Logger;

import static com.crossower.valkyrien_mechanic.block.custom.ThrusterBlock.FACING;
import static com.crossower.valkyrien_mechanic.block.custom.ThrusterBlock.POWER;

public class ThrusterBlockEntity extends BlockEntity implements BlockEntityPhysicsListener
{

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
        double thrustForce = 1000 * getBlockState().getValue(POWER);
        Vector3d force = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(FACING).getNormal());
                force.mul(thrustForce); // multiply direction by your strength
        physShip.applyModelForce(force, VectorConversionsMCKt.toJOML(worldPosition.getCenter()));
        Vec3i jdsfsd = getBlockState().getValue(FACING).getNormal();
    }
    @Override
    public void setDimension(@NotNull String s)
    {

    }
}
