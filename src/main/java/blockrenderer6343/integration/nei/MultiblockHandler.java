package blockrenderer6343.integration.nei;

import static blockrenderer6343.integration.nei.GUI_MultiblockHandler.SLOT_SIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipeCatalyst;
import codechicken.nei.recipe.RecipeCatalysts;
import codechicken.nei.recipe.TemplateRecipeHandler;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public abstract class MultiblockHandler extends TemplateRecipeHandler {

    public static final int CANDIDATE_SLOTS_X = 150;
    public static final int CANDIDATE_SLOTS_Y = 20;
    public static final int CANDIDATE_IN_COlUMN = 6;
    protected List<ItemStack> ingredients = new ArrayList<>();
    protected final List<PositionedStack> positionedIngredients = new ArrayList<>();
    protected int lastRecipeHeight;
    protected RecipeCacher recipeCacher = new RecipeCacher();
    protected GUI_MultiblockHandler guiHandler;
    protected IConstructable[] currentMultiblocks;
    protected int oldRecipe;

    public MultiblockHandler(GUI_MultiblockHandler guiHandler) {
        this.guiHandler = guiHandler;
        guiHandler.setOnIngredientChanged(this::resetPositionedIngredients);
        guiHandler.setOnCandidateChanged(this::setResults);
    }

    public abstract @NotNull ItemStack getConstructableStack(IConstructable multiblock);

    protected abstract @NotNull ObjectSet<IConstructable> tryLoadingMultiblocks(ItemStack candidate);

    protected abstract boolean isPotentialCandidate(ItemStack candidate);

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        loadRecipes(result);
        super.loadCraftingRecipes(result);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        loadRecipes(ingredient);
        super.loadUsageRecipes(ingredient);
    }

    private void loadRecipes(ItemStack stack) {
        currentMultiblocks = null;
        oldRecipe = -1;
        if (!isPotentialCandidate(stack)) return;

        ObjectSet<IConstructable> multiblocks = tryLoadingMultiblocks(stack);
        if (multiblocks.isEmpty()) return;
        currentMultiblocks = multiblocks.toArray(new IConstructable[0]);
    }

    @Override
    public void drawBackground(int recipe) {
        super.drawBackground(recipe);
        if (currentMultiblocks == null) return;
        if (oldRecipe != recipe) {
            oldRecipe = recipe;
            IConstructable multi = currentMultiblocks[recipe];
            guiHandler.loadMultiblock(multi, getConstructableStack(multi));
        }

        guiHandler.drawMultiblock();

        if (lastRecipeHeight != RecipeCatalysts.getHeight()) {
            resetPositionedIngredients(ingredients);
            lastRecipeHeight = RecipeCatalysts.getHeight();
        }
    }

    @Override
    public int numRecipes() {
        return currentMultiblocks == null ? 0 : currentMultiblocks.length;
    }

    @Override
    public String getGuiTexture() {
        return "blockrenderer6343:textures/void.png";
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("blockrenderer6343.multiblock.structure");
    }

    protected GUI_MultiblockHandler getGuiHandler() {
        return guiHandler;
    }

    @Override
    public List<PositionedStack> getIngredientStacks(int recipe) {
        return Collections.emptyList();
    }

    @Override
    public PositionedStack getResultStack(int recipe) {
        return null;
    }

    @Override
    public List<PositionedStack> getOtherStacks(int recipe) {
        return arecipes.isEmpty() ? Collections.emptyList() : arecipes.get(0).getOtherStacks();
    }

    public void resetPositionedIngredients(List<ItemStack> ingredients) {
        this.ingredients = ingredients;
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
