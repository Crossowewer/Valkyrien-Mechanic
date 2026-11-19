package com.crossower.valkyrien_mechanic;

import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.loading.FMLEnvironment
import org.valkyrienskies.mod.api.vsApi
import com.crossower.valkyrien_mechanic.client.ValkyrienMechanicClient
import com.crossower.valkyrien_mechanic.ValkyrienMechanic.Companion.MOD_ID
import com.crossower.valkyrien_mechanic.block.ModBlocks
import com.crossower.valkyrien_mechanic.block.ModCreativeTab
import com.crossower.valkyrien_mechanic.block.entity.ModBlockEntities
import com.crossower.valkyrien_mechanic.block.entity.ModEntities
import com.crossower.valkyrien_mechanic.block.entity.SeatEntity
import com.crossower.valkyrien_mechanic.item.ModItems
import com.crossower.valkyrien_mechanic.sound.ModSounds
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.event.TickEvent


@Mod(MOD_ID)
class ValkyrienMechanic {

    init {
        val modEventBus = FMLJavaModLoadingContext.get().modEventBus

        //Registers
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeTab.register(modEventBus);
        ModEntities.register(modEventBus);

        //Listeners
        modEventBus.addListener(::init)
        if (FMLEnvironment.dist.isClient) {
            modEventBus.addListener(ValkyrienMechanicClient.Companion::clientInit)
            modEventBus.addListener(ValkyrienMechanicClient.Companion::registerRenderers)
            modEventBus.addListener(ValkyrienMechanicClient.Companion::registerOverlays)
            MinecraftForge.EVENT_BUS.addListener(ValkyrienMechanicClient.Companion::onRenderLevelStage)
            MinecraftForge.EVENT_BUS.addListener(ValkyrienMechanicClient.Companion::onInteractionKey)
            MinecraftForge.EVENT_BUS.addListener(ValkyrienMechanicClient.Companion::onMouseButton)
            MinecraftForge.EVENT_BUS.addListener(ValkyrienMechanicClient.Companion::onClientTickEnd)
            MinecraftForge.EVENT_BUS.addListener(ValkyrienMechanicClient.Companion::onClientTick)
            MinecraftForge.EVENT_BUS.addListener(ValkyrienMechanicClient.Companion::onItemTooltip)
        }
        MinecraftForge.EVENT_BUS.addListener(ValkyrienMechanic.Companion::onServerTickEnd)
    }

    companion object {
        const val MOD_ID = "valkyrien_mechanic"

        @JvmStatic
        fun init(event: FMLCommonSetupEvent) {
            // Put anything initialized on forge-side here.
            vsApi.physTickEvent.on {

            }
            event.enqueueWork { ModNetwork.register() }
        }

        @JvmStatic
        fun onServerTickEnd(event: TickEvent.ServerTickEvent) {
            if (event.phase != TickEvent.Phase.END) return
            for (p in event.server.playerList.players) {
                if (p.vehicle is SeatEntity) SeatEntity.lockBody(p)
            }
        }
        private fun addCreativeTab(event: BuildCreativeModeTabContentsEvent) {

        }
    }
}