package blockrenderer6343.integration.gregtech;

import static gregtech.api.GregTechAPI.METATILEENTITIES;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.integration.nei.MultiblockHandler;
import blockrenderer6343.integration.nei.StructureHacks;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.TemplateRecipeHandler;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.blocks.ItemMachines;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

public class GTNEIMultiblockHandler extends MultiblockHandler {

    public static final List<IConstructable> multiblocksList = new ArrayList<>();
    private static Long2ObjectMap<ObjectSet<IConstructable>> multiBlockComponents;
    private static final GTGuiMultiblockHandler baseHandler = new GTGuiMultiblockHandler();

    static {
        IStructureElement<MTEMultiBlockBase> coilElem = GTStructureUtility
                .ofCoil((a, c) -> true, b -> HeatingCoilLevel.LV);
        StructureHacks.addTieredElement(coilElem.getClass().getName());
        StructureHacks.addTieredElement(GTStructureUtility.activeCoils(coilElem).getClass().getName());
        for (IMetaTileEntity mte : METATILEENTITIES) {
            if (mte instanceof IConstructable constructable) {
                multiblocksList.add(constructable);
            }
        }

        new Thread(new GTConstructableScan(e -> multiBlockComponents = e, multiblocksList)).start();
    }

    public GTNEIMultiblockHandler() {
        super(baseHandler);
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
        if (candidate.getItem() instanceof ItemMachines) {
            for (IConstructable multiblock : multiblocksList) {
                ItemStack stackForm = ((IMetaTileEntity) multiblock).getStackForm(1);
                if (NEIClientUtils.areStacksSameType(stackForm, candidate)) {
                    return ObjectSets.singleton(multiblock);
                }
            }
        }

        if (multiBlockComponents == null) return ObjectSets.emptySet();

        return multiBlockComponents.getOrDefault(BRUtil.hashStack(candidate), ObjectSets.emptySet());
    }
}
