package blockrenderer6343.integration.gregtech;

import static gregtech.api.GregTechAPI.METATILEENTITIES;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Iterables;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.mojang.authlib.GameProfile;

import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.client.world.ClientFakePlayer;
import blockrenderer6343.client.world.DummyWorld;
import blockrenderer6343.integration.nei.MultiblockHandler;
import codechicken.nei.ItemStackMap;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.TemplateRecipeHandler;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.common.blocks.ItemCasingsAbstract;
import gregtech.common.blocks.ItemFrames;
import gregtech.common.blocks.ItemMachines;
import gtPlusPlus.xmod.gregtech.common.blocks.GregtechMetaItemCasingsAbstract;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

public class GTNEIMultiblockHandler extends MultiblockHandler {

    public static final List<IConstructable> multiblocksList = new ArrayList<>();
    private static final ItemStackMap<ObjectSet<IConstructable>> casingForMulti;
    private static final GTGuiMultiblockHandler baseHandler = new GTGuiMultiblockHandler();
    private static final ItemStack HOLO_STACK = new ItemStack(StructureLibAPI.getDefaultHologramItem());
    public static final String GT_NEI_MB_HANDLER_NAME = "gregtech.nei.multiblockhandler";

    static {
        for (IMetaTileEntity mte : METATILEENTITIES) {
            if (mte instanceof IConstructable constructable) {
                multiblocksList.add(constructable);
            }
        }

        casingForMulti = getCasingForMulti();
    }

    public GTNEIMultiblockHandler() {
        super(baseHandler);
    }

    @Override
    public String getOverlayIdentifier() {
        return GT_NEI_MB_HANDLER_NAME;
    }

    @Override
    public TemplateRecipeHandler newInstance() {
        return new GTNEIMultiblockHandler();
    }

    @Override
    public @NotNull ItemStack getConstructableStack(IConstructable multiblock) {
        return ((IMetaTileEntity) multiblock).getStackForm(1);
    }

    @Override
    protected @NotNull ObjectSet<IConstructable> tryLoadingMultiblocks(ItemStack candidate) {
        if (isCasing(candidate.getItem())) {
            return casingForMulti.getOrDefault(candidate, ObjectSets.emptySet());
        }

        for (IConstructable multiblock : multiblocksList) {
            ItemStack stackForm = ((IMetaTileEntity) multiblock).getStackForm(1);
            if (NEIClientUtils.areStacksSameType(stackForm, candidate)) {
                return ObjectSets.singleton(multiblock);
            }
        }

        return ObjectSets.emptySet();
    }

    @Override
    protected boolean isPotentialCandidate(ItemStack candidate) {
        return isCasing(candidate.getItem()) || candidate.getItem() instanceof ItemMachines;
    }

    public static ItemStackMap<ObjectSet<IConstructable>> getCasingForMulti() {
        ItemStackMap<ObjectSet<IConstructable>> result = new ItemStackMap<>();
        AutoPlaceEnvironment autoPlaceEnv = AutoPlaceEnvironment.fromLegacy(
                CreativeItemSource.instance,
                new ClientFakePlayer(
                        DummyWorld.INSTANCE,
                        new GameProfile(UUID.randomUUID(), "GT_NEI_MultiblockHandler")),
                a -> {});
        for (IConstructable multi : multiblocksList) {
            IStructureDefinition<?> structure = multi.getStructureDefinition();
            if (!(structure instanceof StructureDefinition)) continue;
            // noinspection unchecked
            StructureDefinition<IConstructable> structureDefinition = (StructureDefinition<IConstructable>) structure;

            ObjectSet<IStructureElement<IConstructable>> checkedElements = new ObjectOpenHashSet<>();
            for (IStructureElement<IConstructable>[] elementArray : structureDefinition.getStructures().values()) {
                for (IStructureElement<IConstructable> element : elementArray) {
                    if (!checkedElements.add(element)) continue;
                    IStructureElement.BlocksToPlace blocksToPlace = element
                            .getBlocksToPlace(multi, DummyWorld.INSTANCE, 0, 0, 0, HOLO_STACK, autoPlaceEnv);
                    if (blocksToPlace == null) continue;
                    Iterable<ItemStack> stacks = blocksToPlace.getStacks();
                    if (stacks == null) continue;
                    int size = Iterables.size(stacks);
                    if (size > 1) {
                        ItemStack stack = Iterables.get(stacks, 0);
                        if (!isCasing(stack.getItem())) continue;
                        stacks = extractTieredBlocks(multi, element, autoPlaceEnv, size);
                    }

                    for (ItemStack stack : stacks) {
                        if (!isCasing(stack.getItem())) continue;
                        ObjectSet<IConstructable> set = result.get(stack);
                        if (set == null) {
                            set = new ObjectOpenHashSet<>();
                            result.put(stack, set);
                        }
                        set.add(multi);
                    }
                }
            }
        }

        return result;
    }

    private static ObjectSet<ItemStack> extractTieredBlocks(IConstructable multi,
            IStructureElement<IConstructable> element, AutoPlaceEnvironment autoPlaceEnv, int amountToCheck) {
        ObjectSet<ItemStack> result = new ObjectOpenHashSet<>();
        for (int i = 0; i < amountToCheck; i++) {
            HOLO_STACK.stackSize = i + 1;
            IStructureElement.BlocksToPlace blocks = element
                    .getBlocksToPlace(multi, DummyWorld.INSTANCE, 0, 0, 0, HOLO_STACK, autoPlaceEnv);
            if (blocks == null) continue;
            Iterable<ItemStack> stacks = blocks.getStacks();
            if (stacks == null) continue;
            for (ItemStack stack : stacks) {
                if (isCasing(stack.getItem())) {
                    result.add(stack);
                }
            }
        }
        HOLO_STACK.stackSize = 1;
        return result;
    }

    private static boolean isCasing(Item candidate) {
        return candidate instanceof ItemCasingsAbstract || candidate instanceof ItemFrames
                || candidate instanceof GregtechMetaItemCasingsAbstract;
    }
}
