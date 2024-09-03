package blockrenderer6343.integration.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.integration.nei.GUI_MultiblockHandler;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.ITurnable;
import gregtech.api.threads.RunnableMachineUpdate;

public class GT_GUI_MultiblockHandler extends GUI_MultiblockHandler<IConstructable> {

    public GT_GUI_MultiblockHandler() {
        super();
    }

    @Override
    protected void placeMultiblock() {
        if (RunnableMachineUpdate.isCurrentThreadEnabled()) {
            RunnableMachineUpdate.setCurrentThreadEnabled(false);
        }

        IConstructable constructable = null;
        ItemStack copy = stackForm.copy();
        copy.getItem().onItemUse(
                copy,
                fakeMultiblockBuilder,
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

        if (!StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.enableInstrument(BlockRenderer6343.MOD_ID);
        }
        structureElements.clear();

        if (mte instanceof ISurvivalConstructable survivalConstructable) {
            int result, iterations = 0;
            do {
                result = survivalConstructable.survivalConstruct(
                        getBuildTriggerStack(),
                        Integer.MAX_VALUE,
                        ISurvivalBuildEnvironment.create(CreativeItemSource.instance, fakeMultiblockBuilder));
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

        if (StructureLibAPI.isInstrumentEnabled()) {
            StructureLibAPI.disableInstrument();
        }

        if (!RunnableMachineUpdate.isCurrentThreadEnabled()) {
            RunnableMachineUpdate.setCurrentThreadEnabled(true);
        }
    }
}
