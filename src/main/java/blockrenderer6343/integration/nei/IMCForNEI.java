package blockrenderer6343.integration.nei;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.event.FMLInterModComms;

public class IMCForNEI {

    public static final String GT_NEI_MB_HANDLER_NAME = "gregtech.nei.multiblockhandler";
    public static final String STRUCTURE_LIB_HANDLER = "blockrenderer6343.nei.structurelib";

    public static void IMCSender() {

        sendHandler(
                GT_NEI_MB_HANDLER_NAME,
                "GregTech",
                "gregtech",
                "structurelib:item.structurelib.constructableTrigger",
                168,
                192,
                1,
                6);
        sendHandler(
                STRUCTURE_LIB_HANDLER,
                "StructureLib",
                "structurelib",
                "structurelib:item.structurelib.constructableTrigger",
                168,
                192,
                1,
                6);
    }

    private static void sendHandler(String aName, String modName, String modID, String aBlock, int width, int height,
            int maxrecipesperpage, int yshift) {
        NBTTagCompound aNBT = new NBTTagCompound();
        aNBT.setString("handler", aName);
        aNBT.setString("modName", modName);
        aNBT.setString("modId", modID);
        aNBT.setBoolean("modRequired", true);
        aNBT.setString("itemName", aBlock);
        aNBT.setInteger("handlerHeight", height);
        aNBT.setInteger("handlerWidth", width);
        aNBT.setInteger("maxRecipesPerPage", maxrecipesperpage);
        aNBT.setInteger("yShift", yshift);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerHandlerInfo", aNBT);
    }
}
