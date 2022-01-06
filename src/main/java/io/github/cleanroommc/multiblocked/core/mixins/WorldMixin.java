package io.github.cleanroommc.multiblocked.core.mixins;

import io.github.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.cleanroommc.multiblocked.api.framework.structure.MultiblockInstance;

@Mixin(World.class)
public class WorldMixin {

    @Shadow @Final public Profiler profiler;

    @Inject(method = "updateEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 4))
    private void afterUpdatingEntities(CallbackInfo ci) {
        this.profiler.startSection("multiblocks");
        for (MultiblockInstance instance : MultiblockWorldSavedData.getOrCreate((World) (Object) this).getInstances()) {
            instance.getInternalTileEntity().update();
        }
        this.profiler.endSection();
    }

}