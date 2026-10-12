package com.crossower.valkyrien_mechanic.client;

import com.crossower.valkyrien_mechanic.LinkPacket;
import com.crossower.valkyrien_mechanic.ModNetwork;
import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.fml.common.Mod;

import java.util.logging.Level;

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
        if (hovered == null) return;
        ConnectionSystem.PortRef pressed = hovered.ref();

        ConnectionSystem.PortRef source = connectedSource(pressed);
        if (source != null) {
            ModNetwork.CHANNEL.sendToServer(new LinkPacket(pressed, null));
            dragFrom = source;
        }
        else dragFrom = pressed;
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


    private static ConnectionSystem.PortRef startOfDrag(ConnectionSystem.PortRef pressed) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pressed.pos()) instanceof ConnectionSystem.ConnectableBlockEntity be) {
            ConnectionSystem.PortLinks.Link link = be.getLinks().all().get(pressed.id());
            if (link != null) {
                return new ConnectionSystem.PortRef(pressed.pos().offset(link.offset()), link.sourcePort());
            }
        }
        return pressed;
    }


    private static ConnectionSystem.PortRef connectedSource(ConnectionSystem.PortRef pressed)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pressed.pos()) instanceof ConnectionSystem.ConnectableBlockEntity be) {
            ConnectionSystem.PortLinks.Link link = be.getLinks().all().get(pressed.id());
            if (link != null) {
                return new ConnectionSystem.PortRef(pressed.pos().offset(link.offset()), link.sourcePort());
            }
        }
        return null;
    }

}
