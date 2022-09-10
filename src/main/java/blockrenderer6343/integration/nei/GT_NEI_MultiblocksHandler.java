package blockrenderer6343.integration.nei;

import static blockrenderer6343.integration.gregtech.GT_GUI_MultiblocksHandler.*;
import static gregtech.api.GregTech_API.METATILEENTITIES;
import static gregtech.api.enums.GT_Values.RES_PATH_GUI;

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
    private static final GT_GUI_MultiblocksHandler baseHandler = new GT_GUI_MultiblocksHandler();

    public GT_NEI_MultiblocksHandler() {
        super();
        // Ban Plasma forge since it causes severe performance issue
        for (IMetaTileEntity mte : METATILEENTITIES) {
            if (mte instanceof GT_MetaTileEntity_MultiBlockBase && !(mte instanceof GT_MetaTileEntity_PlasmaForge)) {
                multiblocksList.add((GT_MetaTileEntity_MultiBlockBase) (mte));
            }
        }
        baseHandler.setOnIngredientChanged(this::setIngredients);
    }

    public class recipeCacher extends CachedRecipe {
        private final List<PositionedStack> positionedIngredients = new ArrayList<>();

        public recipeCacher(List<ItemStack> ingredients) {
            for (int i = 0; i < ingredients.size(); i++)
                positionedIngredients.add(new PositionedStack(ingredients.get(i), SLOTS_X + i * SLOT_SIZE, SLOTS_Y));
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return positionedIngredients;
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
    }

    @Override
    public void drawForeground(int recipe) {
        super.drawForeground(recipe);
    }

    private void tryLoadMultiblocks(ItemStack candidate) {
        for (GT_MetaTileEntity_MultiBlockBase multiblocks : multiblocksList) {
            if (NEIClientUtils.areStacksSameType(((IMetaTileEntity) multiblocks).getStackForm(1), candidate)) {
                baseHandler.loadMultiblocks(multiblocks);
                return;
            }
        }
    }

    @Override
    public void drawExtras(int recipe) {
        super.drawExtras(recipe);
        baseHandler.drawMultiblock();
    }

    public void setIngredients(List<ItemStack> ingredients) {
        arecipes.clear();
        arecipes.add(new GT_NEI_MultiblocksHandler.recipeCacher(ingredients));
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
