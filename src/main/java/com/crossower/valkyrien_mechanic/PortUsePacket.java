package com.crossower.valkyrien_mechanic;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PortUsePacket(ConnectionSystem.PortRef ref) {
    public static void encode(PortUsePacket p, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(p.ref.pos()); buf.writeVarInt(p.ref.id());
    }

    public static PortUsePacket decode(FriendlyByteBuf buf)
    {
        return new PortUsePacket(new ConnectionSystem.PortRef(buf.readBlockPos(), buf.readVarInt()));
    }

    public static void handle(PortUsePacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ConnectionSystem.PortLinks.handlePortUse(player, p.ref);
        });
        ctx.get().setPacketHandled(true);
    }
}