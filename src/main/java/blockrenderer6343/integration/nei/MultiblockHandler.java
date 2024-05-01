package blockrenderer6343.integration.nei;

import static blockrenderer6343.integration.nei.GUI_MultiblocksHandler.SLOT_SIZE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipeCatalyst;
import codechicken.nei.recipe.RecipeCatalysts;
import codechicken.nei.recipe.TemplateRecipeHandler;

public abstract class MultiblockHandler extends TemplateRecipeHandler {

    public static final int CANDIDATE_SLOTS_X = 150;
    public static final int CANDIDATE_SLOTS_Y = 20;
    public static final int CANDIDATE_IN_COlUMN = 6;

    protected List<ItemStack> ingredients = new ArrayList<>();
    protected final List<PositionedStack> positionedIngredients = new ArrayList<>();
    protected int lastRecipeHeight;
    protected RecipeCacher recipeCacher = new RecipeCacher();
    protected GUI_MultiblocksHandler<?> guiHandler;

    public MultiblockHandler(GUI_MultiblocksHandler<?> guiHandler) {
        super();
        this.guiHandler = guiHandler;
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
    public void drawBackground(int recipe) {
        super.drawBackground(recipe);
        guiHandler.drawMultiblock();

        if (lastRecipeHeight != RecipeCatalysts.getHeight()) {
            resetPositionedIngredients();
            lastRecipeHeight = RecipeCatalysts.getHeight();
        }
    }

    @Override
    public String getGuiTexture() {
        return "blockrenderer6343:textures/void.png";
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("blockrenderer6343.multiblock.structure");
    }

    protected abstract void tryLoadMultiblocks(ItemStack candidate);

    protected GUI_MultiblocksHandler<?> getGuiHandler(){
        return guiHandler;
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
        catalystMap.put(getOverlayIdentifier(), positionedIngredients);
    }

    public void setResults(List<List<ItemStack>> results) {
        arecipes.clear();
        recipeCacher.setResults(results);
        arecipes.add(recipeCacher);
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
}
