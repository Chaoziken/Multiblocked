package io.github.cleanroommc.multiblocked.api.gui.widget.imp.content;

import io.github.cleanroommc.multiblocked.api.capability.IO;
import io.github.cleanroommc.multiblocked.api.gui.widget.imp.SlotWidget;
import io.github.cleanroommc.multiblocked.api.recipe.ItemsIngredient;
import io.github.cleanroommc.multiblocked.util.CycleItemStackHandler;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class ItemsContentWidget extends ContentWidget<ItemsIngredient> {
    protected CycleItemStackHandler itemHandler;

    public ItemsContentWidget(@Nonnull IO io, @Nonnull ItemsIngredient object) {

    }

    @Override
    protected void onContentUpdate() {
        itemHandler = new CycleItemStackHandler(Collections.singletonList(Arrays.stream(content.getMatchingStacks()).collect(Collectors.toList())));
        addWidget(new SlotWidget(itemHandler, 0, 1, 1, false, false));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (itemHandler != null) itemHandler.update();
    }
}
