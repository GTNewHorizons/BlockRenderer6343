package blockrenderer6343.integration.structurelib;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import blockrenderer6343.client.world.DummyWorld;
import blockrenderer6343.integration.nei.MultiblockHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

public class StructureCompatNEIHandler extends MultiblockHandler {

    private static final StructureCompatGuiHandler baseHandler = new StructureCompatGuiHandler();
    private static final Map<IConstructable, ItemStack> multiToStack = new HashMap<>();
    public static final String STRUCTURE_LIB_HANDLER = "blockrenderer6343.nei.structurelib";

    public StructureCompatNEIHandler() {
        super(baseHandler);
    }

    @Override
    public @NotNull ItemStack getConstructableStack(IConstructable multiblock) {
        return multiToStack.get(multiblock);
    }

    @Override
    public TemplateRecipeHandler newInstance() {
        return new StructureCompatNEIHandler();
    }

    @Override
    public String getOverlayIdentifier() {
        return STRUCTURE_LIB_HANDLER;
    }

    @Override
    protected @NotNull ObjectSet<IConstructable> tryLoadingMultiblocks(ItemStack candidate) {
        if (candidate.getItem() instanceof ItemBlock ib) {
            Block block = ib.field_150939_a;
            if (block.hasTileEntity(candidate.getItemDamage())) {
                TileEntity te = block.createTileEntity(DummyWorld.INSTANCE, ib.getMetadata(candidate.getItemDamage()));
                if (te != null && IMultiblockInfoContainer.contains(te.getClass())) {
                    IMultiblockInfoContainer<TileEntity> m = IMultiblockInfoContainer.get(te.getClass());
                    IConstructable constructable = m.toConstructable(te, ExtendedFacing.DEFAULT);
                    multiToStack.put(constructable, candidate);
                    return ObjectSets.singleton(constructable);
                }
            }
        }
        return ObjectSets.emptySet();
    }

    @Override
    protected boolean isPotentialCandidate(ItemStack candidate) {
        return candidate.getItem() instanceof ItemBlock ib
                && ib.field_150939_a.hasTileEntity(candidate.getItemDamage());
    }
}
