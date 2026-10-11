package com.crossower.valkyrien_mechanic.client;

import com.crossower.valkyrien_mechanic.LinkPacket;
import com.crossower.valkyrien_mechanic.ModNetwork;
import com.crossower.valkyrien_mechanic.block.ConnectionSystem;

public final class ConnectorClient
{
    private static ConnectionSystem.PortRef dragFrom = null;

    public static ConnectionSystem.PortRef getDragFrom()
    {
        return dragFrom;
    }

    public static void cancel()
    {
        dragFrom = null;
    }

    public static void onLeftPress(ConnectionRenderer.ScreenPort hovered) {
        if (hovered != null) dragFrom = hovered.ref();
    }

    public static void onLeftRelease(ConnectionRenderer.ScreenPort target)
    {
        if (dragFrom == null) return;
        ConnectionSystem.PortRef from = dragFrom;
        dragFrom = null;
        if (target != null && !target.ref().equals(from)) {
            ModNetwork.CHANNEL.sendToServer(new LinkPacket(from, target.ref()));
        }
    }

    public static void onRightPress(ConnectionRenderer.ScreenPort hovered)
    {
        if (dragFrom != null) {dragFrom = null; return;}
        if (hovered != null) ModNetwork.CHANNEL.sendToServer(new LinkPacket(hovered.ref(), null));
    }


}
