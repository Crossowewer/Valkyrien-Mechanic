package com.crossower.valkyrien_mechanic;

import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.loading.FMLEnvironment
import org.valkyrienskies.mod.api.vsApi
import com.crossower.valkyrien_mechanic.client.ValkyrienMechanicClient
import com.crossower.valkyrien_mechanic.ValkyrienMechanic.Companion.MOD_ID


@Mod(MOD_ID)
class ValkyrienMechanic {

    init {
        val modEventBus = FMLJavaModLoadingContext.get().modEventBus

        modEventBus.addListener(::init)
        if (FMLEnvironment.dist.isClient) {
            modEventBus.addListener(ValkyrienMechanicClient.Companion::clientInit)
        }
    }

    companion object {
        const val MOD_ID = "valkyrien_mechanic"

        @JvmStatic
        fun init(event: FMLCommonSetupEvent) {
            // Put anything initialized on forge-side here.
            vsApi.physTickEvent.on {

            }
        }
    }
}