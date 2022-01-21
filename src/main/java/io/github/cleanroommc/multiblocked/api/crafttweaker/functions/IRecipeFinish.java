package io.github.cleanroommc.multiblocked.api.crafttweaker.functions;

import crafttweaker.annotations.ZenRegister;
import io.github.cleanroommc.multiblocked.api.recipe.Recipe;
import io.github.cleanroommc.multiblocked.api.recipe.RecipeLogic;
import stanhebben.zenscript.annotations.ZenClass;

@FunctionalInterface
@ZenClass("mods.multiblocked.function.IRecipeFinish")
@ZenRegister
public interface IRecipeFinish {
    void apply(RecipeLogic recipeLogic, Recipe recipe);
}