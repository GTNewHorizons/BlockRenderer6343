package blockrenderer6343.integration.nei;

import cpw.mods.fml.common.event.FMLInterModComms;
import net.minecraft.nbt.NBTTagCompound;

public class IMCForNEI {

    public static void IMCSender() {

        sendHandler(
                "gregtech.nei.multiblockhandler",
                "GregTech",
                "gregtech",
                "structurelib:item.structurelib.constructableTrigger",
                168,
                192,
                1,
                6);
    }

    private static void sendHandler(
            String aName,
            String modName,
            String modID,
            String aBlock,
            int width,
            int height,
            int maxrecipesperpage,
            int yshift) {
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
