package blockrenderer6343.integration.nei;

import static blockrenderer6343.integration.nei.GuiMultiblockHandler.SLOT_SIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import blockrenderer6343.client.utils.ConstructableData;
import codechicken.nei.LayoutManager;
import codechicken.nei.PositionedStack;
import codechicken.nei.RecipeSearchField;
import codechicken.nei.SearchField;
import codechicken.nei.SearchTokenParser;
import codechicken.nei.api.API;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton.UpdateRecipeButtonsEvent;
import codechicken.nei.recipe.RecipeCatalysts;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import it.unimi.dsi.fastutil.objects.ObjectSet;

@EventBusSubscriber
public abstract class MultiblockHandler extends TemplateRecipeHandler {

    public static final int CANDIDATE_SLOTS_X = 150;
    public static final int CANDIDATE_SLOTS_Y = 20;
    public static final int CANDIDATE_IN_COlUMN = 6;
    private ItemStack lastStack;
    private List<ItemStack> lastPositionedIngredients;
    protected RecipeCacher recipeCacher = new RecipeCacher();
    protected GuiMultiblockHandler guiHandler;
    protected IConstructable[] currentMultiblocks;
    protected int oldRecipe;
    protected static RecipeSearchField recipeSearchField;
    protected static final PositionedStack DUMMY_STACK = new PositionedStack(
            new ItemStack(Items.poisonous_potato),
            0,
            9999,
            false);

    static {
        // this is a hack to let us hijack the "in recipe search" field while in the preview
        API.addSearchProvider(
                new SearchField.SearchParserProvider(
                        '䌡',
                        "inpreview",
                        EnumChatFormatting.RESET,
                        a -> new InPreviewFilter()) {

                    @Override
                    public SearchTokenParser.SearchMode getSearchMode() {
                        return SearchTokenParser.SearchMode.ALWAYS;
                    }
                });
        try {
            recipeSearchField = (RecipeSearchField) ReflectionHelper.findField(GuiRecipe.class, "searchField")
                    .get(null);
        } catch (Exception ignored) {}
    }

    public MultiblockHandler(GuiMultiblockHandler guiHandler) {
        this.guiHandler = guiHandler;
        guiHandler.setOnIngredientChanged(this::resetPositionedIngredients);
        guiHandler.setOnCandidateChanged(this::setResults);
    }

    public abstract @NotNull ItemStack getConstructableStack(IConstructable multiblock);

    protected abstract @NotNull ObjectSet<IConstructable> tryLoadingMultiblocks(ItemStack candidate);

    protected @NotNull IConstructable[] getAllMultiblocks() {
        return new IConstructable[0];
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if ("all".equals(outputId)) {
            loadAllRecipes();
            return;
        }
        super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients) {
        if ("all".equals(inputId)) {
            loadAllRecipes();
            return;
        }
        super.loadUsageRecipes(inputId, ingredients);
    }

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
        lastPositionedIngredients = null;
        ObjectSet<IConstructable> multiblocks = tryLoadingMultiblocks(stack);
        if (multiblocks.isEmpty()) return;
        lastStack = stack;
        currentMultiblocks = multiblocks.toArray(new IConstructable[0]);
    }

    private void loadAllRecipes() {
        currentMultiblocks = null;
        lastStack = null;
        oldRecipe = -1;
        lastPositionedIngredients = null;

        currentMultiblocks = getAllMultiblocks();
        if (currentMultiblocks.length == 0) currentMultiblocks = null;
    }

    @Override
    public void drawBackground(int recipe) {
        super.drawBackground(recipe);
        if (currentMultiblocks == null) return;
        if (oldRecipe != recipe) {
            oldRecipe = recipe;
            IConstructable multi = currentMultiblocks[recipe];
            ItemStack displayStack = getConstructableStack(multi);
            ItemStack tierStack = lastStack == null ? displayStack : lastStack;
            guiHandler.loadMultiblock(
                    multi,
                    displayStack,
                    ConstructableData.getTierData(multi).setTierFromStack(tierStack));
        }

        guiHandler.recalculateSearch(recipeSearchField.text());
        guiHandler.drawMultiblock(recipe);
    }

    public static @Nullable MultiblockHandler getHandlerFromGui(GuiScreen gui) {
        // noinspection rawtypes
        if (!(gui instanceof GuiRecipe recipe)) return null;
        if (recipe.getHandler() instanceof MultiblockHandler handler) {
            return handler;
        }
        return null;
    }

    public static @Nullable GuiMultiblockHandler getCurrentGuiHandler() {
        MultiblockHandler handler = getHandlerFromGui(Minecraft.getMinecraft().currentScreen);
        if (handler != null) {
            return handler.getGuiHandler();
        }
        return null;
    }

    @Override
    public String getOverlayIdentifier() {
        return getClass().getName();
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
        return StringUtils.abbreviate(guiHandler.getMultiblockName(), 25);
    }

    public String getFullRecipeName() {
        return guiHandler.getMultiblockName();
    }

    @Override
    public String getRecipeTabName() {
        return StatCollector.translateToLocal("blockrenderer6343.multiblock.structure");
    }

    protected @NotNull GuiMultiblockHandler getGuiHandler() {
        return guiHandler;
    }

    @Override
    public List<PositionedStack> getIngredientStacks(int recipe) {
        if (currentMultiblocks == null || recipe < 0 || recipe >= currentMultiblocks.length) {
            return Collections.emptyList();
        }

        ItemStack identity = new ItemStack(Items.poisonous_potato);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("BlockRenderer6343Recipe", getRecipeIdentity(currentMultiblocks[recipe]));
        identity.setTagCompound(tag);
        return Collections.singletonList(new PositionedStack(identity, 0, 9999, false));
    }

    private String getRecipeIdentity(IConstructable multiblock) {
        return multiblock.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(multiblock));
    }

    @Override
    public PositionedStack getResultStack(int recipe) {
        // There needs to be some sort of result stack for the in recipe search to work
        return DUMMY_STACK;
    }

    @Override
    public List<PositionedStack> getOtherStacks(int recipe) {
        return arecipes.isEmpty() ? Collections.emptyList() : arecipes.get(0).getOtherStacks();
    }

    public void resetPositionedIngredients(List<ItemStack> ingredients) {
        if (sameIngredients(lastPositionedIngredients, ingredients)) return;

        List<ItemStack> ingredientSnapshot = new ArrayList<>(ingredients.size());
        for (ItemStack ingredient : ingredients) {
            ingredientSnapshot.add(ingredient.copy());
        }
        RecipeCatalysts.putRecipeCatalysts(getOverlayIdentifier(), ingredients);
        lastPositionedIngredients = ingredientSnapshot;
    }

    private static boolean sameIngredients(List<ItemStack> first, List<ItemStack> second) {
        if (first == null || first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            if (!ItemStack.areItemStacksEqual(first.get(i), second.get(i))) return false;
        }
        return true;
    }

    public void setResults(List<List<ItemStack>> results) {
        arecipes.clear();
        recipeCacher.setResults(results);
        arecipes.add(recipeCacher);
    }

    public class RecipeCacher extends CachedRecipe {

        private List<PositionedStack> positionedResults = new ArrayList<>();

        public void setResults(List<List<ItemStack>> results) {
            if (positionedResults.size() > results.size() * 4L + 32) {
                positionedResults = new ArrayList<>(results.size());
            } else {
                positionedResults.clear();
            }
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

    public static class InPreviewFilter implements ItemFilter {

        @Override
        public boolean matches(ItemStack item) {
            GuiMultiblockHandler handler = getCurrentGuiHandler();
            if (handler != null && !LayoutManager.searchField.focused()) {
                return recipeSearchField.isVisible() && recipeSearchField.focused()
                        && !recipeSearchField.text().isEmpty();
            }
            return false;
        }
    }

    @SubscribeEvent
    @SuppressWarnings({ "unused", "rawtypes" })
    public static void onPostOverlay(UpdateRecipeButtonsEvent.Post event) {
        if (event.gui instanceof GuiRecipe recipe && recipe.getHandler() instanceof MultiblockHandler) {
            // We have to remove the recipe overlay now that this handler has a result stack
            event.buttonList.clear();
        }
    }
}
