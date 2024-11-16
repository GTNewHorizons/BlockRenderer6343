package blockrenderer6343.integration.nei;

import static blockrenderer6343.client.utils.BRUtil.FAKE_PLAYER;
import static blockrenderer6343.integration.nei.GuiMultiblockHandler.SLOT_SIZE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.ConstructableData;
import codechicken.nei.LayoutManager;
import codechicken.nei.PositionedStack;
import codechicken.nei.RecipeSearchField;
import codechicken.nei.SearchField;
import codechicken.nei.SearchTokenParser;
import codechicken.nei.api.API;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.recipe.GuiOverlayButton.UpdateOverlayButtonsEvent;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeCatalyst;
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
    private static ItemStack lastStack;
    protected List<ItemStack> ingredients = new ArrayList<>();
    protected final List<PositionedStack> positionedIngredients = new ArrayList<>();
    protected int lastRecipeHeight;
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
        lastStack = stack;
        currentMultiblocks = multiblocks.toArray(new IConstructable[0]);
    }

    @Override
    public void drawBackground(int recipe) {
        super.drawBackground(recipe);
        if (currentMultiblocks == null) return;
        if (oldRecipe != recipe) {
            oldRecipe = recipe;
            IConstructable multi = currentMultiblocks[recipe];
            guiHandler.loadMultiblock(
                    multi,
                    getConstructableStack(multi),
                    ConstructableData.getTierData(multi).setTierFromStack(lastStack));
        }

        guiHandler.recalculateSearch(recipeSearchField.text());
        guiHandler.drawMultiblock();

        if (lastRecipeHeight != RecipeCatalysts.getHeight()) {
            resetPositionedIngredients(ingredients);
            lastRecipeHeight = RecipeCatalysts.getHeight();
        }
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

    @Override
    public String getRecipeTabName() {
        return StatCollector.translateToLocal("blockrenderer6343.multiblock.structure");
    }

    protected @NotNull GuiMultiblockHandler getGuiHandler() {
        return guiHandler;
    }

    @Override
    public List<PositionedStack> getIngredientStacks(int recipe) {
        return Collections.emptyList();
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

    protected static Long2ObjectMap<ObjectSet<IConstructable>> getComponentToConstructableMap(
            Collection<IConstructable> constructables, Predicate<ItemStack> isValidItem) {
        Long2ObjectMap<ObjectSet<IConstructable>> result = new Long2ObjectOpenHashMap<>();
        for (IConstructable multi : constructables) {
            IStructureDefinition<?> structure = multi.getStructureDefinition();
            if (!(structure instanceof StructureDefinition)) continue;
            // noinspection unchecked
            StructureDefinition<IConstructable> structureDefinition = (StructureDefinition<IConstructable>) structure;

            ObjectSet<IStructureElement<IConstructable>> checkedElements = new ObjectOpenHashSet<>();
            for (IStructureElement<IConstructable>[] elementArray : structureDefinition.getStructures().values()) {
                for (IStructureElement<IConstructable> element : elementArray) {
                    if (!checkedElements.add(element)) continue;
                    Iterable<ItemStack> stacks = StructureHacks.getStacksForElement(multi, element);
                    if (stacks == null) continue;

                    for (ItemStack stack : stacks) {
                        if (!isValidItem.test(stack)) continue;
                        result.computeIfAbsent(BRUtil.hashStack(stack), k -> new ObjectOpenHashSet<>()).add(multi);
                    }
                }
            }
        }
        return result;
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
    public static void onPostOverlay(UpdateOverlayButtonsEvent.Post event) {
        if (event.gui instanceof GuiRecipe recipe && recipe.getHandler() instanceof MultiblockHandler) {
            // We have to remove the recipe overlay now that this handler has a result stack
            event.buttonList.clear();
        }
    }
}
