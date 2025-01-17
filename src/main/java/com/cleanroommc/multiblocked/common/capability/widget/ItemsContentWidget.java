package com.cleanroommc.multiblocked.common.capability.widget;

import com.cleanroommc.multiblocked.api.gui.widget.imp.SlotWidget;
import com.cleanroommc.multiblocked.api.gui.widget.imp.recipe.ContentWidget;
import com.cleanroommc.multiblocked.api.recipe.ItemsIngredient;
import com.cleanroommc.multiblocked.util.CycleItemStackHandler;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class ItemsContentWidget extends ContentWidget<ItemsIngredient> {
    protected CycleItemStackHandler itemHandler;

    @Override
    protected void onContentUpdate() {
        if (itemHandler != null) removeWidget(widgets.get(0));
        itemHandler = new CycleItemStackHandler(Collections.singletonList(Arrays.stream(content.getMatchingStacks()).map(stack -> {
            ItemStack copy = stack.copy();
            copy.setCount(content.getAmount());
            return copy;
        }).collect(Collectors.toList())));
        addWidget(new SlotWidget(itemHandler, 0, 1, 1, false, false).setDrawOverlay(false));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }
}
