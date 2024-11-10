package blockrenderer6343.integration.gregtech;

import static blockrenderer6343.integration.nei.StructureHacks.HOLO_STACK;
import static gregtech.api.GregTechAPI.METATILEENTITIES;

import java.util.ArrayList;
import java.util.List;

import blockrenderer6343.integration.nei.StructureHacks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.world.DummyWorld;
import blockrenderer6343.integration.nei.MultiblockHandler;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.TemplateRecipeHandler;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.multitileentity.multiblock.casing.Glasses;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.blocks.ItemCasingsAbstract;
import gregtech.common.blocks.ItemFrames;
import gregtech.common.blocks.ItemMachines;
import gtPlusPlus.xmod.gregtech.common.blocks.GregtechMetaItemCasingsAbstract;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

public class GTNEIMultiblockHandler extends MultiblockHandler {

    public static final List<IConstructable> multiblocksList = new ArrayList<>();
    private final Long2ObjectMap<ObjectSet<IConstructable>> multiBlockComponents;
    private static final GTGuiMultiblockHandler baseHandler = new GTGuiMultiblockHandler();
    private static final List<Item> validGlass = getGlasses();

    static {
        StructureHacks.addTieredElement(
                GTStructureUtility.ofCoil((a, c) -> true, b -> HeatingCoilLevel.LV).getClass().getName());
        for (IMetaTileEntity mte : METATILEENTITIES) {
            if (mte instanceof IConstructable constructable) {
                multiblocksList.add(constructable);
            }
        }
    }

    public GTNEIMultiblockHandler() {
        super(baseHandler);
        multiBlockComponents = getComponentToConstructableMap(multiblocksList, GTNEIMultiblockHandler::isValidItem);
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
        if (isValidItem(candidate.getItem())) {
            return multiBlockComponents.getOrDefault(BRUtil.hashStack(candidate), ObjectSets.emptySet());
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

    public static boolean isValidItem(ItemStack candidate) {
        return isValidItem(candidate.getItem());
    }

    public static boolean isValidItem(Item candidate) {
        if (!(candidate instanceof ItemBlock)) return false;
        return candidate instanceof ItemCasingsAbstract || candidate instanceof ItemFrames
                || candidate instanceof GregtechMetaItemCasingsAbstract
                || validGlass.contains(candidate);
    }

    private static List<Item> getGlasses() {
        List<Item> result = new ArrayList<>();
        // noinspection ConstantConditions
        Iterable<ItemStack> stacks = Glasses.chainAllGlasses()
                .getBlocksToPlace(null, DummyWorld.INSTANCE, 0, 0, 0, HOLO_STACK, BRUtil.getBuildEnvironment())
                .getStacks();
        if (stacks == null) return result;
        stacks.forEach(stack -> result.add(stack.getItem()));
        return result;
    }
}
