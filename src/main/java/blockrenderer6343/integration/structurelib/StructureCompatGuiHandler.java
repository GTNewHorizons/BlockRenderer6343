package blockrenderer6343.integration.structurelib;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import blockrenderer6343.integration.nei.GuiMultiblockHandler;

public class StructureCompatGuiHandler extends GuiMultiblockHandler {

    @Override
    protected void placeMultiblock() {
        Block stackBlock = ((ItemBlock) stackForm.getItem()).field_150939_a;
        renderer.world
                .setBlock(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z, stackBlock, stackForm.getItemDamage(), 3);

        TileEntity tTileEntity = renderer.world.getTileEntity(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z);
        IMultiblockInfoContainer<TileEntity> t = IMultiblockInfoContainer.get(tTileEntity.getClass());
        ISurvivalConstructable multi = t.toConstructable(tTileEntity, ExtendedFacing.DEFAULT);

        placeSurvivalPreview(multi);

        // A single tick is needed for some non GT multiblocks to complete
        renderer.world.updateEntitiesForNEI();
    }

    @Override
    protected Object getContextObject() {
        return renderer.world.getTileEntity(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z);
    }
}
