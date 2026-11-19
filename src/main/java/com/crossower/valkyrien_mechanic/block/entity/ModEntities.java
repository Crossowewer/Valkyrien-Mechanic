package com.crossower.valkyrien_mechanic.block.entity;

import com.crossower.valkyrien_mechanic.ValkyrienMechanic;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ValkyrienMechanic.MOD_ID);

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.of(SeatEntity::new, MobCategory.MISC).sized(0f, 0f)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build("seat"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
