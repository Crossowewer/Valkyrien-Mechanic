package com.crossower.valkyrien_mechanic.client;

import com.crossower.valkyrien_mechanic.block.custom.ThrusterBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class ThrusterFlameRenderer implements BlockEntityRenderer<BlockEntity>
{
    public ThrusterFlameRenderer(BlockEntityRendererProvider.Context context)
    {

    }


    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        BlockState blockState = blockEntity.getBlockState();
        Direction direction = blockState.getValue(ThrusterBlock.FACING).getOpposite();

        if (ConnectionRenderer.isToolActive())
        {
            ConnectionRenderer.submitPorts(poseStack, blockEntity);
        }

        int power = blockState.getValue(ThrusterBlock.POWER);
        if (power == 0) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(direction.getRotation());
        poseStack.translate(0, 1.5, 0);

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());

        float len = (0.4f + power / 15f * 1.2f) * (1f + 0.1f * Mth.sin(blockEntity.getLevel().getGameTime() + partialTick * 2f));

        drawPlane(vc, poseStack.last().pose(), 0.35f, len);

        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        drawPlane(vc, poseStack.last().pose(), 0.35f, len);

        poseStack.popPose();
    }

    private static void drawPlane(VertexConsumer vc, Matrix4f m, float w, float len)
    {

        vc.vertex(m, -w, 0, 0).color(255, 255, 255, 200).endVertex();
        vc.vertex(m, w, 0, 0).color(255, 255, 255, 200).endVertex();
        vc.vertex(m, 0.05f, len, 0).color(0, 242, 252, 0).endVertex();
        vc.vertex(m, -0.05f, len, 0).color(0, 242, 252, 0).endVertex();

        vc.vertex(m, -0.05f, len, 0).color(0, 242, 252, 0).endVertex();
        vc.vertex(m, 0.05f, len, 0).color(0, 242, 252, 0).endVertex();
        vc.vertex(m, w, 0, 0).color(255, 255, 255, 200).endVertex();
        vc.vertex(m, -w, 0, 0).color(255, 255, 255, 200).endVertex();
    }
}
