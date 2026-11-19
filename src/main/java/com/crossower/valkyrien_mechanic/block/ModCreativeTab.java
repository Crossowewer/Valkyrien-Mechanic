package com.crossower.valkyrien_mechanic.block;

import com.crossower.valkyrien_mechanic.ValkyrienMechanic;
import com.crossower.valkyrien_mechanic.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ValkyrienMechanic.MOD_ID);

    public static final RegistryObject<CreativeModeTab> VALKYRIEN_MECHANIC_TAB = CREATIVE_MODE_TABS.register("valkyrien_mechanic_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.BEARING.get()))
                    .title(Component.translatable("creativetab.mechanic_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.BEARING.get());
                        output.accept(ModBlocks.SWITCH.get());
                        output.accept(ModBlocks.BUTTON.get());
                        output.accept(ModBlocks.GAS_ENGINE.get());
                        output.accept(ModBlocks.THRUSTER.get());
                        output.accept(ModBlocks.SEAT.get());
                        output.accept(ModItems.CONNECTOR_TOOL.get());

                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
