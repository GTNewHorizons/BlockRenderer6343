package blockrenderer6343.integration.gregtech;

import net.minecraft.item.ItemStack;

import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.common.blocks.ItemMachines;

public class GTNEIUtil {

    public static boolean isHatchItem(ItemStack item) {
        if (item != null && item.getItem() instanceof ItemMachines) {
            return ItemMachines.getMetaTileEntity(item) instanceof MTEHatch;
        }
        return false;
    }
}
