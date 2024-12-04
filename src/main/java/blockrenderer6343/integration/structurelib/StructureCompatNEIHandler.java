package blockrenderer6343.integration.structurelib;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;

import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.integration.nei.MultiblockHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

public class StructureCompatNEIHandler extends MultiblockHandler {

    private static final StructureCompatGuiHandler baseHandler = new StructureCompatGuiHandler();
    private static Long2ObjectMap<ObjectSet<IConstructable>> multiBlockComponents;
    private static Object2ObjectMap<IConstructable, ItemStack> stacks;

    static {
        new Thread(
                new MultiblockInfoContainerScan(
                        e -> multiBlockComponents = e,
                        s -> stacks = s,
                        IMultiblockInfoContainer.MULTIBLOCK_MAP)).start();
    }

    public StructureCompatNEIHandler() {
        super(baseHandler);
    }

    @Override
    public @NotNull ItemStack getConstructableStack(IConstructable multiblock) {
        return stacks.get(multiblock);
    }

    @Override
    public TemplateRecipeHandler newInstance() {
        return new StructureCompatNEIHandler();
    }

    @Override
    protected @NotNull ObjectSet<IConstructable> tryLoadingMultiblocks(ItemStack candidate) {
        if (multiBlockComponents == null) return ObjectSets.emptySet();

        return multiBlockComponents.getOrDefault(BRUtil.hashStack(candidate), ObjectSets.emptySet());
    }
}
