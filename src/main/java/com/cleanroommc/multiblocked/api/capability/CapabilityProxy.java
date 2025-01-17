package com.cleanroommc.multiblocked.api.capability;

import com.cleanroommc.multiblocked.api.recipe.Recipe;
import net.minecraft.tileentity.TileEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The Proxy of a specific capability that has been detected {@link MultiblockCapability}. Providing I/O and such features to a controller.
 */
public abstract class CapabilityProxy<K> {
    public final MultiblockCapability<? super K> capability;

    private TileEntity tileEntity;

    public CapabilityProxy(MultiblockCapability<? super K> capability, TileEntity tileEntity) {
        this.capability = capability;
        this.tileEntity = tileEntity;
    }

    public TileEntity getTileEntity() {
        if (tileEntity != null && tileEntity.isInvalid()) {
            tileEntity = tileEntity.getWorld().getTileEntity(tileEntity.getPos());
        }
        return tileEntity;
    }

    /**
     * matching or handling the given recipe.
     *
     * @param io the IO type of this recipe. always be one of the {@link IO#IN} or {@link IO#OUT}
     * @param recipe recipe.
     * @param left left contents for to be handled.
     * @param simulate simulate.
     * @return left contents for continue handling by other proxies.
     * <br>
     *      null - nothing left. handling successful/finish. you should always return null as a handling-done mark.
     */
    protected abstract List<K> handleRecipeInner(IO io, Recipe recipe, List<K> left, boolean simulate);

    @SuppressWarnings("unchecked")
    public final K copyContent(Object content) {
        return (K) capability.copyInner((K)content);
    }

    public final List<K> searchingRecipe(IO io, Recipe recipe, List<?> left) {
        return handleRecipeInner(io, recipe, left.stream().map(this::copyContent).collect(Collectors.toList()), true);
    }

    public final List<K> handleRecipeInput(Recipe recipe, List<?> left) {
        return handleRecipeInner(IO.IN, recipe, left.stream().map(this::copyContent).collect(Collectors.toList()), false);
    }

    public final List<K> handleRecipeOutput(Recipe recipe, List<?> left) {
        return handleRecipeInner(IO.OUT, recipe, left.stream().map(this::copyContent).collect(Collectors.toList()), false);
    }
}
