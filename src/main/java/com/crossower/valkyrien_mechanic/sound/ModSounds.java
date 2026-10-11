package com.crossower.valkyrien_mechanic.sound;

import com.crossower.valkyrien_mechanic.ValkyrienMechanic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.valkyrienskies.core.impl.shadow.Fo;

public class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ValkyrienMechanic.MOD_ID);

    public static final RegistryObject<SoundEvent> SWITCH_CLICK = registerSoundEvents("switch_click");

    private static RegistryObject<SoundEvent> registerSoundEvents(String name)
    {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ValkyrienMechanic.MOD_ID, name)));
    }



    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
