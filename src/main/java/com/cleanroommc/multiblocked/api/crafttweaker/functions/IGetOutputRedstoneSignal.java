package com.cleanroommc.multiblocked.api.crafttweaker.functions;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.world.IFacing;
import com.cleanroommc.multiblocked.Multiblocked;
import com.cleanroommc.multiblocked.api.tile.ComponentTileEntity;
import net.minecraftforge.fml.common.Optional;
import stanhebben.zenscript.annotations.ZenClass;

@FunctionalInterface
@ZenClass("mods.multiblocked.function.IGetOutputRedstoneSignal")
@ZenRegister
public interface IGetOutputRedstoneSignal {
    @Optional.Method(modid = Multiblocked.MODID_CT)
    int apply(ComponentTileEntity<?> controllerTileEntity, IFacing facing);
}
