package blockrenderer6343.integration.nei;

import static blockrenderer6343.integration.gregtech.GT_GUI_MultiblocksHandler.*;
import static blockrenderer6343.integration.nei.IMCForNEI.GT_NEI_MB_HANDLER_NAME;
import static gregtech.api.GregTech_API.METATILEENTITIES;
import static gregtech.api.enums.Mods.GregTech;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;

import blockrenderer6343.ClientProxy;
import blockrenderer6343.integration.gregtech.GT_GUI_MultiblocksHandler;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import codechicken.nei.recipe.*;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

public class GT_NEI_MultiblocksHandler extends TemplateRecipeHandler {

    public static List<ISurvivalConstructable> multiblocksList = new ArrayList<>();
    private static final GT_GUI_MultiblocksHandler baseHandler = ClientProxy.guiMultiblocksHandler;

    public static final int CANDIDATE_SLOTS_X = 150;
    public static final int CANDIDATE_SLOTS_Y = 20;
    public static final int CANDIDATE_IN_COlUMN = 6;

    private List<ItemStack> ingredients = new ArrayList<>();
    private final List<PositionedStack> positionedIngredients = new ArrayList<>();
    private int lastRecipeHeight;

    private final RecipeCacher recipeCacher = new RecipeCacher();

    public GT_NEI_MultiblocksHandler() {
        super();
        for (IMetaTileEntity mte : METATILEENTITIES) {
            if (mte instanceof ISurvivalConstructable) {
                multiblocksList.add((ISurvivalConstructable) mte);
            }
        }
    }

    public class RecipeCacher extends CachedRecipe {

        private final List<PositionedStack> positionedResults = new ArrayList<>();

        public void setResults(List<List<ItemStack>> results) {
            positionedResults.clear();
            int columnCount = results.size() / CANDIDATE_IN_COlUMN + 1;
            int realCandidateInColumn = results.size() % columnCount == 0 ? results.size() / columnCount
                    : results.size() / columnCount + 1;
            for (int i = 0; i < results.size(); i++) {
                PositionedStack result = new PositionedStack(
                        results.get(i),
                        CANDIDATE_SLOTS_X - (columnCount - 1) * SLOT_SIZE + (i / realCandidateInColumn) * SLOT_SIZE,
                        CANDIDATE_SLOTS_Y + (i % realCandidateInColumn) * SLOT_SIZE);
                result.generatePermutations();
                positionedResults.add(result);
            }
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            return getCycledIngredients(cycleticks / 20, positionedResults);
        }
    }

    @Override
    public TemplateRecipeHandler newInstance() {
        return new GT_NEI_MultiblocksHandler();
    }

    @Override
    public String getOverlayIdentifier() {
        return GT_NEI_MB_HANDLER_NAME;
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
        return GregTech.getResourcePath("textures", "gui/void.png");
    }

    @Override
    public String getRecipeName() {
        return "Multiblocks Structure";
    }

    @Override
    public void drawBackground(int recipe) {
        super.drawBackground(recipe);
        baseHandler.drawMultiblock();

        if (lastRecipeHeight != RecipeCatalysts.getHeight()) {
            resetPositionedIngredients();
            lastRecipeHeight = RecipeCatalysts.getHeight();
        }
    }

    @Override
    public void drawForeground(int recipe) {
        super.drawForeground(recipe);
    }

    private void tryLoadMultiblocks(ItemStack candidate) {
        for (ISurvivalConstructable multiblock : multiblocksList) {
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

    public void resetPositionedIngredients() {
        positionedIngredients.clear();

        int rowCount = RecipeCatalysts.getRowCount(RecipeCatalysts.getHeight(), ingredients.size());

        for (int index = 0; index < ingredients.size(); index++) {
            ItemStack catalyst = ingredients.get(index);
            int column = index / rowCount;
            int row = index % rowCount;
            positionedIngredients.add(
                    new PositionedStack(
                            catalyst,
                            -column * GuiRecipeCatalyst.ingredientSize,
                            row * GuiRecipeCatalyst.ingredientSize));
        }

        Map<String, List<PositionedStack>> catalystMap = RecipeCatalysts.getPositionedRecipeCatalystMap();
        catalystMap.put(GT_NEI_MB_HANDLER_NAME, positionedIngredients);
    }

    public void setResults(List<List<ItemStack>> results) {
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
        public List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int mousex, int mousey,
                List<String> currenttip) {
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
