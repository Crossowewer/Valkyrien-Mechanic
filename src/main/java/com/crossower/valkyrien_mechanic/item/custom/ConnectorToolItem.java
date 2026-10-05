package com.crossower.valkyrien_mechanic.item.custom;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ConnectorToolItem extends Item
{

    public ConnectorToolItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            private static final HumanoidModel.ArmPose POSE = HumanoidModel.ArmPose.create(
                    "VALKYRIEN_MECHANIC_CONNECTOR", false, (model, entity, arm) -> {
                        ModelPart modelPart = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                        modelPart.xRot = (float) (-Math.PI / 2) + model.head.xRot;
                        modelPart.yRot = model.head.yRot + (arm == HumanoidArm.RIGHT? -0.1f : 0.1f);
                    }
            );
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack itemStack)
            {
                return POSE;
            }
        });
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player)
    {
        return false;
    }
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity){
        return true;
    }
}
