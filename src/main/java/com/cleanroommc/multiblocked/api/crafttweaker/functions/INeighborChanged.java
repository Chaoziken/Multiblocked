package com.cleanroommc.multiblocked.api.crafttweaker.functions;

import crafttweaker.annotations.ZenRegister;
import com.cleanroommc.multiblocked.api.tile.ComponentTileEntity;
import stanhebben.zenscript.annotations.ZenClass;

@FunctionalInterface
@ZenClass("mods.multiblocked.function.INeighborChanged")
@ZenRegister
public interface INeighborChanged {
    void apply(ComponentTileEntity<?> controllerTileEntity);
}
