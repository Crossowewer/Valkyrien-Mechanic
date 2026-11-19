package com.crossower.valkyrien_mechanic;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork
{
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ValkyrienMechanic.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);


    public static void register()
    {
        CHANNEL.registerMessage(0, LinkPacket.class, LinkPacket::encode, LinkPacket::decode, LinkPacket::handle);
        CHANNEL.registerMessage(1, PortUsePacket.class, PortUsePacket::encode, PortUsePacket::decode, PortUsePacket::handle);
        CHANNEL.registerMessage(2, SeatInputPacket.class, SeatInputPacket::encode, SeatInputPacket::decode, SeatInputPacket::handle);
    }
}
