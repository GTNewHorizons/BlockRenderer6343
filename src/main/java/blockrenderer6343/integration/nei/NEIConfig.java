package blockrenderer6343.integration.nei;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.structurelib.StructureLibAPI;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.Tags;
import blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler;
import blockrenderer6343.integration.structurelib.StructureCompatNEIHandler;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.HandlerInfo;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber
@SuppressWarnings("unused")
public class NEIConfig implements IConfigureNEI {

    @SubscribeEvent
    public static void registerHandler(NEIRegisterHandlerInfosEvent event) {
        if (BlockRenderer6343.isGTLoaded) {
            event.registerHandlerInfo(
                    new HandlerInfo.Builder(GTNEIMultiblockHandler.class, "GregTech", "gregtech")
                            .setDisplayStack(new ItemStack(StructureLibAPI.getDefaultHologramItem())).setHeight(168)
                            .setWidth(192).setMaxRecipesPerPage(1).setShiftY(6).build());
        }
        event.registerHandlerInfo(
                new HandlerInfo.Builder(StructureCompatNEIHandler.class, "StructureLib", StructureLibAPI.MOD_ID)
                        .setDisplayStack(new ItemStack(StructureLibAPI.getDefaultHologramItem())).setHeight(168)
                        .setWidth(192).setMaxRecipesPerPage(1).setShiftY(6).build());
    }

    @Override
    public void loadConfig() {
        addHandler(new StructureCompatNEIHandler());

        if (BlockRenderer6343.isGTLoaded) {
            addHandler(new GTNEIMultiblockHandler());
        }
    }

    private void addHandler(TemplateRecipeHandler handler) {
        GuiCraftingRecipe.craftinghandlers.add(handler);
        GuiUsageRecipe.usagehandlers.add(handler);
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
