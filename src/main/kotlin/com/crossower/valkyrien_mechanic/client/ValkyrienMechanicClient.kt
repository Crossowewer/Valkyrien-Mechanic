package com.crossower.valkyrien_mechanic.client

import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities
import com.crossower.valkyrien_mechanic.item.ModItems
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.impl.shadow.EL

@Mod.EventBusSubscriber(modid = "valkyrien_mechanic", bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
class ValkyrienMechanicClient {
    companion object {
        @JvmStatic
        fun clientInit(event: FMLClientSetupEvent) {
            // Put anything initialized on forge-side client here.
        }

        @JvmStatic
        fun registerOverlays(event: RegisterGuiOverlaysEvent) {
            event.registerBelowAll("connector_ports") { gui, g, partialTick, w, h -> ConnectionRenderer.renderOverlay(gui, g, partialTick, w, h) }
        }

        @JvmStatic
        fun onClientTick(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.START) return
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return

            if (mc.screen != null || !player.isHolding(ModItems.CONNECTOR_TOOL.get())) {
                ConnectorClient.cancel()
                return
            }

            mc.options.keyAttack.setDown(false)
            mc.options.keyUse.setDown(false)
        }

        @JvmStatic
        fun onMouseButton(event: InputEvent.MouseButton.Post) {
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return
            if (mc.screen != null || !player.isHolding(ModItems.CONNECTOR_TOOL.get())) return

            println("mouse button=${event.button} action=${event.action}")

            val hovered = ConnectionRenderer.getHovered();
            if (mc.options.keyAttack.matchesMouse(event.button)) {
                if (event.action == GLFW.GLFW_PRESS) ConnectorClient.onLeftPress(hovered)
                else if (event.action == GLFW.GLFW_RELEASE) ConnectorClient.onLeftRelease(hovered)
            }
            else if (mc.options.keyUse.matchesMouse(event.button) &&  event.action == GLFW.GLFW_PRESS) {
                ConnectorClient.onRightPress(hovered)
            }
        }

        @JvmStatic
        fun onRenderLevelStage(event: RenderLevelStageEvent) {
            if (event.stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
                ConnectionRenderer.clear()
                ConnectionRenderer.captureCamera(event.poseStack.last().pose(), event.projectionMatrix, event.camera.position)
            }
        }

        @JvmStatic
        fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
            event.registerBlockEntityRenderer(ModBlockEntities.THRUSTER_BLOCK_ENTITY.get()) { context ->
                ThrusterFlameRenderer(context)
            }
            event.registerBlockEntityRenderer(ModBlockEntities.SWITCH_BLOCK_ENTITY.get()) { context ->
                PortsRenderer(context)
            }
        }

        @JvmStatic
        fun onInteractionKey(event: net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered) {
            val player = Minecraft.getInstance().player ?: return
            if (!player.isHolding(ModItems.CONNECTOR_TOOL.get())) {
                return
            }

            if (event.isAttack  || event.isUseItem) {
                event.isCanceled = true
                event.setSwingHand(false)
            }
        }
    }
}