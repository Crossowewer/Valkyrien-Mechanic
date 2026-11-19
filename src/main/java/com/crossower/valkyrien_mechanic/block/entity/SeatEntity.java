package com.crossower.valkyrien_mechanic.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class SeatEntity extends Entity
{

    public SeatEntity(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {

    }
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isInvisible() {  return false; }
    @Override public boolean shouldBeSaved() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick()
    {
        super.tick();
        for (Entity passenger : getPassengers()) lockBody(passenger);
    }

    public static void lockBody(Entity passenger)
    {
        if (!(passenger instanceof LivingEntity living)) return;
        float head = living.getYRot();
        float body = living.yBodyRot;
        float diff = Mth.wrapDegrees(head - body);
        if (Math.abs(diff) > BODY_DEAD_ZONE)
            body = head - Math.copySign(BODY_DEAD_ZONE, diff);
        living.yBodyRotO = living.yBodyRot;
        living.yBodyRot = body;
    }
    private static final float BODY_DEAD_ZONE = 60f;
}
