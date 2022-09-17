package blockrenderer6343.integration.nei;

import blockrenderer6343.Tags;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;

public class NEI_Config implements IConfigureNEI {

    public static boolean isAdded = true;
    private static GT_NEI_MultiblocksHandler handler;

    @Override
    public void loadConfig() {
        isAdded = false;
        handler = new GT_NEI_MultiblocksHandler();
        GuiCraftingRecipe.craftinghandlers.add(handler);
        GuiUsageRecipe.usagehandlers.add(handler);
        isAdded = true;
    }

    @Override
    public String getName() {
        return Tags.MODID + " NEI Plugin";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
