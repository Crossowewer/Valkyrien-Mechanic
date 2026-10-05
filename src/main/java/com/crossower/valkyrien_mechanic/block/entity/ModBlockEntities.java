package com.crossower.valkyrien_mechanic.block.entity;

import com.crossower.valkyrien_mechanic.ValkyrienMechanic;
import com.crossower.valkyrien_mechanic.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ValkyrienMechanic.MOD_ID);

    public static final RegistryObject<BlockEntityType<ThrusterBlockEntity>> THRUSTER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("thruster_block_entity",
                    () -> BlockEntityType.Builder.of(ThrusterBlockEntity::new, ModBlocks.THRUSTER.get())
                            .build(null));

    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITIES.register(eventBus);
    }
}
