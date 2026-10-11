package com.crossower.valkyrien_mechanic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PortsRenderer implements BlockEntityRenderer
{
    public PortsRenderer(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public void render(BlockEntity blockEntity, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int i1)
    {
        ConnectionRenderer.submitPorts(poseStack, blockEntity);
    }
}
