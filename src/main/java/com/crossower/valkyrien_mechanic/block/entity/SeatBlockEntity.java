package com.crossower.valkyrien_mechanic.block.entity;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SeatBlockEntity extends PortBlockEntity implements ConnectionSystem.RotationSource
{
    public static final String SEAT_TAG = "valkyrien_mechanic_seat";
    private static final float STEER_DEGREES = 35f;
    private static final float STEER_TORQUE = 10f;
    private static final double SEAT_Y = 0.25;

    private static final List<ConnectionSystem.Port> PORTS = List.of(
            //ConnectionSystem.Port.input(0, ConnectionSystem.SignalType.LOGIC, Set.of(ConnectionSystem.SignalType.LOGIC), 0.5f, 0.2f, 0.5f),
            ConnectionSystem.Port.output(1, ConnectionSystem.SignalType.CONTROL_SEAT, 0.5f, 0.2f, 0.5f));

    private volatile float throttle = 0f;
    private volatile float steer = 0f;
    private UUID seatEntity = null;

    public SeatBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.SEAT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public List<ConnectionSystem.Port> getPorts()
    {
        return PORTS;
    }

    @Override
    public float getOutput(int portId) { return portId == 1 ? throttle : 0f; }

    @Override public float getTorque() { return STEER_TORQUE; }
    @Override public float getMaxSpeed() { return 0f; }
    @Override public int getDirection() { return 0; }
    @Override public float getTargetAngle() { return steer * STEER_DEGREES; }

    public void setInput(float t, float s)
    {
        throttle = Mth.clamp(t, -1f, 1f);
        steer = Mth.clamp(s, -1f, 1f);
    }

    public void mount(ServerPlayer player)
    {
        if (!(level instanceof ServerLevel sl)) return;

        if (seatEntity != null)
        {
            Entity old = sl.getEntity(seatEntity);
            if (old != null && old.isVehicle()) return;
            if (old != null) old.discard();
            seatEntity = null;
            setInput(0f, 0f);
        }
        if (player.getVehicle() != null) return;

        SeatEntity stand = ModEntities.SEAT.get().create(sl);
        if (stand == null) return;
        stand.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + SEAT_Y, worldPosition.getZ() + 0.5);
        sl.addFreshEntity(stand);
        if (!player.startRiding(stand, true))
        {
            stand.discard();
            return;
        }
        seatEntity = stand.getUUID();
        setChanged();
    }

    public void dismountAll()
    {
        if (level instanceof ServerLevel sl && seatEntity != null)
        {
            Entity stand = sl.getEntity(seatEntity);
            if (stand != null) stand.discard();
            seatEntity = null;
        }
        setInput(0f, 0f);
    }

    @Override
    public void serverTick()
    {
        super.serverTick();
        if (!(level instanceof ServerLevel sl) || seatEntity == null) return;
        Entity stand = sl.getEntity(seatEntity);
        if (stand == null || !stand.isVehicle())
        {
            if (stand != null) stand.discard();
            seatEntity = null;
            setInput(0f, 0f);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        if (seatEntity != null) tag.putUUID("seat", seatEntity);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        seatEntity = tag.hasUUID("seat") ? tag.getUUID("seat") : null;
    }
}
