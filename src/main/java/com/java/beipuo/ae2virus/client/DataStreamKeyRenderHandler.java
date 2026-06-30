package com.java.beipuo.ae2virus.client;

import appeng.api.client.AEKeyRenderHandler;
import appeng.api.stacks.AEFluidKey;
import appeng.client.gui.style.FluidBlitter;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public final class DataStreamKeyRenderHandler implements AEKeyRenderHandler<DataStreamKey> {
    private static final AEFluidKey ICON_FLUID = AEFluidKey.of(Fluids.WATER);

    @Override
    public void drawInGui(Minecraft minecraft, GuiGraphics guiGraphics, int x, int y, DataStreamKey stack) {
        FluidBlitter.create(ICON_FLUID)
                .dest(x, y, 16, 16)
                .blit(guiGraphics);
    }

    @Override
    public void drawOnBlockFace(PoseStack poseStack, MultiBufferSource buffers, DataStreamKey what, float scale,
            int combinedLight, Level level) {
        var fluidStack = ICON_FLUID.toStack(1);
        var renderProps = IClientFluidTypeExtensions.of(ICON_FLUID.getFluid());
        var texture = renderProps.getStillTexture(fluidStack);
        var color = renderProps.getTintColor(fluidStack);
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);

        poseStack.pushPose();
        poseStack.translate(0, 0, 0.01f);
        poseStack.last().normal().rotateX(Mth.DEG_TO_RAD * -45f);

        var buffer = buffers.getBuffer(RenderType.solid());
        scale -= 0.05f;

        var x0 = -scale / 2;
        var y0 = scale / 2;
        var x1 = scale / 2;
        var y1 = -scale / 2;

        var transform = poseStack.last().pose();
        buffer.addVertex(transform, x0, y1, 0)
                .setColor(color)
                .setUv(sprite.getU0(), sprite.getV1())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(combinedLight)
                .setNormal(0, 0, 1);
        buffer.addVertex(transform, x1, y1, 0)
                .setColor(color)
                .setUv(sprite.getU1(), sprite.getV1())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(combinedLight)
                .setNormal(0, 0, 1);
        buffer.addVertex(transform, x1, y0, 0)
                .setColor(color)
                .setUv(sprite.getU1(), sprite.getV0())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(combinedLight)
                .setNormal(0, 0, 1);
        buffer.addVertex(transform, x0, y0, 0)
                .setColor(color)
                .setUv(sprite.getU0(), sprite.getV0())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(combinedLight)
                .setNormal(0, 0, 1);
        poseStack.popPose();
    }

    @Override
    public Component getDisplayName(DataStreamKey stack) {
        return stack.getDisplayName();
    }

    @Override
    public List<Component> getTooltip(DataStreamKey stack) {
        var tooltip = new ArrayList<Component>();
        tooltip.add(Component.translatable(stack.virusTypeTranslationKey()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.infected_item",
                stack.target().getDisplayName()).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.level", stack.virusLevel())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.experience", stack.experience())
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.target_id",
                stack.target().getId().toString()).withStyle(ChatFormatting.DARK_GRAY));
        return tooltip;
    }
}
