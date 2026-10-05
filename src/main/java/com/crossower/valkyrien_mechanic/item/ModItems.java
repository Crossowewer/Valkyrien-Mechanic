package com.crossower.valkyrien_mechanic.item;

import com.crossower.valkyrien_mechanic.ValkyrienMechanic;
import com.crossower.valkyrien_mechanic.item.custom.ConnectorToolItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ValkyrienMechanic.MOD_ID);

    public static final RegistryObject<Item> CONNECTOR_TOOL = ITEMS.register("connector_tool",
            () -> new ConnectorToolItem(new Item.Properties()));

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
