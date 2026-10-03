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

    private static volatile Long2ObjectMap<ObjectSet<IConstructable>> multiBlockComponents;
    private static volatile Object2ObjectMap<IConstructable, ItemStack> stacks;
    private static volatile IConstructable[] allMultiblocks;

    static {
        new Thread(
                new MultiblockInfoContainerScan(
                        e -> multiBlockComponents = e,
                        s -> {
                            stacks = s;
                            allMultiblocks = s.keySet().toArray(new IConstructable[0]);
                        },
                        IMultiblockInfoContainer.MULTIBLOCK_MAP)).start();
    }

    public StructureCompatNEIHandler() {
        super(new StructureCompatGuiHandler());
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

    @Override
    protected @NotNull IConstructable[] getAllMultiblocks() {
        IConstructable[] result = allMultiblocks;
        return result == null ? new IConstructable[0] : result;
    }
}
