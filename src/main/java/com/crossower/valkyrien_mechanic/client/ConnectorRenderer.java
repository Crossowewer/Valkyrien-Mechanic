package com.crossower.valkyrien_mechanic.client;

import com.crossower.valkyrien_mechanic.item.ModItems;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ConnectorRenderer
{
    private record ScreenPort(float x, float y, float radius, float depth, int argb)
    {
    }

    private static final float WORLD_RADIUS = 0.12f;
    private static final float MIN_RADIUS = 1.5f;
    private static final float MAX_RADIUS = 20.0f;

    private static final List<ScreenPort> PORTS = new ArrayList<>();

    public static boolean isToolActive()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isHolding(ModItems.CONNECTOR_TOOL.get()))
        {
            return true;
        } else return false;
    }

    public static void clear()
    {
        PORTS.clear();
    }

    public static void submit(PoseStack poseStack, float lx, float ly, float lz, int argb)
    {
        Vector4f p = new Vector4f(lx, ly, lz, 1.0f);
        p.mul(poseStack.last().pose());
        p.mul(RenderSystem.getProjectionMatrix());
        if (p.w <= 0f) return;

        Window window = Minecraft.getInstance().getWindow();
        float sx = (p.x / p.w * 0.5f + 0.5f) * window.getGuiScaledWidth();
        float sy = (0.5f - p.y / p.w * 0.5f) * window.getGuiScaledHeight();

        float radius = WORLD_RADIUS * RenderSystem.getProjectionMatrix().m11() / p.w * window.getGuiScaledWidth() * 0.5f;
        radius = Mth.clamp(radius, MIN_RADIUS, MAX_RADIUS);

        PORTS.add(new ScreenPort(sx, sy, radius, p.w, argb));
    }

    public static void renderOverlay(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h)
    {
        for (ScreenPort port : PORTS)
        {
            //fillCircle(g, port.x(), port.y(), 6f, 0xFFFFFFFF);
            fillCircle(g, port.x(), port.y(), port.radius(), port.argb);
        }
    }

    private static void fillCircle(GuiGraphics gui, float cx, float cy, float radius, int argb)
    {
        float a = (argb >>> 24 & 255) / 255f, r = (argb >>> 16 & 255) / 255f,
                gr = (argb >>> 8 & 255) / 255f, b = (argb & 255) / 255f;
        Matrix4f m = gui.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(m, cx, cy, 0).color(r, gr, b, a).endVertex();
        int segments = 24;
        for (int i = 0; i <= segments; i++)
        {
            double ang = 2 * Math.PI * i / segments;
            buf.vertex(m, cx + (float) Math.cos(ang) * radius, cy + (float) Math.sin(ang) * radius, 0).color(r, gr, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();
    }
}
