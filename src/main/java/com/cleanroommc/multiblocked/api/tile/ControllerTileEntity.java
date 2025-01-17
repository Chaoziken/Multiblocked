package com.cleanroommc.multiblocked.api.tile;

import com.cleanroommc.multiblocked.api.capability.CapabilityProxy;
import com.cleanroommc.multiblocked.api.capability.IO;
import com.cleanroommc.multiblocked.api.capability.MultiblockCapability;
import com.cleanroommc.multiblocked.api.definition.ControllerDefinition;
import com.cleanroommc.multiblocked.api.pattern.BlockPattern;
import com.cleanroommc.multiblocked.api.pattern.MultiblockState;
import com.cleanroommc.multiblocked.api.recipe.RecipeLogic;
import com.cleanroommc.multiblocked.api.registry.MultiblockCapabilities;
import com.cleanroommc.multiblocked.client.renderer.IRenderer;
import com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.mc1120.player.MCPlayer;
import crafttweaker.mc1120.world.MCFacing;
import com.cleanroommc.multiblocked.api.gui.widget.imp.controller.RecipePage;
import com.cleanroommc.multiblocked.api.tile.part.PartTileEntity;
import com.cleanroommc.multiblocked.api.gui.factory.TileEntityUIFactory;
import com.cleanroommc.multiblocked.api.gui.modular.ModularUI;
import com.cleanroommc.multiblocked.api.gui.texture.IGuiTexture;
import com.cleanroommc.multiblocked.api.gui.util.ModularUIBuilder;
import com.cleanroommc.multiblocked.api.gui.widget.imp.controller.IOPageWidget;
import com.cleanroommc.multiblocked.api.gui.widget.imp.controller.structure.StructurePageWidget;
import com.cleanroommc.multiblocked.api.gui.widget.imp.tab.TabContainer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenProperty;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A TileEntity that defies all controller machines.
 *
 * Head of the multiblock.
 */
@ZenClass("mods.multiblocked.tile.Controller")
@ZenRegister
public class ControllerTileEntity extends ComponentTileEntity<ControllerDefinition>{
    public MultiblockState state;
    protected Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities;
    private Map<Long, Map<MultiblockCapability<?>, IO>> settings;
    protected LongOpenHashSet parts;
    private boolean isWorking;
    @ZenProperty
    public RecipeLogic recipeLogic;

    public ControllerTileEntity() {
    }

    public BlockPattern getPattern() {
        if (definition.dynamicPattern != null) {
            return definition.dynamicPattern.apply(this);
        }
        return definition.basePattern;
    }

    @ZenMethod
    public boolean checkPattern() {
        if (state == null) return false;
        return getPattern().checkPatternAt(state, false);
    }

    @Override
    public boolean isValidFrontFacing(EnumFacing facing) {
        return definition.allowRotate && facing.getAxis() != EnumFacing.Axis.Y;
    }

    @ZenGetter
    public boolean isFormed() {
        return state != null && state.isFormed();
    }

    @ZenGetter
    public boolean isWorking() {
        return isWorking;
    }

    @Override
    public void update() {
        super.update();
        if (isFormed()) {
            updateFormed();
        } else if (definition.catalyst == null && getTimer() % 20 == 0) {
            if (state == null) state = new MultiblockState(world, pos);
            if (checkPattern()) { // formed
                MultiblockWorldSavedData.getOrCreate(world).addMapping(state);
                onStructureFormed();
            }
        }
    }

    public void updateFormed() {
        if (recipeLogic != null) {
            recipeLogic.update();
            if (isWorking != recipeLogic.isWorking) {
                isWorking = !isWorking;
                writeCustomData(-2, buffer -> buffer.writeBoolean(isWorking));
            }
        }
        if (definition.updateFormed != null) {
            definition.updateFormed.apply(this);
        }
    }

    @Override
    public IRenderer getRenderer() {
        if (definition.dynamicRenderer != null) {
            return definition.dynamicRenderer.apply(this);
        }
        if (definition.workingRenderer != null && recipeLogic != null && recipeLogic.isWorking && isFormed()) {
            return definition.workingRenderer;
        }
        return super.getRenderer();
    }

    public Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> getCapabilities() {
        return capabilities;
    }

    /**
     * Called when its formed, server side only.
     */
    public void onStructureFormed() {
        if (recipeLogic == null) {
            recipeLogic = new RecipeLogic(this);
        }
        // init capabilities
        Map<Long, EnumMap<IO, Set<MultiblockCapability<?>>>> capabilityMap = state.getMatchContext().get("capabilities");
        if (capabilityMap != null) {
            capabilities = Tables.newCustomTable(new EnumMap<>(IO.class), Object2ObjectOpenHashMap::new);
            for (Map.Entry<Long, EnumMap<IO, Set<MultiblockCapability<?>>>> entry : capabilityMap.entrySet()) {
                TileEntity tileEntity = world.getTileEntity(BlockPos.fromLong(entry.getKey()));
                if (tileEntity != null) {
                    if (settings != null) {
                        Map<MultiblockCapability<?>, IO> caps = settings.get(entry.getKey());
                        if (caps != null) {
                            for (Map.Entry<MultiblockCapability<?>, IO> ioEntry : caps.entrySet()) {
                                IO io = ioEntry.getValue();
                                MultiblockCapability<?> capability = ioEntry.getKey();
                                if (io == null || capability == null) continue;
                                if (capability.isBlockHasCapability(io, tileEntity)) {
                                    if (!capabilities.contains(io, capability)) {
                                        capabilities.put(io, capability, new Long2ObjectOpenHashMap<>());
                                    }
                                    capabilities.get(io, capability).put(entry.getKey().longValue(), capability.createProxy(io, tileEntity));
                                }
                            }
                        }
                    } else {
                        entry.getValue().forEach((io,set)->{
                            for (MultiblockCapability<?> capability : set) {
                                if (capability.isBlockHasCapability(io, tileEntity)) {
                                    if (!capabilities.contains(io, capability)) {
                                        capabilities.put(io, capability, new Long2ObjectOpenHashMap<>());
                                    }
                                    capabilities.get(io, capability).put(entry.getKey().longValue(), capability.createProxy(io, tileEntity));
                                }
                            }
                        });
                    }
                }
            }
        }

        settings = null;

        // init parts
        parts = state.getMatchContext().get("parts");
        if (parts != null) {
            for (Long pos : parts) {
                TileEntity tileEntity = world.getTileEntity(BlockPos.fromLong(pos));
                if (tileEntity instanceof PartTileEntity) {
                    ((PartTileEntity<?>) tileEntity).addedToController(this);
                }
            }
        }

        writeCustomData(-1, this::writeState);
        if (definition.structureFormed != null) {
            definition.structureFormed.apply(this);
        }
    }

    public void onStructureInvalid() {
        recipeLogic = null;
        // invalid parts
        if (parts != null) {
            for (Long pos : parts) {
                TileEntity tileEntity = world.getTileEntity(BlockPos.fromLong(pos));
                if (tileEntity instanceof PartTileEntity) {
                    ((PartTileEntity<?>) tileEntity).removedFromController(this);
                }
            }
            parts = null;
        }
        capabilities = null;

        writeCustomData(-1, this::writeState);
        if (definition.structureInvalid != null) {
            definition.structureInvalid.apply(this);
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == -1) {
            readState(buf);
            scheduleChunkForRenderUpdate();
        } else if (dataId == -2) {
            isWorking = buf.readBoolean();
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        writeState(buf);
        buf.writeBoolean(isWorking);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        readState(buf);
        isWorking = buf.readBoolean();
        scheduleChunkForRenderUpdate();
    }

    protected void writeState(PacketBuffer buffer) {
        buffer.writeBoolean(isFormed());
        if (isFormed() && definition.disableOthersRendering) {
            buffer.writeVarInt(state.cache.size());
            for (long blockPos : state.cache) {
                buffer.writeLong(blockPos);
            }
        }
    }

    protected void readState(PacketBuffer buffer) {
        if (buffer.readBoolean()) {
            state = new MultiblockState(world, pos);
            if (definition.disableOthersRendering) {
                int size = buffer.readVarInt();
                state.cache = new LongOpenHashSet();
                for (int i = size; i > 0; i--) {
                    state.cache.add(buffer.readLong());
                }
                long controllerPos = getPos().toLong();
                MultiblockWorldSavedData.addDisableModel(state.cache.stream().filter(pos->pos != controllerPos).map(BlockPos::fromLong).collect(Collectors.toList()));
            }
        } else {
            if (state != null && definition.disableOthersRendering && state.cache != null) {
                MultiblockWorldSavedData.removeDisableModel(state.cache.stream().map(BlockPos::fromLong).collect(Collectors.toList()));
            }
            state = null;
        }
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("recipeLogic")) {
            recipeLogic = new RecipeLogic(this);
            recipeLogic.readFromNBT(compound.getCompoundTag("recipeLogic"));
        }
        if (compound.hasKey("capabilities")) {
            NBTTagList tagList = compound.getTagList("capabilities", Constants.NBT.TAG_COMPOUND);
            settings = new HashMap<>();
            for (NBTBase base : tagList) {
                NBTTagCompound tag = (NBTTagCompound) base;
                settings.computeIfAbsent(tag.getLong("pos"), l->new HashMap<>())
                        .put(MultiblockCapabilities.get(tag.getString("cap")),
                                IO.VALUES[tag.getInteger("io")]);
            }
        }
        state = MultiblockWorldSavedData.getOrCreate(world).mapping.get(pos);
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (recipeLogic != null) compound.setTag("recipeLogic", recipeLogic.writeToNBT(new NBTTagCompound()));
        if (capabilities != null) {
            NBTTagList tagList = new NBTTagList();
            for (Table.Cell<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> cell : capabilities.cellSet()) {
                IO io = cell.getRowKey();
                MultiblockCapability<?> cap = cell.getColumnKey();
                Long2ObjectOpenHashMap<CapabilityProxy<?>> value = cell.getValue();
                if (io != null && cap != null && value != null) {
                    for (long posLong : value.keySet()) {
                        NBTTagCompound tag = new NBTTagCompound();
                        tag.setInteger("io", io.ordinal());
                        tag.setString("cap", cap.name);
                        tag.setLong("pos", posLong);
                        tagList.appendTag(tag);
                    }
                }
            }
            compound.setTag("capabilities", tagList);
        }
        return compound;
    }

    @Override
    public boolean onRightClick(EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (definition.onRightClick != null) {
            if (definition.onRightClick.apply(this, new MCPlayer(player), new MCFacing(facing), hitX, hitY, hitZ)) return true;
        }
        if (!world.isRemote) {
            if (!isFormed() && definition.catalyst != null) {
                if (state == null) state = new MultiblockState(world, pos);
                ItemStack held = player.getHeldItem(hand);
                if (definition.catalyst.isEmpty() || held.isItemEqual(definition.catalyst)) {
                    if (checkPattern()) { // formed
                        player.swingArm(hand);
                        ITextComponent formedMsg = new TextComponentTranslation(getUnlocalizedName()).appendSibling(new TextComponentTranslation("multiblocked.multiblock.formed"));
                        player.sendStatusMessage(formedMsg, true);
                        if (!player.isCreative() && !definition.catalyst.isEmpty()) {
                            held.shrink(1);
                        }
                        MultiblockWorldSavedData.getOrCreate(world).addMapping(state);
                        onStructureFormed();
                        return true;
                    }
                }
            }
            if (!player.isSneaking()) {
                if (!world.isRemote && player instanceof EntityPlayerMP) {
                    TileEntityUIFactory.INSTANCE.openUI(this, (EntityPlayerMP) player);
                }
            }
        }
        return true;
    }

    @Override
    public ModularUI createUI(EntityPlayer entityPlayer) {
        TabContainer tabContainer = new TabContainer(0, 0, 200, 232);
        if (isFormed()) {
            new RecipePage(this, tabContainer);
            new IOPageWidget(this, tabContainer);
        } else {
            new StructurePageWidget(this.definition, tabContainer);
        }
        return new ModularUIBuilder(IGuiTexture.EMPTY, 196, 256)
                .widget(tabContainer)
                .build(this, entityPlayer);
    }
}
