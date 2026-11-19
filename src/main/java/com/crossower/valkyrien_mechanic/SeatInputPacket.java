package com.crossower.valkyrien_mechanic;

import com.crossower.valkyrien_mechanic.block.entity.SeatBlockEntity;
import com.crossower.valkyrien_mechanic.block.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SeatInputPacket(BlockPos pos, float throttle, float steer)
{
    public static void encode(SeatInputPacket p, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(p.pos);
        buf.writeFloat(p.throttle);
        buf.writeFloat(p.steer);
    }

    public static SeatInputPacket decode(FriendlyByteBuf buf)
    {
        return new SeatInputPacket(buf.readBlockPos(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(SeatInputPacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Entity vehicle = player.getVehicle();
            if (!(vehicle instanceof SeatEntity) || !vehicle.blockPosition().equals(p.pos)) return;
            if (player.level().getBlockEntity(p.pos) instanceof SeatBlockEntity seat) seat.setInput(p.throttle, p.steer);
        });
        ctx.get().setPacketHandled(true);
    }
}
