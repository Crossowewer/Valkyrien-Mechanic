package com.crossower.valkyrien_mechanic.client;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem;

import com.crossower.valkyrien_mechanic.block.ConnectionSystem.Port;
import com.crossower.valkyrien_mechanic.item.ModItems;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ConnectionRenderer
{

    private static final List<ScreenPort> PORTS = new ArrayList<ScreenPort>();

    public record ScreenPort(BlockPos pos, int id, float x, float y, float radius, float depth, int argb)
    {
        public ConnectionSystem.PortRef ref() {
            return new ConnectionSystem.PortRef(pos, id);
        }
    }

    private static final float WORLD_RADIUS = 0.12f;
    private static final float MIN_RADIUS = 1.5f;
    private static final float MAX_RADIUS = 20.0f;

    private static final float HIT_MIN_RADIUS = 2.5f;
    private static final float HOVER_REACH = 24f;

    private static ScreenPort hovered = null;
    private static float hoveredDistance = Float.MAX_VALUE;

    public static ScreenPort getHovered() {return hovered;}



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
        hovered = null;
        hoveredDistance = Float.MAX_VALUE;
        LINKS.clear();
    }

    public static void submit(PoseStack poseStack, BlockPos pos, int id, float lx, float ly, float lz, int argb)
    {
        Vector4f p = new Vector4f(lx, ly, lz, 1.0f);
        p.mul(poseStack.last().pose());

        ConnectionSystem.PortRef dragFrom = ConnectorClient.getDragFrom();
        if (dragFrom != null && dragFrom.equals(new ConnectionSystem.PortRef(pos, id))) {
            Vector4f wv = new Vector4f(p).mul(INV_VIEW);
            dragSourceWorld = new Vec3(cameraPos.x + wv.x, cameraPos.y + wv.y, cameraPos.z + wv.z);
            dragSourceColor = argb;
        }
        p.mul(RenderSystem.getProjectionMatrix());
        if (p.w <= 0f) return;

        Window window = Minecraft.getInstance().getWindow();
        float sx = (p.x / p.w * 0.5f + 0.5f) * window.getGuiScaledWidth();
        float sy = (0.5f - p.y / p.w * 0.5f) * window.getGuiScaledHeight();

        float radius = WORLD_RADIUS * RenderSystem.getProjectionMatrix().m11() / p.w * window.getGuiScaledWidth() * 0.5f;
        radius = Mth.clamp(radius, MIN_RADIUS, MAX_RADIUS);

        ScreenPort port = new ScreenPort(pos, id, sx, sy, radius, p.w, argb);
        PORTS.add(port);

        float dx = sx - window.getGuiScaledWidth() / 2f;
        float dy = sy - window.getGuiScaledHeight() / 2f;
        float distance = dx * dx + dy * dy;
        float hit = Math.max(radius, HIT_MIN_RADIUS) + 1.5f;

        if (p.w <= HOVER_REACH && distance <= hit * hit && distance < hoveredDistance)
        {
            hovered = port;
            hoveredDistance = distance;
        }
    }

    public static void renderOverlay(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h)
    {
        PORTS.sort(Comparator.comparingDouble(ScreenPort::depth).reversed());
        for (LinkLine line : LINKS) drawWorldLine(g, line.a(), line.b(), w, h, 2f, line.argb());
        ConnectionSystem.PortRef from = ConnectorClient.getDragFrom();
        if (from == null) dragSourceWorld = null;

        else if (dragSourceWorld != null)
        {
            float[] src = projectWorld(dragSourceWorld, w, h);
            float tx = w / 2f, ty = h / 2f;
            int color = dragSourceColor;
            if (hovered != null && !hovered.ref().equals(from)) {
                tx = hovered.x(); ty = hovered.y();
                color = lighten(color, 0.35f);

            }
            drawLine(g, src[0], src[1], tx, ty, 2f, color);
        }



        for (ScreenPort port : PORTS)
        {
            boolean hover = port == hovered;
            float r = port.radius() * (hover ? 1.3f : 1f);
            int color = hover ? lighten(port.argb(), 0.35f) : port.argb;

            fillCircle(g, port.x(), port.y(), r, color);
        }
    }

    public static int lighten(int argb, float amount) {
        int a = argb >>> 24;
        int r = argb >>> 16 & 255, g = argb >>> 8 & 255, b = argb & 255;
        r += (int) ((255 - r) * amount);
        g += (int) ((255 - g) * amount);
        b += (int) ((255 - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
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

    public static void drawLine(GuiGraphics gui, float x1, float y1, float x2, float y2, float thickness,int argb) {
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5f) return;
        float nx = -dy / length * thickness / 2f, ny = dx / length * thickness / 2f;

        float a = (argb >>> 24 & 255) / 255f, r = (argb >>> 16 & 255) / 255f,
                gr = (argb >>> 8 & 255) / 255f, b = (argb & 255) / 255f;
        Matrix4f m = gui.pose().last().pose();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(m, x1 + nx, y1 + ny, 0).color(r, gr, b, a).endVertex();
        buf.vertex(m, x2 + nx, y2 + ny, 0).color(r, gr, b, a).endVertex();
        buf.vertex(m, x2 - nx, y2 - ny, 0).color(r, gr, b, a).endVertex();
        buf.vertex(m, x1 - nx, y1 - ny, 0).color(r, gr, b, a).endVertex();
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();
    }

    private static @Nullable ScreenPort findPort(ConnectionSystem.PortRef ref) {
        for (ScreenPort port : PORTS) {if (port.ref().equals(ref)) return port;}
        return null;
    }


    private static Matrix4f VIEW = new Matrix4f();
    private static Matrix4f INV_VIEW = new Matrix4f();
    private static Matrix4f PROJ = new Matrix4f();
    private static Vec3 cameraPos = Vec3.ZERO;
    private static Vec3 dragSourceWorld = null;
    private static int dragSourceColor = 0xFFFFFFFF;

    public static void captureCamera(Matrix4f view, Matrix4f projection, Vec3 camPos)
    {
        VIEW.set(view);
        INV_VIEW.set(view).invert();
        PROJ.set(projection);
        cameraPos = camPos;
    }

    private static float[] projectWorld(Vec3 worldPos, int w, int h) {
            Vector4f v = new Vector4f((float) (worldPos.x - cameraPos.x),
                    (float) (worldPos.y - cameraPos.y),
                    (float) (worldPos.z - cameraPos.z), 1f);
                v.mul(VIEW).mul(PROJ);
                if(v.w > 0.05f) {
                    return new float[]{(v.x / v.w * 0.5f + 0.5f) * w, (0.5f - v.y / v.w * 0.5f) * h};
                 }
                float length = Math.max(Mth.sqrt(v.x * v.x + v.y * v.y), 1e-4f);
                float far = Math.max(w, h) * 2f;
                return new float[]{w / 2f + v.x / length * far, h / 2f - v.y / length * far};

    }


    public static void submitPorts(PoseStack poseStack, BlockEntity blockEntity) {
        if (!isToolActive() || !(blockEntity instanceof ConnectionSystem.ConnectableBlockEntity c)) return;
        for (Port port : c.getPorts()) {
            submit(poseStack, blockEntity.getBlockPos(), port.id(), port.x(), port.y(), port.z(), port.type().color);
        }
        submitLinks(poseStack, blockEntity, c);
    }

    private static void submitLinks(PoseStack poseStack, BlockEntity be, ConnectionSystem.ConnectableBlockEntity c)
    {
        Level level = be.getLevel();
        if (level == null) return;
        for (Map.Entry<Integer, ConnectionSystem.PortLinks.Link> entry : c.getLinks().all().entrySet()) {
            ConnectionSystem.PortLinks.Link link = entry.getValue();
            Port in = ConnectionSystem.portById(c.getPorts(), entry.getKey());
            BlockPos srcPos = be.getBlockPos().offset(link.offset());
            if (in == null || !(level.getBlockEntity(srcPos) instanceof ConnectionSystem.ConnectableBlockEntity src)) continue;
            Port out = ConnectionSystem.portById(src.getPorts(), link.sourcePort());
            if (out == null) continue;

            Vec3 a = toWorld(poseStack, in.x(), in.y(), in.z());
            Vec3 b = toWorld(poseStack,
                    link.offset().getX() + out.x(),
                    link.offset().getY() + out.y(),
                    link.offset().getZ() + out.z());
            LINKS.add(new LinkLine(a, b, out.type().color));
        }
    }

    private static Vec3 toWorld(PoseStack poseStack, float lx, float ly, float lz)
    {
        Vector4f v = new Vector4f(lx, ly, lz, 1f).mul(poseStack.last().pose()).mul(INV_VIEW);
        return new  Vec3(cameraPos.x + v.x,cameraPos.y +  v.y, cameraPos.z + v.z);
    }

    private record LinkLine(Vec3 a, Vec3 b, int argb) {}
    private static final List<LinkLine> LINKS = new ArrayList<>();

    private static void drawWorldLine(GuiGraphics g, Vec3 wa, Vec3 wb, int w, int h, float thickness, int argb)
    {
        Vector4f a = viewSpace(wa), b = viewSpace(wb);
        final float near = -0.1f;
        if (a.z > near && b.z > near) return;
        if (a.z > near) a = clip(a, b, near); else if (b.z > near) b = clip(b, a, near);

        float[] pa = toScreen(a, w, h), pb = toScreen(b, w, h);
        drawLine(g, pa[0], pa[1], pb[0], pb[1], thickness, argb);
    }

    private static Vector4f viewSpace(Vec3 p)
    {
        return new Vector4f((float) (p.x - cameraPos.x), (float) (p.y - cameraPos.y), (float) (p.z - cameraPos.z), 1f).mul(VIEW);
    }

    private static Vector4f clip(Vector4f behind, Vector4f front, float near) {
        float t = (near - behind.z) / (front.z - behind.z);
        return new Vector4f(behind).lerp(front, t);
    }

    private static float[] toScreen(Vector4f v, int w, int h) {
        Vector4f c = new Vector4f(v).mul(PROJ);
        return new float[]{(c.x / c.w * 0.5f + 0.5f) * w, (0.5f - c.y / c.w * 0.5f) * h};
    }
}
