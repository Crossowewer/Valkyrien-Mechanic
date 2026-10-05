package com.crossower.valkyrien_mechanic.client

import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities
import com.crossower.valkyrien_mechanic.item.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

class ValkyrienMechanicClient {
    companion object {
        @JvmStatic
        fun clientInit(event: FMLClientSetupEvent) {
            // Put anything initialized on forge-side client here.
        }

        @JvmStatic
        fun registerOverlays(event: RegisterGuiOverlaysEvent) {
            event.registerBelowAll("connector_ports") { gui, g, partialTick, w, h -> ConnectorRenderer.renderOverlay(gui, g, partialTick, w, h) }
        }

        @JvmStatic
        fun onRenderLevelStage(event: RenderLevelStageEvent) {
            if (event.stage == RenderLevelStageEvent.Stage.AFTER_SKY) ConnectorRenderer.clear()
        }

        @JvmStatic
        fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
            event.registerBlockEntityRenderer(ModBlockEntities.THRUSTER_BLOCK_ENTITY.get()) { context ->
                ThrusterFlameRenderer(context)
            }
        }

        @JvmStatic
        fun onInteractionKey(event: net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered) {
            val player = Minecraft.getInstance().player ?: return
            if (!player.isHolding(ModItems.CONNECTOR_TOOL.get())) return

            if (event.isAttack  || event.isUseItem) {
                event.isCanceled = true
                event.setSwingHand(false)
            }
        }

        @JvmStatic
        fun onClienTick(event: TickEvent.ClientTickEvent) {
            if (event.phase == TickEvent.Phase.START) return
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return
            if (!player.isHolding(ModItems.CONNECTOR_TOOL.get())) return

            mc.options.keyAttack.setDown(false)
            mc.options.keyUse.setDown(false)
            while (mc.options.keyAttack.consumeClick()) {}
            while (mc.options.keyUse.consumeClick()) {}

        }
    }
}