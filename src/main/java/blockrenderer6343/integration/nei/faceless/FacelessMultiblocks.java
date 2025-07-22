package blockrenderer6343.integration.nei.faceless;

import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.integration.structurelib.StructureCompatNEIHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * Handler for Multiblocks with no controller/TileEntity.
 */
public class FacelessMultiblocks {

    public static ItemStack registerFacelessMultiblock(String tileEntityClass,
            Int2ObjectMap<String> facelessMultiblocks) {
        ItemStack stack = new ItemStack(Blocks.air, 1, 0);
        facelessMultiblocks.put(stack.hashCode(), tileEntityClass);
        return stack;
    }

    public static TileEntity getFromItemStack(ItemStack stack) {
        Int2ObjectMap<String> facelessMultiblocks = StructureCompatNEIHandler.getFacelessMultiblocks();
        String teClassName = facelessMultiblocks.get(stack.hashCode());

        return BRUtil.getUnsafeTile(teClassName);
    }

    public static String getDisplayName(ItemStack stack) {
        Int2ObjectMap<String> facelessMultiblocks = StructureCompatNEIHandler.getFacelessMultiblocks();
        String className = facelessMultiblocks.get(stack.hashCode());
        Class<?> tileClass;
        try {
            tileClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
        String tileName = getTileNameFromRegistry(tileClass);
        if (tileName != null) return tileName;
        // In case this fails: Get the display name from the class's name
        String simpleName = tileClass.getSimpleName();
        tileName = convertClassNameToDisplayName(simpleName);
        return tileName;
    }

    /**
     * Since not every Faceless Multiblock has implemented TileEntity.getBlockType(), I have to get the display name
     * from GameRegistry.classToNameMap
     */
    private static String getTileNameFromRegistry(Class<?> tileClass) {
        try {
            Map<?, ?> classToName = BRUtil.getClassToNameMap();

            String unlocalizedName = "tile." + classToName.get(tileClass) + ".name";

            return StatCollector.translateToLocal(unlocalizedName);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static String convertClassNameToDisplayName(String className) {
        if (className.startsWith("Tile")) {
            className = className.substring(4);
        }

        // Use regex to split camel case words
        String[] words = className.split("(?=[A-Z])");

        // Join with spaces
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(' ');
            result.append(words[i]);
        }

        return result.toString();
    }
}
