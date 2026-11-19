package com.crossower.valkyrien_mechanic.client;

import com.crossower.valkyrien_mechanic.ModNetwork;
import com.crossower.valkyrien_mechanic.SeatInputPacket;
import com.crossower.valkyrien_mechanic.block.entity.SeatBlockEntity;
import com.crossower.valkyrien_mechanic.block.entity.SeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public final class SeatClient
{
    private static BlockPos lastSeat = null;
    private static float lastThrottle = 0f, lastSteer = 0f;

    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Entity vehicle = player == null ? null : player.getVehicle();
        boolean onSeat = vehicle instanceof SeatEntity;

        if (!onSeat)
        {
            if (lastSeat != null) send(lastSeat, 0f, 0f);
            lastSeat = null;
            lastThrottle = 0f;
            lastSteer = 0f;
            return;
        }

        float throttle = 0f, steer = 0f;
        if (mc.screen == null)
        {
            throttle = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
            steer = (mc.options.keyRight.isDown() ? 1f : 0f) - (mc.options.keyLeft.isDown() ? 1f : 0f);
        }

        BlockPos seat = vehicle.blockPosition();
        if (!seat.equals(lastSeat) || throttle != lastThrottle || steer != lastSteer)
        {
            send(seat, throttle, steer);
            lastSeat = seat;
            lastThrottle = throttle;
            lastSteer = steer;
        }
    }

    private static void send(BlockPos seat, float throttle, float steer)
    {
        ModNetwork.CHANNEL.sendToServer(new SeatInputPacket(seat, throttle, steer));
    }
}
