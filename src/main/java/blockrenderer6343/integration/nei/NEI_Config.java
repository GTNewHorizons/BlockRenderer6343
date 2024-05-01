package blockrenderer6343.integration.nei;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.Tags;
import blockrenderer6343.integration.gregtech.GT_NEI_MultiblocksHandler;
import blockrenderer6343.integration.structurelib.StructureCompatNEIHandler;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class NEI_Config implements IConfigureNEI {

    public static boolean isAdded = true;

    @Override
    public void loadConfig() {
        isAdded = false;
        TemplateRecipeHandler handler = new StructureCompatNEIHandler();
        GuiCraftingRecipe.craftinghandlers.add(handler);
        GuiUsageRecipe.usagehandlers.add(handler);

        if (BlockRenderer6343.isGTLoaded) {
            handler = new GT_NEI_MultiblocksHandler();
            GuiCraftingRecipe.craftinghandlers.add(handler);
            GuiUsageRecipe.usagehandlers.add(handler);
        }
        isAdded = true;
    }

    @Override
    public String getName() {
        return BlockRenderer6343.MOD_ID + " NEI Plugin";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
