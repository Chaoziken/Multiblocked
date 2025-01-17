package com.cleanroommc.multiblocked.core.mixins;

import com.cleanroommc.multiblocked.client.renderer.ComponentRenderer;
import com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRendererDispatcher.class)
public class BlockRendererDispatcherMixin {

    @Inject(method = "renderBlockDamage", at = @At(value = "HEAD"), cancellable = true)
    private void injectRenderBlockDamage(IBlockState state, BlockPos pos, TextureAtlasSprite texture, IBlockAccess blockAccess, CallbackInfo ci) {
        if (blockAccess instanceof ChunkCache && ((ChunkCache) blockAccess).world == Minecraft.getMinecraft().world && MultiblockWorldSavedData.isModelDisabled(pos)) {
            ci.cancel();
        } else if (state.getRenderType() == ComponentRenderer.COMPONENT_RENDER_TYPE) {
            ComponentRenderer.INSTANCE.renderBlockDamage(state, pos, texture, blockAccess);
            ci.cancel();
        }
    }

    @Inject(method = "renderBlock", at = @At(value = "HEAD"), cancellable = true)
    public void injectRenderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, BufferBuilder bufferBuilderIn, CallbackInfoReturnable<Boolean> cir) {
        if (blockAccess instanceof ChunkCache && ((ChunkCache) blockAccess).world == Minecraft.getMinecraft().world && MultiblockWorldSavedData.isModelDisabled(pos)) {
            cir.setReturnValue(false);
        } else if (state.getRenderType() == ComponentRenderer.COMPONENT_RENDER_TYPE) {
            try {
                boolean result = ComponentRenderer.INSTANCE.renderBlock(state, pos, blockAccess, bufferBuilderIn);
                cir.setReturnValue(result);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Multiblocked Component Renderer Tesselating block in world");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Component being tesselated");
                CrashReportCategory.addBlockInfo(crashreportcategory, pos, state.getBlock(), state.getBlock().getMetaFromState(state));
                throw new ReportedException(crashreport);
            }
        }
    }

}
