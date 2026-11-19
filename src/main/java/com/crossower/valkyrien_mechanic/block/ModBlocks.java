package com.crossower.valkyrien_mechanic.block;

import com.crossower.valkyrien_mechanic.ValkyrienMechanic;
import com.crossower.valkyrien_mechanic.block.custom.*;
import com.crossower.valkyrien_mechanic.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.valkyrienskies.core.impl.shadow.Bl;

import java.util.function.Supplier;

public class ModBlocks
{
    public static DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ValkyrienMechanic.MOD_ID);

    //Block registers
    public static final RegistryObject<Block> THRUSTER = register("thruster",
            () -> new ThrusterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> THRUSTER_DUMMY = register("thruster_dummy",
            () -> new ThrusterDummyBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().noLootTable().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SWITCH = register("switch",
            () -> new SwitchBlock(BlockBehaviour.Properties.copy(Blocks.LEVER)));
    public static final RegistryObject<Block> BUTTON = register("button",
            () -> new LogicButtonBlock(BlockBehaviour.Properties.copy(Blocks.LEVER)));
    public static final RegistryObject<Block> BEARING = register("bearing",
            () -> new BearingBlock(BlockBehaviour.Properties.of().noOcclusion().noCollission().strength(0.35f, 20f).sound(SoundType.COPPER)));
    public static final RegistryObject<Block> GAS_ENGINE = register("gas_engine",
            () -> new GasEngineBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SEAT = register("seat",
            () -> new SeatBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops()));


    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block)
    {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }


    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block)
    {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
