package blockrenderer6343.integration.gregtech;

import static blockrenderer6343.integration.nei.IMCForNEI.GT_NEI_MB_HANDLER_NAME;
import static gregtech.api.GregTech_API.METATILEENTITIES;

import java.util.ArrayList;
import java.util.List;

import blockrenderer6343.integration.nei.MultiblockHandler;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.TemplateRecipeHandler;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

public class GT_NEI_MultiblocksHandler extends MultiblockHandler {

    public static List<IConstructable> multiblocksList = new ArrayList<>();
    private static final GT_GUI_MultiblocksHandler baseHandler = new GT_GUI_MultiblocksHandler();

    public GT_NEI_MultiblocksHandler() {
        super(baseHandler);
        for (IMetaTileEntity mte : METATILEENTITIES) {
            if (mte instanceof IConstructable) {
                multiblocksList.add((IConstructable) mte);
            }
        }
    }

    @Override
    public String getOverlayIdentifier() {
        return GT_NEI_MB_HANDLER_NAME;
    }

    @Override
    public TemplateRecipeHandler newInstance() {
        return new GT_NEI_MultiblocksHandler();
    }

    @Override
    protected void tryLoadMultiblocks(ItemStack candidate) {
        for (IConstructable multiblock : multiblocksList) {
            ItemStack stackForm = ((IMetaTileEntity) multiblock).getStackForm(1);
            if (NEIClientUtils.areStacksSameType(stackForm, candidate)) {
                baseHandler.setOnIngredientChanged(ingredients -> {
                    this.ingredients = ingredients;
                    resetPositionedIngredients();
                });
                baseHandler.setOnCandidateChanged(this::setResults);
                baseHandler.loadMultiblock(multiblock, stackForm);
                return;
            }
        }
    }
}
