package blockrenderer6343.integration.gregtech;

import static blockrenderer6343.client.utils.BRUtil.FAKE_PLAYER;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.integration.nei.GuiMultiblockHandler;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.ITurnable;
import gregtech.api.threads.RunnableMachineUpdate;

public class GTGuiMultiblockHandler extends GuiMultiblockHandler {

    @Override
    protected void placeMultiblock() {
        if (RunnableMachineUpdate.isCurrentThreadEnabled()) {
            RunnableMachineUpdate.setCurrentThreadEnabled(false);
        }

        IConstructable constructable = null;
        ItemStack copy = stackForm.copy();
        copy.getItem().onItemUse(
                copy,
                FAKE_PLAYER,
                renderer.world,
                MB_PLACE_POS.x,
                MB_PLACE_POS.y,
                MB_PLACE_POS.z,
                0,
                MB_PLACE_POS.x,
                MB_PLACE_POS.y,
                MB_PLACE_POS.z);

        TileEntity tTileEntity = renderer.world.getTileEntity(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z);
        ((ITurnable) tTileEntity).setFrontFacing(ForgeDirection.SOUTH);
        IMetaTileEntity mte = ((IGregTechTileEntity) tTileEntity).getMetaTileEntity();

        if (mte instanceof ISurvivalConstructable survivalConstructable) {
            int result, iterations = 0;
            do {
                result = survivalConstructable.survivalConstruct(
                        getBuildTriggerStack(),
                        Integer.MAX_VALUE,
                        ISurvivalBuildEnvironment.create(CreativeItemSource.instance, FAKE_PLAYER));
                iterations++;
            } while (result > 0 && iterations < MAX_PLACE_ROUNDS);
        } else if (tTileEntity instanceof IConstructableProvider iConstructableProvider) {
            constructable = iConstructableProvider.getConstructable();
        } else if (tTileEntity instanceof IConstructable iConstructable) {
            constructable = iConstructable;
        }
        if (constructable != null) {
            constructable.construct(getBuildTriggerStack(), false);
        }

        if (!RunnableMachineUpdate.isCurrentThreadEnabled()) {
            RunnableMachineUpdate.setCurrentThreadEnabled(true);
        }
    }
}
