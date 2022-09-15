package blockrenderer6343.integration.nei;

import static blockrenderer6343.integration.gregtech.GT_GUI_MultiblocksHandler.*;
import static gregtech.api.GregTech_API.METATILEENTITIES;
import static gregtech.api.enums.GT_Values.RES_PATH_GUI;

import blockrenderer6343.ClientProxy;
import blockrenderer6343.integration.gregtech.GT_GUI_MultiblocksHandler;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_PlasmaForge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

public class GT_NEI_MultiblocksHandler extends TemplateRecipeHandler {

    public static List<GT_MetaTileEntity_MultiBlockBase> multiblocksList = new ArrayList<>();
    private static final GT_GUI_MultiblocksHandler baseHandler = ClientProxy.guiMultiblocksHandler;
    private final RecipeCacher recipeCacher = new RecipeCacher();

    public static final int CANDIDATE_SLOTS_X = 5;
    public static final int CANDIDATE_SLOTS_Y = 20;

    public static final int INGREDIENT_SLOTS_X = 5;
    public static final int INGREDIENT_SLOTS_Y = 135;

    public GT_NEI_MultiblocksHandler() {
        super();
        // Ban Plasma forge since it causes severe performance issue
        for (IMetaTileEntity mte : METATILEENTITIES) {
            if (mte instanceof GT_MetaTileEntity_MultiBlockBase && !(mte instanceof GT_MetaTileEntity_PlasmaForge)) {
                multiblocksList.add((GT_MetaTileEntity_MultiBlockBase) (mte));
            }
        }
        baseHandler.setOnIngredientChanged(this::setIngredients);
        baseHandler.setOnCandidateChanged(this::setResults);
    }

    public class RecipeCacher extends CachedRecipe {
        private final List<PositionedStack> positionedIngredients = new ArrayList<>();
        private final List<PositionedStack> positionedResults = new ArrayList<>();

        public void setIngredients(List<ItemStack> ingredients) {
            positionedIngredients.clear();
            for (int i = 0; i < ingredients.size(); i++){
                positionedIngredients.add(new PositionedStack(ingredients.get(i), INGREDIENT_SLOTS_X + i * SLOT_SIZE, INGREDIENT_SLOTS_Y));
            }
        }

        public void setResults(List<ItemStack> results){
            positionedResults.clear();
            for (int i = 0; i < results.size(); i++){
                positionedResults.add(new PositionedStack(results.get(i), CANDIDATE_SLOTS_X, CANDIDATE_SLOTS_Y + i * SLOT_SIZE));
            }
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return positionedIngredients;
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            return positionedResults;
        }
    }

    @Override
    public TemplateRecipeHandler newInstance() {
        return new GT_NEI_MultiblocksHandler();
    }

    @Override
    public String getOverlayIdentifier() {
        return "gregtech.nei.multiblockhandler";
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        tryLoadMultiblocks(result);
        super.loadCraftingRecipes(result);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        tryLoadMultiblocks(ingredient);
        super.loadUsageRecipes(ingredient);
    }

    @Override
    public String getGuiTexture() {
        return RES_PATH_GUI + "void.png";
    }

    @Override
    public String getRecipeName() {
        return "Multiblocks Structure";
    }

    @Override
    public void drawBackground(int recipe) {
        super.drawBackground(recipe);
        baseHandler.drawMultiblock();
    }

    @Override
    public void drawForeground(int recipe) {
        super.drawForeground(recipe);
    }

    private void tryLoadMultiblocks(ItemStack candidate) {
        for (GT_MetaTileEntity_MultiBlockBase multiblocks : multiblocksList) {
            if (NEIClientUtils.areStacksSameType(((IMetaTileEntity) multiblocks).getStackForm(1), candidate)) {
                baseHandler.loadMultiblock(multiblocks);
                return;
            }
        }
    }

    public void setIngredients(List<ItemStack> ingredients) {
        arecipes.clear();
        recipeCacher.setIngredients(ingredients);
        arecipes.add(recipeCacher);
    }

    public void setResults(List<ItemStack> results) {
        arecipes.clear();
        recipeCacher.setResults(results);
        arecipes.add(recipeCacher);
    }

    static {
        GuiContainerManager.addInputHandler(new MB_RectHandler());
        GuiContainerManager.addTooltipHandler(new MB_RectHandler());
    }

    public static class MB_RectHandler implements IContainerInputHandler, IContainerTooltipHandler {

        public boolean canHandle(GuiContainer gui) {
            return (gui instanceof GuiUsageRecipe
                            && ((GuiUsageRecipe) gui).getHandler() instanceof GT_NEI_MultiblocksHandler)
                    || (gui instanceof GuiCraftingRecipe
                            && ((GuiCraftingRecipe) gui).getHandler() instanceof GT_NEI_MultiblocksHandler);
        }

        @Override
        public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
            if (canHandle(gui)) {
                return baseHandler.mouseClicked(button);
            }
            return false;
        }

        @Override
        public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
            return false;
        }

        @Override
        public List<String> handleTooltip(GuiContainer gui, int mousex, int mousey, List<String> currenttip) {
            if (canHandle(gui) && baseHandler.handleTooltip() != null) currenttip.addAll(baseHandler.handleTooltip());
            return currenttip;
        }

        @Override
        public List<String> handleItemDisplayName(GuiContainer gui, ItemStack itemstack, List<String> currenttip) {
            return currenttip;
        }

        @Override
        public List<String> handleItemTooltip(
                GuiContainer gui, ItemStack itemstack, int mousex, int mousey, List<String> currenttip) {
            return currenttip;
        }

        @Override
        public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
            return false;
        }

        @Override
        public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

        @Override
        public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

        @Override
        public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {}

        @Override
        public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
            return false;
        }

        @Override
        public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

        @Override
        public void onMouseDragged(GuiContainer gui, int amousex, int amousey, int button, long heldTime) {}
    }
}
