package com.crossower.valkyrien_mechanic.client

import com.crossower.valkyrien_mechanic.ValkyrienMechanic.Companion.MOD_ID
import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities
import com.crossower.valkyrien_mechanic.block.entity.ModEntities
import com.crossower.valkyrien_mechanic.block.entity.SeatEntity
import com.crossower.valkyrien_mechanic.item.ModItems
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.NoopRenderer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.fml.I18NParser
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.core.impl.shadow.EL
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.registries.ForgeRegistries

@Mod.EventBusSubscriber(modid = "valkyrien_mechanic", bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
class ValkyrienMechanicClient {
    companion object {
        @JvmStatic
        fun clientInit(event: FMLClientSetupEvent) {
            // Put anything initialized on forge-side client here.
        }

        @JvmStatic
        fun onItemTooltip(event: ItemTooltipEvent) {
            val id = ForgeRegistries.ITEMS.getKey(event.itemStack.item) ?: return
            if (id.namespace != MOD_ID) return

            val base = "tooltip.${id.namespace}.${id.path}"
            val lines = event.toolTip

            if (Screen.hasShiftDown()) {
                var i = 1
                while (I18n.exists("$base.detail$i")) {
                    lines.add(Component.translatable("$base.detail$i").withStyle(ChatFormatting.DARK_GRAY))
                    i++
                }
            } else {
                if (!I18n.exists("$base.summary")) return
                lines.add(Component.translatable("$base.summary").withStyle(ChatFormatting.GRAY))
                lines.add(Component.translatable("tooltip.valkyrien_mechanic.shift_hint").withStyle(ChatFormatting.DARK_GRAY))
            }
        }

        //Connector tool shenanigans 1
        @JvmStatic
        fun onClientTick(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.START) return
            SeatClient.tick()
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return

            if (mc.screen != null || !player.isHolding(ModItems.CONNECTOR_TOOL.get())) {
                ConnectorClient.cancel()
                return
            }
            mc.options.keyAttack.setDown(false)
            mc.options.keyUse.setDown(false)
        }

        //Connector tool shenanigans 2
        @JvmStatic
        fun onMouseButton(event: InputEvent.MouseButton.Post) {
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return
            if (mc.screen != null || !player.isHolding(ModItems.CONNECTOR_TOOL.get())) return

            val hovered = ConnectionRenderer.getHovered();
            if (mc.options.keyAttack.matchesMouse(event.button)) {
                if (event.action == GLFW.GLFW_PRESS) ConnectorClient.onLeftPress(hovered)
                else if (event.action == GLFW.GLFW_RELEASE) ConnectorClient.onLeftRelease(hovered)
            }
            else if (mc.options.keyUse.matchesMouse(event.button) &&  event.action == GLFW.GLFW_PRESS) {
                ConnectorClient.onRightPress(hovered)
            }
        }

        //Connector tool shenanigans 3
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

        //Port blocks registers (ports won't show without that) and shenanigans (lower)
        @JvmStatic
        fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
            event.registerBlockEntityRenderer(ModBlockEntities.THRUSTER_BLOCK_ENTITY.get()) { context ->
                ThrusterFlameRenderer(context)
            }
            event.registerBlockEntityRenderer(ModBlockEntities.SWITCH_BLOCK_ENTITY.get()) { context ->
                PortsRenderer(context)
            }
            event.registerBlockEntityRenderer(ModBlockEntities.LOGIC_BUTTON_BLOCK_ENTITY.get()) { context ->
                PortsRenderer(context)
            }
            event.registerBlockEntityRenderer(ModBlockEntities.BEARING_BLOCK_ENTITY.get()) { context ->
                PortsRenderer(context)
            }
            event.registerBlockEntityRenderer(ModBlockEntities.GAS_ENGINE_BLOCK_ENTITY.get()) { context ->
                PortsRenderer(context)
            }
            event.registerBlockEntityRenderer(ModBlockEntities.SEAT_BLOCK_ENTITY.get()) { context ->
                PortsRenderer(context)
            }
            event.registerEntityRenderer(ModEntities.SEAT.get()) { context -> NoopRenderer(context) }
        }

        @JvmStatic
        fun registerOverlays(event: RegisterGuiOverlaysEvent) {
            event.registerBelowAll("connector_ports") { gui, g, partialTick, w, h -> ConnectionRenderer.renderOverlay(gui, g, partialTick, w, h) }
        }

        @JvmStatic
        fun onClientTickEnd(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.END) return
            val player = Minecraft.getInstance().player ?: return
            if (player.vehicle is SeatEntity) SeatEntity.lockBody(player)
        }


        @JvmStatic
        fun onRenderLevelStage(event: RenderLevelStageEvent) {
            if (event.stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
                ConnectionRenderer.clear()
                ConnectionRenderer.captureCamera(event.poseStack.last().pose(), event.projectionMatrix, event.camera.position)
            }
        }

    }
}