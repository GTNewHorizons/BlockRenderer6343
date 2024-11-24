package blockrenderer6343.client.utils;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

public class TieredConstructable implements ISurvivalConstructable {

    private final IConstructable original;
    private final int tier;

    public TieredConstructable(IConstructable original, int tier) {
        this.original = original;
        this.tier = tier;
    }

    public IConstructable getOriginal() {
        return original;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        original.construct(stackSize, hintsOnly);
    }

    @Override
    public IStructureDefinition<?> getStructureDefinition() {
        return original.getStructureDefinition();
    }

    @Override
    public String[] getStructureDescription(ItemStack stackSize) {
        return original.getStructureDescription(stackSize);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        return original instanceof ISurvivalConstructable survivalConstructable
                ? survivalConstructable.survivalConstruct(stackSize, elementBudget, env)
                : -2;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int survivalConstruct(ItemStack stackSize, int elementBudget, IItemSource source, EntityPlayerMP actor) {
        return original instanceof ISurvivalConstructable survivalConstructable
                ? survivalConstructable.survivalConstruct(stackSize, elementBudget, source, actor)
                : -2;
    }
}
