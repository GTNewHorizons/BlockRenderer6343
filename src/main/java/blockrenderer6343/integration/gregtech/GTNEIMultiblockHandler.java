package blockrenderer6343.integration.gregtech;

import static gregtech.api.GregTechAPI.METATILEENTITIES;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.mojang.authlib.GameProfile;

import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.client.utils.TieredConstructable;
import blockrenderer6343.client.world.ClientFakePlayer;
import blockrenderer6343.client.world.DummyWorld;
import blockrenderer6343.integration.nei.MultiblockHandler;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.TemplateRecipeHandler;
import goodgenerator.items.GGItemBlocks;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.common.blocks.ItemCasingsAbstract;
import gregtech.common.blocks.ItemFrames;
import gregtech.common.blocks.ItemMachines;
import gtPlusPlus.xmod.gregtech.common.blocks.GregtechMetaItemCasingsAbstract;
import gtnhlanth.common.block.BlockAntennaCasing;
import gtnhlanth.common.block.BlockCasing;
import gtnhlanth.common.block.BlockShieldedAccGlass;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

public class GTNEIMultiblockHandler extends MultiblockHandler {

    public static final List<IConstructable> multiblocksList = new ArrayList<>();
    private static final ItemStackMap<ObjectSet<IConstructable>> casingForMulti;
    private static final GTGuiMultiblockHandler baseHandler = new GTGuiMultiblockHandler();
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
        if (multiblock instanceof TieredConstructable tiered) {
            multiblock = tiered.getOriginal();
        }
        return ((IMetaTileEntity) multiblock).getStackForm(1);
    }

    @Override
    protected @NotNull ObjectSet<IConstructable> tryLoadingMultiblocks(ItemStack candidate) {
        if (isValidItem(candidate.getItem())) {
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
        return isValidItem(candidate.getItem()) || candidate.getItem() instanceof ItemMachines;
    }

    private static ItemStackMap<ObjectSet<IConstructable>> getCasingForMulti() {
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
                    Iterable<ItemStack> stacks = StructureHacks.getStacksForElement(multi, element, autoPlaceEnv);
                    if (stacks == null) continue;

                    for (ItemStack stack : stacks) {
                        if (!isValidItem(stack.getItem())) continue;
                        ObjectSet<IConstructable> set = result.computeIfAbsent(stack, k -> new ObjectOpenHashSet<>());

                        if (stack.stackSize > 1) {
                            set.add(new TieredConstructable(multi, stack.stackSize));
                        } else {
                            set.add(multi);
                        }
                    }
                }
            }
        }

        return result;
    }

    static boolean isValidItem(Item candidate) {
        if (!(candidate instanceof ItemBlock)) return false;
        if (candidate instanceof ItemCasingsAbstract || candidate instanceof ItemFrames
                || candidate instanceof GregtechMetaItemCasingsAbstract
                || candidate instanceof GGItemBlocks
                || StructureHacks.validGlass.contains(candidate))
            return true;
        Block candidateBlock = Block.getBlockFromItem(candidate);
        return candidateBlock instanceof BlockCasing || candidateBlock instanceof BlockShieldedAccGlass
                || candidateBlock instanceof BlockAntennaCasing;
    }
}
