package com.crossower.valkyrien_mechanic;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public record LinkPacket(ConnectionSystem.PortRef a, @Nullable ConnectionSystem.PortRef b)
{
    public static void encode(LinkPacket p, FriendlyByteBuf buf) {
        write(buf, p.a);
        buf.writeBoolean(p.b != null);
        if (p.b != null) write(buf, p.b());
    }

    public static LinkPacket decode(FriendlyByteBuf buf)
    {
        ConnectionSystem.PortRef a = read(buf);
        return new LinkPacket(a, buf.readBoolean() ? read(buf) : null);
    }

    public static void handle(LinkPacket p, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ConnectionSystem.PortLinks.handleLink(player, p.a, p.b);
        });
    }

    private static void write(FriendlyByteBuf buf, ConnectionSystem.PortRef r) {buf.writeBlockPos(r.pos()); buf.writeVarInt(r.id());}
    private static ConnectionSystem.PortRef read(FriendlyByteBuf buf) {return new ConnectionSystem.PortRef(buf.readBlockPos(), buf.readVarInt());}
}
