package blockrenderer6343.integration.structurelib;

import static blockrenderer6343.client.utils.BRUtil.FAKE_PLAYER;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import blockrenderer6343.api.utils.CreativeItemSource;
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

        int result, iterations = 0;
        boolean tryConstruct = false;
        do {
            result = multi.survivalConstruct(
                    getBuildTriggerStack(),
                    Integer.MAX_VALUE,
                    ISurvivalBuildEnvironment.create(CreativeItemSource.instance, FAKE_PLAYER));
            iterations++;
            if (result == -2) {
                tryConstruct = true;
                break;
            }
        } while (renderer.world.hasChanged() && iterations < MAX_PLACE_ROUNDS);

        if (tryConstruct) {
            multi.construct(getBuildTriggerStack(), false);
        }

        // A single tick is needed for some non GT multiblocks to complete
        renderer.world.updateEntitiesForNEI();
    }

    @Override
    protected Object getContextObject() {
        return renderer.world.getTileEntity(MB_PLACE_POS.x, MB_PLACE_POS.y, MB_PLACE_POS.z);
    }
}
